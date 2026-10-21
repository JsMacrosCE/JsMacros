import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import docs_campaign as campaign


class FakeAPI:
    def __init__(self, fail=False, admission_fail=False, timeout=False):
        self.calls = []
        self.fail = fail
        self.admission_fail = admission_fail
        self.timeout = timeout

    def request(self, method, path, payload=None, timeout=30):
        self.calls.append((method, path, payload))
        if path.startswith('/api/agent?'):
            return {'data': [{'id': 'docs-overseer'}]}
        if path == '/api/session':
            return {'data': {'id': 'ses_test', 'model': campaign.MODEL}}
        if path.endswith('/prompt'):
            if self.admission_fail:
                raise subprocess.TimeoutExpired('opencode', timeout)
            return {'data': {'id': 'inbox_test'}}
        if path.endswith('/wait'):
            if self.timeout:
                self.timeout = False
                raise subprocess.TimeoutExpired('opencode', timeout)
            return None
        if path.endswith('/permission') or path.endswith('/form'):
            return {'data': []}
        if '/message?' in path:
            return {'data': [{'content': [{'type': 'text', 'text': 'bounded batch complete'}]}]}
        return {'data': {'id': 'ses_test', 'outcome': 'failed' if self.fail else 'succeeded'}}


class QuotaAPI(FakeAPI):
    def __init__(self):
        super().__init__()
        self.error_type = 'provider.quota'
        self.outcome = 'failed'
        self.active = {}
        self.other_project = 'unrelated-project'
        self.pending = {}
        self.user_id = 'msg_failed'
        self.assistant_created = 200
        self.terminal_messages = None

    def request(self, method, path, payload=None, timeout=30):
        self.calls.append((method, path, payload))
        if path == '/api/session/active':
            return {'data': self.active}
        if path.startswith('/api/shell?'):
            return {'data': self.pending.get('shell', [])}
        for endpoint in ['inbox', 'permission', 'form']:
            if path.endswith('/' + endpoint):
                return {'data': self.pending.get(endpoint, [])}
        if '/message?' in path:
            if 'type=user' in path:
                return {'data': [{'id': self.user_id, 'type': 'user', 'time': {'created': 100}}]}
            message = {'id': 'msg_quota', 'type': 'assistant', 'time': {'created': self.assistant_created},
                       'error': {'type': self.error_type, 'message': 'Usage limit reached'} if self.error_type else None}
            if 'type=assistant' not in path and self.terminal_messages is not None:
                return {'data': self.terminal_messages}
            return {'data': [message]}
        if path == '/api/session/ses_test':
            return {'data': {'id': 'ses_test', 'projectID': 'campaign-project',
                             'outcome': self.outcome, 'agent': campaign.AGENT, 'model': campaign.MODEL}}
        if path.startswith('/api/session/'):
            return {'data': {'projectID': self.other_project}}
        raise AssertionError(f'Unexpected recovery API call: {method} {path}')


class WaitErrorAPI(FakeAPI):
    def __init__(self, failures=1, active=True, fail=False):
        super().__init__(fail=fail)
        self.failures = failures
        self.active = active
        self.wait_calls = 0
        self.pending = []

    def request(self, method, path, payload=None, timeout=30):
        if path.endswith('/wait'):
            self.calls.append((method, path, payload))
            self.wait_calls += 1
            if self.wait_calls <= self.failures:
                raise subprocess.CalledProcessError(1, ['opencode', 'api', 'POST', path], stderr='Socket closed')
            return None
        if path == '/api/session/active':
            self.calls.append((method, path, payload))
            return {'data': {'ses_test': {'type': 'running'}} if self.active else {}}
        if path.endswith('/permission') or path.endswith('/form'):
            self.calls.append((method, path, payload))
            return {'data': self.pending}
        return super().request(method, path, payload, timeout)


class CompletedWaitAPI(QuotaAPI):
    def __init__(self):
        super().__init__()
        self.outcome = 'succeeded'
        self.user_id = 'msg_failed'
        self.completed = 200
        self.finish = 'stop'

    def request(self, method, path, payload=None, timeout=30):
        if '/message?' in path:
            self.calls.append((method, path, payload))
            if 'type=user' in path:
                return {'data': [{'id': self.user_id, 'time': {'created': 100}}]}
            return {'data': [{'id': 'msg_completed', 'time': {'completed': self.completed},
                              'finish': self.finish, 'content': [{'type': 'text', 'text': 'Completed turn.'}]}]}
        return super().request(method, path, payload, timeout)


class ControllerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.work = self.root / 'doc-prune-work/campaign/autonomous'
        self.work.mkdir(parents=True)
        (self.work / 'instructions.md').write_text('Continue the finite task queue.')
        (self.root / 'stonecutter.active').write_text('1.21.8\n')

    def controller(self, api):
        return campaign.Controller(self.root, self.work, api)

    def test_successful_turn_continues_without_user_message(self):
        api = FakeAPI()
        state = self.controller(api).run(max_turns=2)
        self.assertEqual(state['turn'], 2)
        self.assertEqual(state['status'], 'ready')
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 2)
        self.assertEqual(sum(path == '/api/session' for _, path, _ in api.calls), 1)
        self.assertTrue((self.work / 'turns/000002-response.json').exists())

    def test_transport_wait_does_not_duplicate_prompt(self):
        api = FakeAPI(timeout=True)
        self.controller(api).run(max_turns=1)
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)
        self.assertEqual(sum(path.endswith('/wait') for _, path, _ in api.calls), 2)

    def test_broken_wait_connection_collects_success_without_replaying_prompt(self):
        api = WaitErrorAPI(active=False)
        state = self.controller(api).run(max_turns=1)
        self.assertEqual(state['status'], 'ready')
        self.assertEqual(api.wait_calls, 1)
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)
        event = next(json.loads(line) for line in (self.work / 'events.jsonl').read_text().splitlines()
                     if json.loads(line)['kind'] == 'wait_transport_error')
        self.assertEqual(event['stderr'], 'Socket closed')

    def test_broken_wait_connection_retries_only_same_session_observation(self):
        api = WaitErrorAPI(failures=2)
        with patch.object(campaign.time, 'sleep'):
            self.controller(api).run(max_turns=1)
        self.assertEqual(api.wait_calls, 3)
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)
        self.assertTrue(all(path == '/api/experimental/session/ses_test/wait'
                            for _, path, _ in api.calls if path.endswith('/wait')))

    def test_broken_wait_connection_retries_are_bounded(self):
        api = WaitErrorAPI(failures=10)
        with patch.object(campaign.time, 'sleep'):
            with self.assertRaises(subprocess.CalledProcessError):
                self.controller(api).run(max_turns=1)
        self.assertEqual(api.wait_calls, 3)
        self.assertEqual(campaign.load(self.work / 'controller-state.json')['status'], 'blocked')

    def test_broken_wait_connection_never_retries_quota_failed_turn(self):
        api = WaitErrorAPI(active=False, fail=True)
        with self.assertRaisesRegex(RuntimeError, 'no automatic retry'):
            self.controller(api).run(max_turns=2)
        self.assertEqual(api.wait_calls, 1)
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)

    def test_broken_wait_connection_does_not_approve_interactive_input(self):
        api = WaitErrorAPI()
        api.pending = [{'id': 'permission_request'}]
        with self.assertRaisesRegex(RuntimeError, 'no automatic approval'):
            self.controller(api).run(max_turns=1)
        self.assertEqual(api.wait_calls, 1)

    def test_broken_wait_connection_blocks_when_observation_is_unavailable(self):
        api = WaitErrorAPI()
        original = api.request
        def unavailable(method, path, payload=None, timeout=30):
            if path == '/api/session/active':
                raise subprocess.CalledProcessError(1, ['opencode', 'api', 'GET', path])
            return original(method, path, payload, timeout)
        api.request = unavailable
        with self.assertRaises(subprocess.CalledProcessError):
            self.controller(api).run(max_turns=1)
        self.assertEqual(api.wait_calls, 1)
        self.assertEqual(campaign.load(self.work / 'controller-state.json')['status'], 'blocked')

    def test_resume_inflight_collects_existing_turn(self):
        campaign.write_json(self.work / 'controller-state.json', {
            'session_id': 'ses_test', 'status': 'running', 'inflight': True,
            'turn': 1, 'initial_active_target': '1.21.8',
        })
        api = FakeAPI()
        self.controller(api).run(max_turns=1)
        self.assertFalse(any(path.endswith('/prompt') for _, path, _ in api.calls))

    def test_quota_or_failed_agent_is_not_retried(self):
        api = FakeAPI(fail=True)
        with self.assertRaisesRegex(RuntimeError, 'no automatic retry'):
            self.controller(api).run(max_turns=2)
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)
        with self.assertRaisesRegex(RuntimeError, 'Recorded failure'):
            self.controller(FakeAPI()).run(max_turns=1)

    def test_admission_timeout_is_never_resent(self):
        api = FakeAPI(admission_fail=True)
        with self.assertRaises(subprocess.TimeoutExpired):
            self.controller(api).run(max_turns=1)
        with self.assertRaisesRegex(RuntimeError, 'Recorded failure'):
            self.controller(FakeAPI()).run(max_turns=1)

    def test_stop_marker_does_not_discard_itself_or_dispatch(self):
        (self.work / 'STOP').write_text('stop')
        api = FakeAPI()
        state = self.controller(api).run()
        self.assertEqual(state['status'], 'stopped')
        self.assertTrue((self.work / 'STOP').exists())
        self.assertFalse(any(path.endswith('/prompt') for _, path, _ in api.calls))

    def test_stop_during_wait_settles_turn_without_admitting_next(self):
        work = self.work
        class StopAPI(FakeAPI):
            def request(self, method, path, payload=None, timeout=30):
                result = super().request(method, path, payload, timeout)
                if path.endswith('/wait'):
                    (work / 'STOP').write_text('intentional stop')
                return result
        api = StopAPI()
        state = self.controller(api).run(max_turns=2)
        self.assertEqual(state['status'], 'stopped')
        self.assertFalse(state['inflight'])
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)

    def test_active_version_change_is_blocked(self):
        campaign.write_json(self.work / 'controller-state.json', {
            'session_id': 'ses_test', 'status': 'running', 'inflight': True,
            'turn': 1, 'initial_active_target': '1.21.5',
        })
        with self.assertRaisesRegex(RuntimeError, 'Stonecutter target changed'):
            self.controller(FakeAPI()).run(max_turns=1)

    def test_completion_requires_full_stage_accounting(self):
        with self.assertRaisesRegex(ValueError, 'four campaign stages'):
            campaign.completion({'status': 'complete'}, self.root)
        self.assertFalse(campaign.completion({'status': 'batch_complete'}, self.root))

    def manifest(self):
        evidence = []
        for role in ['coverage_inventory', 'source_reviews', 'validation', 'final_report']:
            path = self.root / (role + '.json')
            path.write_text('{}')
            evidence.append({'role': role, 'path': path.name,
                             'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
        return {'status': 'complete', 'stages': {s: 'complete' for s in [
            'existing_documentation', 'audience_and_metadata', 'undocumented_api_coverage', 'final_validation']},
            'pending_tasks': 0, 'blockers': [], 'evidence': evidence}

    def test_completion_hashes_and_evidence_roles_are_checked(self):
        manifest = self.manifest()
        self.assertTrue(campaign.completion(manifest, self.root))
        manifest['evidence'][0]['sha256'] = 'wrong'
        with self.assertRaisesRegex(ValueError, 'hash mismatch'):
            campaign.completion(manifest, self.root)

    def test_completion_cannot_hide_blockers(self):
        manifest = self.manifest()
        manifest['blockers'] = [{'id': 'real_limit'}]
        with self.assertRaisesRegex(ValueError, 'Unqualified completion'):
            campaign.completion(manifest, self.root)

    def test_pending_tasks_prevent_completion(self):
        manifest = self.manifest()
        manifest['pending_tasks'] = 1
        with self.assertRaisesRegex(ValueError, 'zero runnable'):
            campaign.completion(manifest, self.root)

    def test_complete_manifest_ends_without_another_prompt(self):
        campaign.write_json(self.work / 'completion.json', self.manifest())
        api = FakeAPI()
        state = self.controller(api).run()
        self.assertEqual(state['status'], 'complete')
        self.assertFalse(any(path.endswith('/prompt') for _, path, _ in api.calls))

    def test_probe_permissions_deny_writes_and_questions(self):
        rules = campaign.permissions(self.root, probe=True)
        self.assertFalse(any(r['action'] in ['edit', 'shell', 'subagent'] and r['effect'] == 'allow' for r in rules))
        self.assertIn({'action': 'question', 'resource': '*', 'effect': 'deny'}, rules)

    def blocked_state(self):
        state = {'session_id': 'ses_test', 'status': 'blocked', 'inflight': True,
                 'turn': 49, 'initial_active_target': '1.21.8', 'message_id': 'msg_failed',
                 'error': 'provider failure recorded'}
        campaign.write_json(self.work / 'controller-state.json', state)
        campaign.write_json(self.work / 'plan.json', {'tasks': [{'editor_session': 'ses_worker', 'state': 'failed'}]})
        return state

    def test_quota_recovery_requires_explicit_confirmation(self):
        original = self.blocked_state()
        api = QuotaAPI()
        with self.assertRaisesRegex(RuntimeError, '--confirm-reset'):
            self.controller(api).recover_quota()
        self.assertEqual(api.calls, [])
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_confirmed_quota_recovery_preserves_failure_and_same_session(self):
        original = self.blocked_state()
        api = QuotaAPI()
        result = self.controller(api).recover_quota(confirmed=True)
        state = campaign.load(self.work / 'controller-state.json')
        self.assertEqual(result['next_turn'], 50)
        self.assertEqual(state['status'], 'ready')
        self.assertFalse(state['inflight'])
        self.assertEqual(state['session_id'], original['session_id'])
        self.assertEqual(state['turn'], original['turn'])
        self.assertNotIn('error', state)
        recovery = self.root / state['last_failure']['evidence']
        self.assertEqual(campaign.load(recovery / 'controller-state-before.json'), original)
        self.assertEqual(campaign.load(recovery / 'plan-before.json')['tasks'][0]['state'], 'failed')
        self.assertTrue((recovery / 'recovery-notice.txt').is_file())
        self.assertFalse(any(method != 'GET' for method, _, _ in api.calls))

    def test_quota_recovery_refuses_other_failures_and_keeps_state(self):
        original = self.blocked_state()
        api = QuotaAPI()
        api.error_type = 'provider.authentication'
        with self.assertRaisesRegex(RuntimeError, 'not provider.quota'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_quota_recovery_refuses_interrupted_outcome(self):
        self.blocked_state()
        api = QuotaAPI()
        api.outcome = 'interrupted'
        with self.assertRaisesRegex(RuntimeError, 'settled failed'):
            self.controller(api).recover_quota(confirmed=True)

    def test_quota_recovery_refuses_ambiguous_admission(self):
        state = self.blocked_state()
        state['status'] = 'admission_unknown'
        campaign.write_json(self.work / 'controller-state.json', state)
        with self.assertRaisesRegex(RuntimeError, 'blocked quota'):
            self.controller(QuotaAPI()).recover_quota(confirmed=True)

    def test_quota_recovery_blocks_active_campaign_worker(self):
        original = self.blocked_state()
        api = QuotaAPI()
        api.active = {'ses_worker': {'type': 'running'}}
        with self.assertRaisesRegex(RuntimeError, 'Campaign session is still active'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_quota_recovery_blocks_unrecorded_repository_session(self):
        self.blocked_state()
        api = QuotaAPI()
        api.active = {'ses_unknown': {'type': 'running'}}
        api.other_project = 'campaign-project'
        with self.assertRaisesRegex(RuntimeError, 'in this repository is still active'):
            self.controller(api).recover_quota(confirmed=True)

    def test_quota_recovery_does_not_interrupt_unrelated_projects(self):
        self.blocked_state()
        api = QuotaAPI()
        api.active = {'ses_unknown': {'type': 'running'}}
        self.controller(api).recover_quota(confirmed=True)
        self.assertFalse(any(method != 'GET' for method, _, _ in api.calls))

    def test_quota_recovery_refuses_unknown_active_session_ownership(self):
        self.blocked_state()
        api = QuotaAPI()
        api.active = {'ses_unknown': {'type': 'running'}}
        api.other_project = None
        with self.assertRaisesRegex(RuntimeError, 'Cannot establish active session ownership'):
            self.controller(api).recover_quota(confirmed=True)

    def test_quota_recovery_rejects_pending_input_or_shell_work(self):
        for endpoint in ['inbox', 'permission', 'form', 'shell']:
            with self.subTest(endpoint=endpoint):
                original = self.blocked_state()
                api = QuotaAPI()
                api.pending = {endpoint: [{'id': 'pending'}]}
                with self.assertRaisesRegex(RuntimeError, 'recovery refused'):
                    self.controller(api).recover_quota(confirmed=True)
                self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_quota_recovery_preserves_intentional_stop(self):
        self.blocked_state()
        (self.work / 'STOP').write_text('intentional stop')
        with self.assertRaisesRegex(RuntimeError, 'must not override'):
            self.controller(QuotaAPI()).recover_quota(confirmed=True)
        self.assertTrue((self.work / 'STOP').exists())

    def test_quota_recovery_refuses_active_version_change(self):
        self.blocked_state()
        (self.root / 'stonecutter.active').write_text('26.1.2')
        with self.assertRaisesRegex(RuntimeError, 'target is missing or changed'):
            self.controller(QuotaAPI()).recover_quota(confirmed=True)

    def test_quota_recovery_reloads_state_under_lock(self):
        self.blocked_state()
        controller = self.controller(QuotaAPI())
        campaign.write_json(self.work / 'controller-state.json', {'status': 'running'})
        with self.assertRaisesRegex(RuntimeError, 'blocked quota'):
            controller.recover_quota(confirmed=True)

    def test_quota_recovery_archive_failure_does_not_unblock(self):
        original = self.blocked_state()
        with patch.object(campaign, 'write_json', side_effect=OSError('storage full')):
            with self.assertRaisesRegex(OSError, 'storage full'):
                self.controller(QuotaAPI()).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_quota_recovery_prepared_state_reusable_after_service_start_failure(self):
        self.blocked_state()
        self.controller(QuotaAPI()).recover_quota(confirmed=True)
        before = campaign.load(self.work / 'controller-state.json')
        result = self.controller(QuotaAPI()).recover_quota(confirmed=True)
        self.assertTrue(result['already_prepared'])
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), before)
        self.assertEqual(len(list((self.work / 'recovery').iterdir())), 1)

    def test_quota_recovery_notice_reaches_next_prompt(self):
        self.blocked_state()
        self.controller(QuotaAPI()).recover_quota(confirmed=True)
        api = FakeAPI()
        self.controller(api).run(max_turns=1)
        prompt = next(payload['text'] for _, path, payload in api.calls if path.endswith('/prompt'))
        self.assertIn('EXPLICIT USER QUOTA RESET RECOVERY', prompt)
        self.assertIn('CONTROLLER TURN 50', prompt)

    def test_quota_recovery_cli_starts_persistent_service_after_preparation(self):
        with patch.object(sys, 'argv', ['docs_campaign.py', 'recover-quota', '--confirm-reset']), \
             patch.object(campaign, 'Controller') as factory, \
             patch.object(campaign.subprocess, 'run') as service_start, \
             patch('builtins.print'):
            factory.return_value.recover_quota.return_value = {'status': 'ready', 'next_turn': 50}
            campaign.main()
            factory.return_value.recover_quota.assert_called_once_with(confirmed=True)
            service_start.assert_called_once_with(
                ['systemctl', '--user', 'start', campaign.SERVICE], check=True, timeout=30)
            factory.return_value.run.assert_not_called()

    def test_quota_recovery_cli_does_not_start_service_when_safety_check_fails(self):
        with patch.object(sys, 'argv', ['docs_campaign.py', 'recover-quota', '--confirm-reset']), \
             patch.object(campaign, 'Controller') as factory, \
             patch.object(campaign.subprocess, 'run') as service_start:
            factory.return_value.recover_quota.side_effect = RuntimeError('Worker active')
            with self.assertRaisesRegex(RuntimeError, 'Worker active'):
                campaign.main()
            service_start.assert_not_called()

    def blocked_wait_state(self):
        state = self.blocked_state()
        state['error'] = ("CalledProcessError: Command '['opencode', 'api', 'POST', "
                          "'/api/experimental/session/ses_test/wait', '--data', '{}']' returned non-zero exit status 1.")
        campaign.write_json(self.work / 'controller-state.json', state)
        return state

    def test_completed_wait_recovery_keeps_completed_turn_inflight_for_collection(self):
        original = self.blocked_wait_state()
        api = CompletedWaitAPI()
        result = self.controller(api).recover_quota(confirmed=True)
        state = campaign.load(self.work / 'controller-state.json')
        self.assertTrue(result['collect_completed_turn_first'])
        self.assertTrue(state['inflight'])
        self.assertEqual(state['turn'], 49)
        self.assertEqual(state['recovery_kind'], 'completed_wait')
        recovery = self.root / state['last_failure']['evidence']
        self.assertEqual(campaign.load(recovery / 'controller-state-before.json'), original)
        self.assertEqual(campaign.load(recovery / 'authorization.json')['failure_type'], 'wait_transport')
        self.assertTrue((recovery / 'completed-response.json').is_file())
        self.assertFalse(any(method != 'GET' for method, _, _ in api.calls))
        collector = FakeAPI()
        self.controller(collector).run(max_turns=1)
        self.assertFalse(any(path.endswith('/prompt') for _, path, _ in collector.calls))
        self.assertEqual(campaign.load(self.work / 'controller-state.json')['turn'], 49)

    def test_completed_wait_preparation_is_reusable_without_dropping_inflight_turn(self):
        self.blocked_wait_state()
        self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)
        result = self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)
        self.assertTrue(result['already_prepared'])
        self.assertTrue(result['collect_completed_turn_first'])
        self.assertTrue(campaign.load(self.work / 'controller-state.json')['inflight'])

    def test_completed_wait_recovery_rejects_unrelated_success(self):
        original = self.blocked_wait_state()
        api = CompletedWaitAPI()
        api.user_id = 'msg_some_other_prompt'
        with self.assertRaisesRegex(RuntimeError, 'does not match'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_completed_wait_recovery_requires_success_after_admitted_prompt(self):
        self.blocked_wait_state()
        api = CompletedWaitAPI()
        api.completed = 50
        with self.assertRaisesRegex(RuntimeError, 'does not match'):
            self.controller(api).recover_quota(confirmed=True)

    def test_completed_wait_recovery_rejects_partial_response(self):
        self.blocked_wait_state()
        api = CompletedWaitAPI()
        api.finish = 'tool-calls'
        with self.assertRaisesRegex(RuntimeError, 'completed successful'):
            self.controller(api).recover_quota(confirmed=True)

    def test_completed_wait_recovery_requires_actual_wait_transport_failure(self):
        for error in ['ValueError: source safety failed',
                      "CalledProcessError: Command '['opencode', 'api', 'POST', '/api/session/ses_test/prompt']' failed"]:
            with self.subTest(error=error):
                state = self.blocked_wait_state()
                state['error'] = error
                campaign.write_json(self.work / 'controller-state.json', state)
                with self.assertRaisesRegex(RuntimeError, 'settled failed'):
                    self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)

    def test_completed_wait_recovery_still_blocks_active_workers_and_pending_inputs(self):
        for reason in ['active', 'inbox', 'form', 'permission', 'shell']:
            with self.subTest(reason=reason):
                original = self.blocked_wait_state()
                api = CompletedWaitAPI()
                if reason == 'active':
                    api.active = {'ses_worker': {'type': 'running'}}
                else:
                    api.pending = {reason: [{'id': 'pending'}]}
                with self.assertRaises(RuntimeError):
                    self.controller(api).recover_quota(confirmed=True)
                self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def blocked_missing_cli_state(self):
        state = self.blocked_state()
        state['error'] = ("FileNotFoundError: [Errno 2] No such file or directory: "
                          "'/home/user/.local/share/mise/installs/npm-opencode-cli/2.0.16/node_modules/.bin/opencode'")
        campaign.write_json(self.work / 'controller-state.json', state)
        return state

    def test_missing_cli_recovery_collects_completed_turn_without_replaying_it(self):
        original = self.blocked_missing_cli_state()
        result = self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)
        state = campaign.load(self.work / 'controller-state.json')
        self.assertTrue(result['collect_completed_turn_first'])
        self.assertTrue(state['inflight'])
        recovery = self.root / state['last_failure']['evidence']
        self.assertEqual(campaign.load(recovery / 'controller-state-before.json'), original)
        record = campaign.load(recovery / 'authorization.json')
        self.assertEqual(record['failure_type'], 'missing_cli')
        self.assertTrue(record['missing_cli_path'].endswith('/.bin/opencode'))
        api = FakeAPI()
        self.controller(api).run(max_turns=1)
        self.assertFalse(any(path.endswith('/prompt') for _, path, _ in api.calls))

    def test_missing_cli_recognition_does_not_accept_other_missing_files(self):
        errors = [
            "FileNotFoundError: [Errno 2] No such file or directory: '/repo/protected-byte-evidence.json'",
            "FileNotFoundError: [Errno 2] No such file or directory: 'opencode'",
            "FileNotFoundError: [Errno 2] No such file or directory: not a quoted path",
            "FileNotFoundError: [Errno 2] No such file or directory: []",
        ]
        for error in errors:
            with self.subTest(error=error):
                self.assertIsNone(campaign.missing_cli_path(error))
                state = self.blocked_missing_cli_state()
                state['error'] = error
                campaign.write_json(self.work / 'controller-state.json', state)
                with self.assertRaisesRegex(RuntimeError, 'no retry was attempted'):
                    self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)
                self.assertEqual(campaign.load(self.work / 'controller-state.json'), state)

    def test_missing_cli_recovery_still_requires_exact_admitted_prompt(self):
        original = self.blocked_missing_cli_state()
        api = CompletedWaitAPI()
        api.user_id = 'msg_unrelated'
        with self.assertRaisesRegex(RuntimeError, 'does not match'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_missing_cli_recovery_checks_active_workers_pending_inputs_and_shells(self):
        for reason in ['active', 'inbox', 'form', 'permission', 'shell']:
            with self.subTest(reason=reason):
                original = self.blocked_missing_cli_state()
                api = CompletedWaitAPI()
                if reason == 'active':
                    api.active = {'ses_worker': {'type': 'running'}}
                else:
                    api.pending = {reason: [{'id': 'pending'}]}
                with self.assertRaises(RuntimeError):
                    self.controller(api).recover_quota(confirmed=True)
                self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_missing_cli_recovery_is_reusable_after_service_start_failure(self):
        self.blocked_missing_cli_state()
        self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)
        result = self.controller(CompletedWaitAPI()).recover_quota(confirmed=True)
        self.assertTrue(result['already_prepared'])
        self.assertTrue(campaign.load(self.work / 'controller-state.json')['inflight'])
        self.assertEqual(len(list((self.work / 'recovery').iterdir())), 1)

    def test_missing_cli_recovery_cannot_override_interrupted_session(self):
        original = self.blocked_missing_cli_state()
        api = CompletedWaitAPI()
        api.outcome = 'interrupted'
        with self.assertRaisesRegex(RuntimeError, 'no retry was attempted'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_service_cli_path_is_not_tied_to_removed_mise_version_directory(self):
        service = (Path(__file__).resolve().parents[1] / 'docs_campaign.service').read_text()
        self.assertIn('Environment=OPENCODE_BIN=%h/.local/share/npm/bin/opencode', service)
        self.assertNotIn('/installs/npm-opencode-cli/', service)

    def compaction_quota_api(self, idle_error=True):
        api = QuotaAPI()
        api.error_type = None
        api.assistant_created = 50
        quota = {'type': 'provider.quota', 'message': 'The usage limit has been reached'}
        api.terminal_messages = [
            {'id': 'msg_idle', 'type': 'idle', 'time': {'created': 300},
             'error': quota if idle_error else None},
            {'id': 'msg_compaction', 'type': 'compaction', 'time': {'created': 200}, 'error': quota},
            {'id': 'msg_failed', 'type': 'user', 'time': {'created': 100}},
            {'id': 'msg_previous_assistant', 'type': 'assistant', 'time': {'created': 50}, 'finish': 'stop'},
        ]
        return api

    def test_compaction_quota_before_any_assistant_response_is_recoverable(self):
        original = self.blocked_state()
        api = self.compaction_quota_api()
        result = self.controller(api).recover_quota(confirmed=True)
        state = campaign.load(self.work / 'controller-state.json')
        self.assertEqual(result['next_turn'], 50)
        self.assertFalse(state['inflight'])
        recovery = self.root / state['last_failure']['evidence']
        self.assertEqual(campaign.load(recovery / 'controller-state-before.json'), original)
        self.assertEqual(campaign.load(recovery / 'quota-error-response.json')['data'], api.terminal_messages)
        record = campaign.load(recovery / 'authorization.json')
        self.assertEqual(record['failure_type'], 'provider.quota')
        self.assertEqual(record['failure_message_type'], 'idle')
        self.assertEqual(record['failure_message_id'], 'msg_idle')
        self.assertFalse(any(method != 'GET' for method, _, _ in api.calls))

    def test_compaction_quota_is_found_when_idle_has_no_error(self):
        self.blocked_state()
        result = self.controller(self.compaction_quota_api(idle_error=False)).recover_quota(confirmed=True)
        record = campaign.load(self.root / result['authorization'])
        self.assertEqual(record['failure_message_type'], 'compaction')

    def test_idle_quota_is_recoverable_without_a_compaction_record(self):
        self.blocked_state()
        api = self.compaction_quota_api()
        api.terminal_messages = [api.terminal_messages[0]]
        self.controller(api).recover_quota(confirmed=True)

    def test_previous_turn_quota_is_not_used_for_current_failure(self):
        original = self.blocked_state()
        api = QuotaAPI()
        api.assistant_created = 50
        with self.assertRaisesRegex(RuntimeError, 'not provider.quota'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_newer_non_quota_error_is_not_skipped_for_older_compaction_quota(self):
        original = self.blocked_state()
        api = self.compaction_quota_api()
        api.terminal_messages[0]['error'] = {'type': 'provider.authentication', 'message': 'Not quota'}
        with self.assertRaisesRegex(RuntimeError, 'provider.authentication'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_compaction_non_quota_error_does_not_fall_back_to_previous_quota_reply(self):
        original = self.blocked_state()
        api = self.compaction_quota_api(idle_error=False)
        api.error_type = 'provider.quota'
        api.terminal_messages[1]['error'] = {'type': 'provider.authentication'}
        with self.assertRaisesRegex(RuntimeError, 'provider.authentication'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_compaction_quota_requires_exact_admitted_user_message(self):
        original = self.blocked_state()
        api = self.compaction_quota_api()
        api.user_id = 'msg_unrelated'
        with self.assertRaisesRegex(RuntimeError, 'does not match'):
            self.controller(api).recover_quota(confirmed=True)
        self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_compaction_quota_does_not_override_other_safety_guards(self):
        for reason in ['active', 'inbox', 'form', 'permission', 'shell']:
            with self.subTest(reason=reason):
                original = self.blocked_state()
                api = self.compaction_quota_api()
                if reason == 'active':
                    api.active = {'ses_worker': {'type': 'running'}}
                else:
                    api.pending = {reason: [{'id': 'pending'}]}
                with self.assertRaises(RuntimeError):
                    self.controller(api).recover_quota(confirmed=True)
                self.assertEqual(campaign.load(self.work / 'controller-state.json'), original)

    def test_admitted_error_requires_timestamped_current_terminal_message(self):
        user = {'time': {'created': 100}}
        self.assertIsNone(campaign.admitted_terminal_error([
            {'type': 'assistant', 'time': {'created': 50}, 'error': {'type': 'provider.quota'}},
        ], user))
        self.assertIsNone(campaign.admitted_terminal_error([
            {'type': 'compaction', 'error': {'type': 'provider.quota'}},
        ], user))
        self.assertIsNone(campaign.admitted_terminal_error([
            {'type': 'user', 'time': {'created': 200}, 'error': {'type': 'provider.quota'}},
        ], user))
        self.assertIsNone(campaign.admitted_terminal_error([], {}))

    def test_failed_collection_preserves_compaction_evidence_without_retry(self):
        api = FakeAPI(fail=True)
        original = api.request
        terminal = self.compaction_quota_api().terminal_messages
        def compaction(method, path, payload=None, timeout=30):
            if '/message?' in path and 'type=' not in path:
                api.calls.append((method, path, payload))
                return {'data': terminal}
            return original(method, path, payload, timeout)
        api.request = compaction
        with self.assertRaisesRegex(RuntimeError, 'no automatic retry'):
            self.controller(api).run(max_turns=2)
        self.assertEqual(campaign.load(self.work / 'turns/000001-failure-messages.json')['data'], terminal)
        self.assertEqual(sum(path.endswith('/prompt') for _, path, _ in api.calls), 1)


if __name__ == '__main__':
    unittest.main()
