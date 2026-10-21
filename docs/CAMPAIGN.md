# Unattended full documentation campaign

`scripts/docs_campaign.py` is the continuation controller for the existing
campaign. It uses the authenticated local `opencode api` integration and the
registered `docs-overseer` agent (GPT-6.1 Sol). Bounded editors and independent
source reviewers use GPT-6 Luna, as in `docs/PRUNING.md`.

The historical source scheduler in `scripts/docs_prune.py` remains intact. Its
702 completed tasks, immutable dirty-checkout baseline, transcripts, and
unresolved records are not reset or dispatched again. The continuation layer
adds full-campaign stage accounting rather than mistaking that exhausted queue
or a small follow-up queue for complete API documentation.

## Running and observing

```sh
python3 scripts/docs_campaign.py probe   # read-only live capability check
python3 scripts/docs_campaign.py run     # no turn limit; resumes its session
python3 scripts/docs_campaign.py status
python3 scripts/docs_campaign.py stop    # settle the current turn, retain work
python3 scripts/docs_campaign.py resume  # explicitly resume an intentional stop
python3 scripts/docs_campaign.py recover-quota --confirm-reset
```

The controller must have `doc-prune-work/campaign/autonomous/instructions.md`
before starting. The instructions and the stage plan are durable campaign state.
The controller holds both its own lock and the historical source-campaign lock,
so a second cooperating campaign cannot write this checkout concurrently. Do not
run `scripts/docs_prune.py run` inside a controller turn; its historical queue is
already complete, and the controller owns that lock.

State and evidence live under `doc-prune-work/campaign/autonomous/`:

- `controller-state.json`: PID, session, admitted message, turn, and outcome;
- `events.jsonl`: append-only controller events;
- `turns/`: admitted prompts, final session states, and responses;
- `plan.json`: overseer-maintained finite task inventory and stage progress;
- `completion.json`: terminal manifest only after full stage accounting;
- `FINAL_REPORT.md`: final results and individually scoped residual blockers.

The installed instance runs as the user service
`jsmacrosce-docs-campaign.service`; its template is `scripts/docs_campaign.service`.
It is enabled at login/boot and the current user has lingering enabled, so it
does not depend on keeping this conversation or terminal open. Observe it with:

```sh
systemctl --user status jsmacrosce-docs-campaign.service
python3 scripts/docs_campaign.py status
```

Output is appended to `autonomous/service.log`. The service restarts after an
abnormal controller termination, not after a normal failure exit (including
provider/quota failures). The controller resumes an admitted in-flight turn by
waiting on its recorded session. The installed service uses the version-independent
CLI entry point `%h/.local/share/npm/bin/opencode`, not a removable versioned
Mise installation directory. If the CLI moves to a different package manager or
prefix, update `OPENCODE_BIN` in both the installed unit and its template, reload
the user service manager, and recheck authenticated API access under the unit's
minimal environment before resuming. Do not install another OpenCode copy merely
to recreate a deleted version path.

A successful final response advances to another turn automatically. A transport
timeout waits on the **same** session; it does not start a duplicate writer.
If the wait CLI loses its connection, the controller checks the same session's
activity and outcome. A settled turn is collected normally; an active turn gets
at most three failed wait requests with short backoff. The controller records
the CLI error output. It never resends the editing prompt, retries a failed
provider turn, or approves interactive input during these checks. If the API
cannot establish the session state, the controller still stops safely.
Provider/quota failure, an ambiguous prompt admission, or an unexpected
permission/form is recorded and stops dispatch. These failures are not
automatically retried or approved. Normal operation needs no user prompts, but
an unavailable provider or powered-off machine cannot be made to finish by the
controller. The user service used for the launched instance can resume normal
work after process/service restarts; a recorded failure remains fail-closed.
An intentional `stop` persists across restarts; only the explicit `resume`
command clears it. `resume` refuses blocked/failed states and is not permission
to retry quota failures.

### After a usage quota resets

From the repository root, run:

```sh
python3 scripts/docs_campaign.py recover-quota --confirm-reset
```

This explicitly authorizes **one** recovery and starts the installed user
service, so you can close the terminal. It verifies that the latest overseer
error is `provider.quota`, that the same session has settled, and that no
campaign/repository session, shell command, queued input, permission request,
or form could duplicate work. Active sessions in unrelated projects are left
alone. Run from a terminal once repository sessions have become idle.

Quota exhaustion during context compaction can happen before a new assistant
reply exists. Recovery checks the current admitted prompt's assistant,
compaction, and final idle messages rather than assuming the last assistant
reply contains the error. It verifies the latest user-message ID and timestamps,
rejects quota errors from older turns, and does not skip a newer non-quota error
to find an older quota error. Failure collection and recovery archives retain
the terminal/compaction messages and the admitted-user record.

The command archives the failed state, quota response, plan, and authorization
before making the checkpoint ready. The next turn receives a recovery notice
to reconcile partial tasks rather than replay completed work. An intentional
`STOP`, changed Minecraft target, non-quota failure, or uncertain admission
still requires diagnosis; this command does not override those safeguards.
If service startup fails after recovery was prepared, the same command can be
run again to start it without erasing the archived failure.

The same command also handles a specific non-quota case: the controller's
read-only OpenCode client failed, but the **exact admitted prompt** subsequently
finished successfully. It verifies the latest user-message ID, completed
assistant response, settled session, and the same active-work guards, then
archives the local client failure separately. The completed turn stays in flight
for collection before the next turn is admitted; no edit, review, or generation
is repeated. This does not bypass other non-quota failures or uncertain prompt
admissions. An OpenCode service restart/reconfiguration can cause this socket
failure even while the overseer continues working. A missing absolute OpenCode
executable path is also recognized, with separate `missing_cli` evidence. Other
missing files are not treated as recoverable CLI failures. Fix and verify the
service's executable path first; this recovery does not change it automatically.

`--confirm-reset` is your confirmation, **not** a provider quota probe. Running
too early may hit the limit again and safely stop. No timer, automatic quota
retry, provider switch, or reset of the historical work queue is introduced.

## Stage and completion contract

The overseer owns four stages, in order:

1. Reconcile remaining **existing** documentation work using preserved evidence
   and a finite current inventory; do not repeat completed broad prose audits.
2. Finish source-backed audience/metadata decisions, retaining uncertain APIs and
   preserving source docs for excluded declarations.
3. Inventory and document still-undocumented intended scripting APIs in bounded
   source assignments, only after existing documentation work is reconciled.
4. Validate final explicit Minecraft snapshots, generated prose/typing fidelity,
   protected bytes, and independently reviewed source contracts.

Known history, audience, runtime, and deferred renderer limits may be quarantined
with evidence while independent work continues. They do not excuse stopping at
stage 1 or ignoring undocumented API coverage. They also must not be silently
promoted to resolved facts merely to claim full completion.

`completion.json` uses this schema:

```json
{
  "status": "complete_with_blockers",
  "stages": {
    "existing_documentation": "complete_with_blockers",
    "audience_and_metadata": "complete_with_blockers",
    "undocumented_api_coverage": "complete",
    "final_validation": "complete_with_blockers"
  },
  "pending_tasks": 0,
  "blockers": [
    {"id": "a-specific-limit", "reason": "Evidence and affected scope"}
  ],
  "evidence": [
    {"role": "coverage_inventory", "path": "relative/path.json", "sha256": "..."},
    {"role": "source_reviews", "path": "relative/path.json", "sha256": "..."},
    {"role": "validation", "path": "relative/path.json", "sha256": "..."},
    {"role": "final_report", "path": "relative/report.md", "sha256": "..."}
  ]
}
```

Only `complete` with all stages complete and no blockers permits an unqualified
completion claim. `complete_with_blockers` requires every runnable task finished
and every excluded/quarantined case individually accounted for. The controller
checks stage accounting, zero pending tasks, evidence roles, repository-local
paths, and their hashes. It does **not** prove that an agent's prose or evidence
is true: independent source reviews and direct output checks remain mandatory.

Never commit, push, reset, discard unrelated work, switch the active Minecraft
version, restart retired artifact reviewers, or retry quota failures implicitly.
Use the offline and explicit-target validation described in `docs/PRUNING.md`.
