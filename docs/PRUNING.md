# Resumable documentation pruning

`/docs-prune` selects the GPT-6.1 Sol overseer. All implementation, editing,
and source-review workers use GPT-6 Luna. The overseer manages
quality and blockers; a Python scheduler owns the durable queue rather than a
single growing conversation.

## Scope and ownership

Repair existing documentation before adding comments to undocumented members.
An exhausted comment queue does not establish complete API documentation
coverage. Calibrate a small mixed batch against the agreed structure in
`docs/STYLE.md` before any broader editing wave.

The campaign starts from the current checkout, including existing dirty work.
Its immutable source baseline, queue, and worker transcripts live under
`doc-prune-work/campaign/`. That directory is local campaign state, not generated
documentation or a replacement for source control. Never discard it during an
active campaign. No campaign operation commits, resets, or restores source.

Existing review-ledger and validation-matrix statuses are imported as historical
evidence, not upgraded to full verification. A partial audit and a spot-check
remain partial. Preserve audited `@since` history and user-owned edits,
especially `FWorld.java`.

### Purposeful delegation

Do not launch workers merely to keep the campaign running. Each assignment must
name a bounded question or change, the evidence required, and a completion
criterion. Supply established conventions in the prompt; workers should not
rediscover repository-wide style for each member. Review changed comments for
both accuracy and useful information removed from the original.

Use offline tests for syntax, extraction, presence, and renderer regressions.
Model-based artifact review is retired, including targeted artifact-review agents.
Directly inspect a small set of generated examples when changing a renderer or
documentation structure. Do not launch another model output-review campaign or
use artifact fidelity as evidence of runtime truth.
Implementation-backed research is for concrete unresolved behavioral claims,
not a repeated audit of every ordinary getter. Preserve failures and diagnose
them before any explicit retry; never blindly repeat failed bulk work.


Workers receive bounded comment groups in one source file. Only one worker may
own that file at a time; different files can run concurrently. Editors change
assigned Javadocs only, not code or typing annotations. Reviewers inspect the
changed comments and their relevant implementation, not a second exhaustive
audit of every declaration. A specific unresolved behavior claim is recorded
separately; it does not erase completed prose work elsewhere in the file.

Do not run another editing campaign against this checkout simultaneously.
Scheduler leases coordinate cooperating workers, not arbitrary external editors.
If source changes outside the recorded task, stop and reconcile the conflict
without reverting someone else's work.

## Operation

Run from the repository root:

```sh
python3 scripts/docs_prune.py init
python3 scripts/docs_prune.py status
python3 scripts/docs_prune.py run --workers 4 --reviewers 2
python3 scripts/docs_prune.py stop
```

Initialization is idempotent and retains existing baselines and queue results.
`run` resumes pending work; `stop` stops new dispatches and lets current tasks
settle. Use `--max-tasks` for a pilot, `--source` for a relative path or glob
(repeatable), and `--timeout`/`--retries` to bound individual worker failures.
Begin with a limited wave and inspect its results before increasing concurrency.

The runner uses the authenticated local `opencode api` CLI. It does not require
a separate provider key or a raw model API integration. Worker session IDs and
transcripts are saved so failures can be investigated independently of the
overseer's conversation. Offline tests do not consume model quota:

```sh
python3 -m unittest discover -s scripts/tests -v
```

Do not mistake a test run for a live completion check. Before the first live
wave, verify the worker profiles in the repository's explicit OpenCode
location, not the server's default location. Every created worker session must
name the repository location and the requested model. Session permissions are
read-only by default, with an editor granted access only to its assigned file.

Safety checks preserve every byte outside assigned Javadocs, including
Stonecutter directives and inactive branches encoded as comments. A Java-token
hash alone cannot protect those branches. Source changes outside the assignment
must stop that task; source is retained for reconciliation, never auto-restored.

Completion has separate dimensions: prose, independent source review, unresolved
behavior evidence, and automated/output checks. Report all of them. Never describe
the corpus as fully verified just because all editor tasks have finished.

## Generation and automated output checks

Wait for all editing workers to become idle before generating. Generate every
supported Minecraft target without switching the active Stonecutter version:

```sh
./gradlew -PdocsMinecraftVersions=all generateDocsAll
```

Record the source and doclet fingerprints and the successful generation run.
Old artifacts without this provenance are not automatically current. The
`scripts/docs_prune_artifacts.py` helper creates source/rendered-content packets
for offline analysis. It is an extractor, **not a correctness test**.
Missing output, parser errors, ambiguous overloads, and unavailable provenance
remain explicit blockers or review requirements.

The former artifact manager and its database remain as historical evidence.
Preparation can still collect generation provenance and offline packets:

```sh
python3 scripts/docs_prune_validate.py prepare --generate
python3 scripts/docs_prune_validate.py status
```

`prepare --generate` runs the all-target Gradle generation itself. Do not run
the standalone Gradle command concurrently. Without `--generate`, preparation
requires a matching successful generation stamp. Source or artifact changes
invalidate validation; old passed results must not silently apply to a new
generation. Use `--source` and `--target` to restrict offline preparation when
useful. Do not prepare a full model-review queue as a routine post-edit step.

`ARTIFACT_REVIEW_RETIRED` permanently blocks model dispatch in this campaign,
including old runners using `run --resume`. Preserve all old packets, transcripts,
and findings. Retirement is not verification and does not resolve old findings.

Historical artifact assignments bundled selected targets for one source/member
group into one worker assignment. Identical extracted fragments shared a content
entry only when their prose, signatures, typing, and owner metadata matched
exactly. Retain target-specific IDs, paths, hashes, presence checks, and differing
fragments when inspecting this evidence. Bundling did not establish correctness;
the historical queue must not be dispatched again.

Inspect affected Python docstrings and field docs against source. Inspect all
four formats for markup, tags, metadata, and cross-format changes as required
by `AGENTS.md`. Identical extracted fragments can share a prose review across
targets only if each target retains its own presence and provenance accounting.
Do not merge different signatures or typing metadata, mask broken links by
removing URLs, or infer exclusion simply because a page was not found.

Resolve verified findings in shared sources, then regenerate affected targets.
Never fix generated files directly. Parsing success and matching hashes do not
establish source-contract truth or renderer fidelity.
