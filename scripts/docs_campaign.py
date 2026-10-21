#!/usr/bin/env python3
"""Continue the documentation overseer unattended, using the existing OpenCode API.

This controller does not reset the historical source queue or edit Java itself.
One overseer owns the checkout; completed turns are durably recorded before the
next prompt is admitted. A failed/ambiguous admission is never blindly resent.
"""
from __future__ import annotations

import argparse
import ast
import fcntl
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
from urllib.parse import urlencode
import uuid

import docs_prune

ROOT = docs_prune.ROOT
WORK = docs_prune.WORK / 'autonomous'
MODEL = {'providerID': 'openai', 'id': 'gpt-6.1-sol'}
AGENT = 'docs-overseer'
SERVICE = 'jsmacrosce-docs-campaign.service'
TERMINAL = {'complete', 'complete_with_blockers'}


def write_json(path: Path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.tmp')
    with temporary.open('w') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')
        stream.flush()
        os.fsync(stream.fileno())
    temporary.replace(path)


def load(path: Path, default=None):
    return json.loads(path.read_text()) if path.exists() else default


def missing_cli_path(error: str) -> str | None:
    """Recognize only a missing OpenCode executable, not arbitrary missing files."""
    prefix = 'FileNotFoundError: [Errno 2] No such file or directory: '
    if not error.startswith(prefix):
        return None
    try:
        name = ast.literal_eval(error[len(prefix):])
    except (SyntaxError, ValueError):
        return None
    if isinstance(name, str) and Path(name).is_absolute() and Path(name).name in ['opencode', 'opencode.exe']:
        return name
    return None


def admitted_terminal_error(messages: list, user: dict) -> dict | None:
    """Read the newest error after this prompt; API messages are newest-first.

    Compaction can fail before an assistant reply exists. Do not fall back to a
    quota error from an older turn, or skip a newer non-quota terminal error.
    """
    created = user.get('time', {}).get('created')
    if not isinstance(created, (int, float)):
        return None
    for message in messages:
        when = message.get('time', {}).get('created')
        if (message.get('type') in ['assistant', 'compaction', 'idle'] and
                isinstance(when, (int, float)) and when >= created and message.get('error')):
            return message
    return None


def permissions(root: Path, probe=False):
    rules = [{'action': '*', 'resource': '*', 'effect': 'deny'}]
    for action in ['read', 'glob', 'grep', 'skill']:
        rules.append({'action': action, 'resource': '*', 'effect': 'allow'})
    for directory in [root, root.parent / 'minecraft-source', root.parent / 'stonecutter-docs', Path('/tmp/opencode')]:
        rules.append({'action': 'external_directory', 'resource': str(directory) + '/**', 'effect': 'allow'})
    if not probe:
        for action in ['shell', 'subagent', 'webfetch', 'websearch']:
            rules.append({'action': action, 'resource': '*', 'effect': 'allow'})
        for directory in ['common/src/main/java', 'extension', 'buildSrc', 'docs', 'scripts', 'doc-prune-work/campaign']:
            for path in [directory + '/**', str(root / directory) + '/**']:
                rules.append({'action': 'edit', 'resource': path, 'effect': 'allow'})
        # A stopped controller must not be restarted or rewritten by its worker.
        for path in ['scripts/docs_campaign.py', str(root / 'scripts/docs_campaign.py'),
                     'doc-prune-work/campaign/autonomous/controller-state.json',
                     str(root / 'doc-prune-work/campaign/autonomous/controller-state.json')]:
            rules.append({'action': 'edit', 'resource': path, 'effect': 'deny'})
        for command in ['git commit*', 'git push*', 'git reset*', 'git checkout*', 'git restore*',
                        'git clean*', 'rm *', '*stonecutterSwitch*', '*publish*', '*--scan*',
                        '*runClient*', '*scripts/docs_campaign.py run*',
                        '*scripts/docs_campaign.py recover-quota*', '*scripts/docs_campaign.py resume*']:
            rules.append({'action': 'shell', 'resource': command, 'effect': 'deny'})
    rules.extend([
        {'action': 'question', 'resource': '*', 'effect': 'deny'},
        {'action': 'read', 'resource': '*.env', 'effect': 'deny'},
        {'action': 'read', 'resource': '*.env.*', 'effect': 'deny'},
    ])
    return rules


def create_session(api, root, probe=False):
    agents = api.request('GET', '/api/agent?location[directory]=' + str(root))
    if not any(a['id'] == AGENT for a in agents['data']):
        raise RuntimeError('docs-overseer is not registered in the explicit repository location')
    session = api.request('POST', '/api/session', {
        'title': 'Documentation campaign capability probe' if probe else 'Unattended full documentation campaign',
        'agent': AGENT, 'model': MODEL, 'location': {'directory': str(root)},
        'permissions': permissions(root, probe),
    })['data']
    if any((session.get('model') or {}).get(key) != value for key, value in MODEL.items()):
        raise RuntimeError(f'Unexpected overseer model: {session.get("model")}')
    return session['id']


def completion(manifest, root):
    """Do not accept an agent's 'done' text as completion of the full campaign."""
    if not isinstance(manifest, dict) or manifest.get('status') not in TERMINAL:
        return False
    stages = ['existing_documentation', 'audience_and_metadata', 'undocumented_api_coverage', 'final_validation']
    if any(manifest.get('stages', {}).get(stage) not in ['complete', 'complete_with_blockers'] for stage in stages):
        raise ValueError('A full completion manifest must account for all four campaign stages')
    if manifest.get('pending_tasks') != 0:
        raise ValueError('Completion requires zero runnable/pending tasks')
    blockers = manifest.get('blockers')
    if not isinstance(blockers, list):
        raise ValueError('Completion must explicitly list blockers, including an empty list')
    if manifest['status'] == 'complete' and (blockers or any(v != 'complete' for v in manifest['stages'].values())):
        raise ValueError('Unqualified completion cannot contain blocked dimensions')
    evidence = manifest.get('evidence')
    if not isinstance(evidence, list) or not evidence:
        raise ValueError('Completion requires evidence files')
    required_roles = {'coverage_inventory', 'source_reviews', 'validation', 'final_report'}
    if not required_roles.issubset({record.get('role') for record in evidence}):
        raise ValueError('Completion requires coverage, source-review, validation and final-report evidence')
    for record in evidence:
        path = (root / record['path']).resolve()
        if not path.is_relative_to(root) or not path.is_file():
            raise ValueError(f'Invalid completion evidence path: {path}')
        if hashlib.sha256(path.read_bytes()).hexdigest() != record.get('sha256'):
            raise ValueError(f'Completion evidence hash mismatch: {path}')
    return True


class Controller:
    def __init__(self, root=ROOT, work=WORK, api=None, wait_timeout=300):
        self.root = root.resolve()
        self.work = work
        self.api = api or docs_prune.API()
        self.wait_timeout = wait_timeout
        self.state_path = work / 'controller-state.json'
        self.state = load(self.state_path, {'status': 'new', 'turn': 0})

    def save(self, **updates):
        self.state.update(updates, updated=time.time())
        write_json(self.state_path, self.state)

    def event(self, kind, **data):
        self.work.mkdir(parents=True, exist_ok=True)
        with (self.work / 'events.jsonl').open('a') as stream:
            stream.write(json.dumps({'time': time.time(), 'kind': kind, **data}) + '\n')
            stream.flush()
            os.fsync(stream.fileno())

    def wait_idle(self, session):
        transport_failures = 0
        while True:
            try:
                self.api.request('POST', f'/api/experimental/session/{session}/wait', {}, timeout=self.wait_timeout)
                return
            except subprocess.TimeoutExpired:
                # A transport wait timeout is not a failed agent or permission to
                # launch another writer. Continue waiting on the same session.
                self.save(status='waiting', session_id=session)
                self.event('wait_transport_timeout', session_id=session)
                pending = self.api.request('GET', f'/api/session/{session}/permission')['data']
                forms = self.api.request('GET', f'/api/session/{session}/form')['data']
                if pending or forms:
                    raise RuntimeError('Overseer waiting for interactive input; no automatic approval')
            except subprocess.CalledProcessError as exc:
                # A broken wait connection does not mean the editing turn
                # failed. Observe this same session; never resend its prompt.
                transport_failures += 1
                self.event('wait_transport_error', session_id=session, attempt=transport_failures,
                           returncode=exc.returncode, stdout=str(exc.stdout or '')[:16384],
                           stderr=str(exc.stderr or '')[:16384])
                active = self.api.request('GET', '/api/session/active')['data']
                info = self.api.request('GET', f'/api/session/{session}')['data']
                if session not in active:
                    if info.get('outcome') in ['succeeded', 'failed', 'interrupted']:
                        # collect() still checks the outcome, including quota.
                        return
                    raise RuntimeError('Wait connection failed and session settlement is unknown') from exc
                pending = self.api.request('GET', f'/api/session/{session}/permission')['data']
                forms = self.api.request('GET', f'/api/session/{session}/form')['data']
                if pending or forms:
                    raise RuntimeError('Overseer waiting for interactive input; no automatic approval') from exc
                if transport_failures >= 3:
                    raise
                self.save(status='waiting', session_id=session)
                time.sleep(transport_failures)

    def collect(self, session, turn):
        self.wait_idle(session)
        info = self.api.request('GET', f'/api/session/{session}')['data']
        write_json(self.work / 'turns' / f'{turn:06d}-session.json', info)
        messages = self.api.request('GET', f'/api/session/{session}/message?order=desc&type=assistant&limit=1')
        write_json(self.work / 'turns' / f'{turn:06d}-response.json', messages)
        if info.get('outcome') in ['failed', 'interrupted']:
            # Keep terminal/compaction evidence even if the last assistant reply
            # belongs to the preceding successful turn.
            terminal = self.api.request('GET', f'/api/session/{session}/message?order=desc&limit=20')
            user = self.api.request('GET', f'/api/session/{session}/message?order=desc&type=user&limit=1')
            write_json(self.work / 'turns' / f'{turn:06d}-failure-messages.json', terminal)
            write_json(self.work / 'turns' / f'{turn:06d}-failure-user.json', user)
            raise RuntimeError(f'Overseer ended with outcome {info["outcome"]}; preserve failure, no automatic retry')
        pending = self.api.request('GET', f'/api/session/{session}/permission')['data']
        forms = self.api.request('GET', f'/api/session/{session}/form')['data']
        if pending or forms:
            raise RuntimeError('Unexpected interactive permission/form: recorded, not auto-approved')
        active = self.root / 'stonecutter.active'
        if active.exists() and active.read_text().strip() != self.state.get('initial_active_target'):
            raise RuntimeError('Active Stonecutter target changed; preserve source and stop for diagnosis')

    def recover_quota(self, confirmed=False, caller_session=None):
        """Prepare one explicit recovery, without admitting or replaying work.

        The CLI starts the installed service only after these locks are released.
        Also reconcile a broken read-only client (wait connection or missing CLI)
        if the exact admitted turn succeeded. Other failures need diagnosis.
        """
        if not confirmed:
            raise RuntimeError('Pass --confirm-reset only after the usage quota has reset')
        with (self.work / 'controller.lock').open('a+') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            with (self.root / 'doc-prune-work/campaign/campaign.lock').open('a+') as source_lock:
                fcntl.flock(source_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                # Reload under lock; a concurrently started controller may have
                # changed this state since this object was constructed.
                self.state = load(self.state_path, {})
                prepared = (self.state.get('status') == 'ready' and
                            (not self.state.get('inflight') or self.state.get('recovery_kind') == 'completed_wait') and
                            self.state.get('recovery_authorization') and
                            self.state.get('last_failure', {}).get('turn') == self.state.get('turn'))
                if self.state.get('status') != 'blocked' and not prepared:
                    raise RuntimeError('recover-quota requires a blocked quota failure, not a running/stopped/complete campaign')
                if (self.work / 'STOP').exists():
                    raise RuntimeError('Intentional STOP is set; quota recovery must not override it')
                active_target = self.root / 'stonecutter.active'
                if (not active_target.is_file() or
                        active_target.read_text().strip() != self.state.get('initial_active_target')):
                    raise RuntimeError('Active Stonecutter target is missing or changed; recovery refused')
                session = self.state.get('session_id')
                if not session:
                    raise RuntimeError('Quota recovery requires an existing overseer session')
                info = self.api.request('GET', f'/api/session/{session}')['data']
                recorded_error = (self.state.get('last_failure', {}).get('error', '') if prepared
                                  else self.state.get('error', '')) or ''
                wait_path = f'/api/experimental/session/{session}/wait'
                missing_cli = missing_cli_path(recorded_error)
                broken_wait = (recorded_error.startswith('CalledProcessError:') and
                               "'POST'" in recorded_error and wait_path in recorded_error)
                completed_wait = (info.get('outcome') == 'succeeded' and self.state.get('inflight') and
                                  (broken_wait or missing_cli is not None))
                if (info.get('outcome') != 'failed' and not completed_wait) or info.get('agent') != AGENT:
                    raise RuntimeError('Overseer is not a settled failed documentation session or a verified completed '
                                       f'client-failure case (outcome={info.get("outcome")!r}, '
                                       f'controller_error={recorded_error.split(":", 1)[0]!r}); '
                                       'no retry was attempted. Inspect status and service.log.')
                if any((info.get('model') or {}).get(key) != value for key, value in MODEL.items()):
                    raise RuntimeError('Overseer model changed; recovery refused')
                project = info.get('projectID')
                if not project:
                    raise RuntimeError('Cannot establish the campaign project for active-worker checks')
                messages = self.api.request('GET', f'/api/session/{session}/message?order=desc&type=assistant&limit=1')
                response = messages.get('data', [])
                failure_message = None
                user = self.api.request('GET', f'/api/session/{session}/message?order=desc&type=user&limit=1')['data']
                if not self.state.get('message_id') or not user or user[0].get('id') != self.state['message_id']:
                    raise RuntimeError('Latest user message does not match the admitted controller prompt; recovery refused')
                if completed_wait:
                    if (not response or response[0].get('error') or response[0].get('finish') != 'stop' or
                            not response[0].get('time', {}).get('completed')):
                        raise RuntimeError('Wait recovery requires a completed successful assistant response')
                    if response[0]['time']['completed'] < user[0].get('time', {}).get('created', float('inf')):
                        raise RuntimeError('Successful response does not match the admitted controller prompt')
                else:
                    messages = self.api.request('GET', f'/api/session/{session}/message?order=desc&limit=20')
                    failure_message = admitted_terminal_error(messages.get('data', []), user[0])
                    failure_type = (failure_message.get('error') or {}).get('type') if failure_message else None
                    if failure_type != 'provider.quota':
                        source = failure_message.get('type') if failure_message else None
                        raise RuntimeError('Latest admitted-turn failure is not provider.quota '
                                           f'(error={failure_type!r}, message_type={source!r}); '
                                           'manual diagnosis required, no retry was attempted')
                plan = load(self.work / 'plan.json', {})
                known_sessions = {session}
                def find_sessions(value):
                    if isinstance(value, dict):
                        for item in value.values(): find_sessions(item)
                    elif isinstance(value, list):
                        for item in value: find_sessions(item)
                    elif isinstance(value, str) and value.startswith('ses_'):
                        known_sessions.add(value)
                find_sessions(plan)
                active_sessions = self.api.request('GET', '/api/session/active')['data']
                for sid in active_sessions:
                    # An interactive recovery overseer can identify itself when
                    # calling this method. The terminal CLI has no exemption.
                    if sid == caller_session:
                        continue
                    if sid in known_sessions:
                        raise RuntimeError(f'Campaign session is still active: {sid}; no duplicate writer started')
                    other = self.api.request('GET', f'/api/session/{sid}')['data']
                    if other.get('projectID') == project:
                        raise RuntimeError(f'A session in this repository is still active: {sid}; wait until it settles')
                    if not other.get('projectID'):
                        raise RuntimeError(f'Cannot establish active session ownership: {sid}')
                for endpoint in ['inbox', 'permission', 'form']:
                    if self.api.request('GET', f'/api/session/{session}/{endpoint}')['data']:
                        raise RuntimeError(f'Overseer has pending {endpoint}; recovery refused')
                shell_path = '/api/shell?' + urlencode({'location[directory]': str(self.root)})
                if self.api.request('GET', shell_path)['data']:
                    raise RuntimeError('Shell work is still active in this repository; recovery refused')
                if prepared:
                    authorization = (self.root / self.state['recovery_authorization']).resolve()
                    if not authorization.is_relative_to(self.work.resolve()) or not authorization.is_file():
                        raise RuntimeError('Prepared recovery authorization is missing or invalid')
                    return {'status': 'ready', 'next_turn': self.state['turn'] + 1,
                            'collect_completed_turn_first': bool(completed_wait),
                            'authorization': self.state['recovery_authorization'], 'already_prepared': True}
                # Archive first: crashes before the final state write must leave
                # the campaign blocked, with the original failure untouched.
                kind = ('completed-missing-cli' if missing_cli else 'completed-wait') if completed_wait else 'quota-reset'
                recovery = self.work / 'recovery' / f'{kind}-turn-{self.state["turn"]}-{uuid.uuid4().hex}'
                recovery.mkdir(parents=True, exist_ok=False)
                write_json(recovery / 'controller-state-before.json', self.state)
                write_json(recovery / 'session-before.json', info)
                write_json(recovery / ('completed-response.json' if completed_wait else 'quota-error-response.json'), messages)
                write_json(recovery / 'admitted-user-response.json', user)
                write_json(recovery / 'plan-before.json', plan)
                write_json(recovery / 'source-runner-before.json', load(self.root / 'doc-prune-work/campaign/source-runner.json', {}))
                authorization_text = ('Explicit user request to collect a completed turn after a local OpenCode client failure'
                                      if completed_wait else 'Explicit user confirmation that the usage quota has reset')
                record = {'time': time.time(), 'authorization': authorization_text,
                          'session_id': session, 'failed_turn': self.state['turn'],
                          'failed_message_id': self.state.get('message_id'),
                          'failure_type': ('missing_cli' if missing_cli else 'wait_transport') if completed_wait else 'provider.quota',
                          'missing_cli_path': missing_cli,
                          'failure_message_type': failure_message.get('type') if failure_message else None,
                          'failure_message_id': failure_message.get('id') if failure_message else None,
                          'overseer_outcome': info.get('outcome'),
                          'same_session': True, 'future_automatic_quota_retry': False,
                          'no_active_campaign_workers_or_shells': True,
                          'no_queued_prompts_or_interactive_requests': True, 'next_turn': self.state['turn'] + 1}
                write_json(recovery / 'authorization.json', record)
                heading = 'COMPLETED-WAIT RECOVERY' if completed_wait else 'QUOTA RESET RECOVERY'
                notice = (f'EXPLICIT USER {heading}: blocked controller turn {self.state["turn"]} '
                          f'has been preserved in {recovery.relative_to(self.root)}. The user authorized this one '
                          'resumption. Reconcile partial work and failed worker attempts from '
                          'the current plan/evidence before dispatching; reuse completed edits, reviews and output checks. '
                          'Do not mark partially validated work done or retry unrelated failures. This does not authorize '
                          'automatic retries of future quota failures.\n')
                if completed_wait:
                    notice += ('The controller could not use its read-only OpenCode client, but the exact admitted turn succeeded. '
                               'That turn must be collected, not repeated. Continue from its completed checkpoint.\n')
                (recovery / 'recovery-notice.txt').write_text(notice)
                self.event('explicit_user_completed_wait_recovery' if completed_wait else 'explicit_user_quota_reset_recovery',
                           **record, evidence=str(recovery.relative_to(self.root)))
                self.state['last_failure'] = {'turn': self.state['turn'], 'error': self.state.get('error'),
                                              'evidence': str(recovery.relative_to(self.root))}
                self.state.pop('error', None)
                self.save(status='ready', inflight=bool(completed_wait),
                          recovery_kind='completed_wait' if completed_wait else 'quota',
                          recovery_authorization=str((recovery / 'authorization.json').relative_to(self.root)))
                return {'status': 'ready', 'next_turn': self.state['turn'] + 1,
                        'collect_completed_turn_first': bool(completed_wait),
                        'authorization': self.state['recovery_authorization']}

    def run(self, max_turns=None):
        self.work.mkdir(parents=True, exist_ok=True)
        with (self.work / 'controller.lock').open('a+') as lock:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as exc:
                raise RuntimeError('An unattended documentation controller is already running') from exc
            # Use the historical scheduler's lock to prohibit simultaneous source
            # campaigns. Its exhausted 702 tasks must not be dispatched again.
            with (self.root / 'doc-prune-work/campaign/campaign.lock').open('a+') as source_lock:
                fcntl.flock(source_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                if self.state.get('status') in TERMINAL:
                    return self.state
                if self.state.get('status') in ['blocked', 'admission_unknown']:
                    raise RuntimeError('Recorded failure requires diagnosis; no automatic failed/quota retry')
                if not self.state.get('session_id'):
                    session = create_session(self.api, self.root)
                    active = self.root / 'stonecutter.active'
                    self.save(session_id=session, status='ready', pid=os.getpid(),
                              initial_active_target=active.read_text().strip() if active.exists() else None)
                session = self.state['session_id']
                self.save(pid=os.getpid())
                completed_here = 0
                try:
                    while True:
                        if self.state.get('inflight'):
                            turn = self.state['turn']
                            self.collect(session, turn)
                            self.save(inflight=False, status='ready')
                            self.event('turn_completed', turn=turn, session_id=session)
                            completed_here += 1
                        if (self.work / 'STOP').exists():
                            break
                        manifest = load(self.work / 'completion.json')
                        if completion(manifest, self.root):
                            self.save(status=manifest['status'], completion_file=str(self.work / 'completion.json'))
                            self.event('campaign_completed', status=manifest['status'])
                            return self.state
                        if max_turns is not None and completed_here >= max_turns:
                            self.save(status='ready')
                            return self.state
                        turn = self.state['turn'] + 1
                        prompt = (self.work / 'instructions.md').read_text()
                        authorization = self.state.get('recovery_authorization')
                        if authorization:
                            notice = self.root / authorization
                            notice = notice.with_name('recovery-notice.txt')
                            if notice.is_file():
                                prompt += '\n\n' + notice.read_text()
                        prompt += (f'\n\nCONTROLLER TURN {turn}. Resume from your own durable campaign plan and checkpoint. '
                                   'A final response only completes this turn, not the campaign. '
                                   'Finish the next bounded unit, update durable progress, and continue work while feasible. '
                                   'Only write autonomous/completion.json after all four full-campaign stages satisfy '
                                   'the documented completion schema. Do not stop at exhaustion of a small followup queue.\n')
                        message_id = 'msg_' + uuid.uuid4().hex
                        write_json(self.work / 'turns' / f'{turn:06d}-prompt.json', {'id': message_id, 'text': prompt})
                        # Save before admission. If the CLI dies after delivery,
                        # uncertainty cannot accidentally create a duplicate turn.
                        self.save(turn=turn, message_id=message_id, status='admission_unknown')
                        self.api.request('POST', f'/api/session/{session}/prompt', {
                            'id': message_id, 'text': prompt, 'delivery': 'queue',
                        })
                        self.save(inflight=True, status='running')
                        self.event('turn_admitted', turn=turn, session_id=session, message_id=message_id)
                    self.save(status='stopped')
                    return self.state
                except Exception as exc:
                    # Preserve inflight identity for manual recovery/diagnosis; do
                    # not resend a failed agent or silently approve permissions.
                    self.save(status='blocked', error=f'{type(exc).__name__}: {exc}')
                    self.event('controller_blocked', error=self.state['error'])
                    raise


def probe(api, work=WORK):
    sid = create_session(api, ROOT, probe=True)
    api.request('POST', f'/api/session/{sid}/prompt', {'text':
        'Read docs/STYLE.md and doc-prune-work/campaign/source-runner.json in this repository. '
        'This is a read-only capability probe; do not edit, delegate, run shell commands, or begin the campaign. '
        'Return exactly JSON {"ready":true,"prose_repairs":<actual current completed_prose_repairs>}.'})
    c = Controller(api=api, work=work)
    c.wait_idle(sid)
    info = api.request('GET', f'/api/session/{sid}')['data']
    messages = api.request('GET', f'/api/session/{sid}/message?order=desc&type=assistant&limit=1')
    texts = [part['text'] for message in messages['data'] for part in message.get('content', []) if part.get('type') == 'text']
    parsed = json.loads(texts[-1]) if texts else {}
    actual = load(docs_prune.WORK / 'source-runner.json')['completed_prose_repairs']
    if parsed != {'ready': True, 'prose_repairs': actual} or info.get('outcome') in ['failed', 'interrupted']:
        raise RuntimeError(f'Live overseer capability probe failed: {parsed}')
    evidence = {'session_id': sid, 'model': info.get('model'), 'response': parsed, 'time': time.time()}
    write_json(work / 'live-probe.json', evidence)
    return evidence


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['run', 'resume', 'recover-quota', 'status', 'stop', 'probe'])
    parser.add_argument('--confirm-reset', action='store_true', help='explicitly confirm the usage quota has reset; only for recover-quota')
    parser.add_argument('--max-turns', type=int, help='diagnostic limit only; normal unattended runs have no turn limit')
    args = parser.parse_args()
    if args.confirm_reset and args.command != 'recover-quota':
        parser.error('--confirm-reset is only valid with recover-quota')
    if args.command == 'status':
        result = load(WORK / 'controller-state.json', {'status': 'not_started'})
    elif args.command == 'stop':
        WORK.mkdir(parents=True, exist_ok=True)
        (WORK / 'STOP').write_text('Stop after the current overseer turn; never discard source work.\n')
        result = {'stop_requested': True}
    elif args.command == 'probe':
        result = probe(docs_prune.API())
    elif args.command == 'recover-quota':
        result = Controller().recover_quota(confirmed=args.confirm_reset)
        # Start the persistent service, not a foreground loop tied to a terminal.
        # If this fails, retain the ready authorization; rerunning this explicit
        # command can start it without erasing the original failure.
        subprocess.run(['systemctl', '--user', 'start', SERVICE], check=True, timeout=30)
        result['service_start_requested'] = SERVICE
    else:
        if args.command == 'resume':
            state = load(WORK / 'controller-state.json', {})
            if state.get('status') != 'stopped':
                raise RuntimeError('resume is only for an intentional stopped state, not a failed/quota retry')
            (WORK / 'STOP').unlink(missing_ok=True)
        result = Controller().run(args.max_turns)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
