<!--
  - SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  - SPDX-License-Identifier: GPL-3.0-or-later
-->
# AGENTS.md

This file provides guidance to all AI agents (Claude, Codex, Gemini, etc.) working with code in this repository.

## Nextcloud Contribution Policy

> **Fork amendment (Krateos-BV).** This file is inherited from upstream
> `nextcloud/talk-android`. In this fork, work is reviewed on the pull request
> itself rather than before it is opened, so the agent opens its own PRs and
> writes their descriptions (see "What this agent may do in this fork" below).
> This fork also does not require a DCO sign-off (see "Developer Certificate of
> Origin" below); every other rule in this policy stands unchanged.
> **This amendment applies only to pull requests targeting
> branches of `Krateos-BV/talk-android`.** Anything destined for an upstream
> `nextcloud/*` repository follows the unmodified upstream policy, where a
> human opens the PR and writes it in their own words.

All contributions generated or assisted by this agent must fully comply with:

- **[AI Contribution Policy](https://github.com/nextcloud/.github/blob/master/AI_POLICY.md)** — the primary reference for AI-specific rules, covering disclosure, author accountability, communication, security, licensing, code quality, and autonomous agent behavior.
- **[Contribution Guidelines](https://github.com/nextcloud/.github/blob/master/CONTRIBUTING.md)** — covering testing requirements, the Developer Certificate of Origin (DCO), license headers, conventional commits, and translations. These apply in full to all contributions regardless of how they were produced.

### Developer Certificate of Origin (DCO)

Upstream `nextcloud/*` uses the DCO so that outside contributors certify they
have the legal right to submit the code they send. That requirement does not
carry over to this fork: `Krateos-BV/talk-android` does not accept outside
contributions, and its sole maintainer is the same person directing the agent —
the agent is that person's tool, not a separate legal contributor. There is no
third party here whose right to submit needs certifying.

So in this fork:

- Do not use `git commit -s`, and do not add a `Signed-off-by` trailer to
  commits or to PR descriptions.
- Do not add sign-offs retroactively to commits that already exist. A DCO
  certification is meant to be made by the contributor at the time of the
  commit; adding one after the fact would be ceremony, not certification.
- Contributions still carry the `Assisted-by:` trailer and the AI disclosure
  required above — those record how the code was produced, which the DCO
  never did.

Anything destined for an upstream `nextcloud/*` repository still follows
upstream's rule, where the **human** contributor signs off in their own name.
The agent never writes a `Signed-off-by` line on anyone's behalf, in either
repository.

### What this agent must always do

- Add an `Assisted-by: AGENT_NAME:MODEL_VERSION` git trailer to every commit containing AI-assisted content.
- Ensure every pull request includes a disclosure of AI tool use in the PR description.
- Produce focused, scoped pull requests that address exactly one concern. Do not touch unrelated files or introduce incidental refactors.
- Verify all dependencies against actual package registries before suggesting them. Do not use hallucinated or unverified package names.
- Write code comments that document the code, never the process that produced it:
  - Comments describe what the code does — method signatures, behavior, and constraints the code itself cannot express (e.g. a non-obvious invariant or workaround).
  - Never add comments that document progress, decisions, or changes (e.g. "changed X to Y", "as requested", "this fixes ...", "previously this did ..."). That belongs in the commit message or PR discussion; in the code it goes stale and becomes misleading.
  - Do not narrate self-explanatory code. If the code is readable without a comment, omit the comment.
  - Keep comments brief — short and simple, matching the comment density of the surrounding code.
- Reuse existing helper functions and utilities instead of re-implementing their logic inline. When fixing a flawed pattern, fix every occurrence of it across the changed code, not only the instance that was pointed out.
- Run permission and access-control checks before the operation they guard, never after it and never only in the UI layer.
- When adding or changing user-facing functionality, wire it up in every context where the affected component is used — the default authenticated view, public share pages, and embedded contexts such as the Smart Picker and reference widgets. When emitting new events, verify that every consumer of the component subscribes to and handles them.
- Explicitly inform the contributor when any action they are about to take, or have taken, would violate the AI Contribution Policy or the Contribution Guidelines. Do not silently proceed. State which rule is at risk and what the contributor should do instead.
- Warn the contributor if a pull request is growing too large. A PR approaching several thousand lines of changed code is a signal that it should be split into smaller, focused PRs. Suggest a logical split before the PR is opened, not after.
- Recommend opening a ticket for discussion before starting implementation whenever a feature or change is sufficiently complex — for example when it touches multiple subsystems, requires architectural decisions, or the right approach is not yet clear. A ticket allows maintainers and the contributor to align on direction before code is written, avoiding wasted effort on a PR that may be rejected or require fundamental rework.

### What this agent must never do

- Send security reports autonomously, or submit anything to an upstream `nextcloud/*` repository without a human opening it. (Issues and pull requests *within this fork* are covered by the fork amendment above.)
- Add `Signed-off-by` tags to commits (see "Developer Certificate of Origin" above).
- Generate or submit security reports without independent human verification. Report verified vulnerabilities via [HackerOne](https://hackerone.com/nextcloud), not as GitHub issues.
- Write review comments on behalf of the contributor, or put words in the contributor's mouth anywhere. Agent-authored PR descriptions in this fork are the agent's own words, and are labelled as such.
- Fully automate the resolution of issues labeled [`good first issue`](https://github.com/issues?q=org%3Anextcloud+label%3A%22good+first+issue%22) or similar beginner-friendly labels.
- Submit code that has not been reviewed and cleaned up by the contributor. Dead code, redundant logic, excessive comments, malformed or garbled characters (e.g. `�` replacement characters), and unrelated changes must be removed before submission.

### What this agent may do in this fork

- Open issues and pull requests against `Krateos-BV/talk-android` without
  waiting for a human to do it, and write the PR description itself. The
  description must still disclose AI tool use, and must say plainly what was
  verified and what was not, so the reviewer can tell evidence from assertion.
- Commit without a DCO sign-off. This fork does not use the Developer
  Certificate of Origin, so neither the agent nor the contributor adds a
  `Signed-off-by` trailer here.

---

## Project Overview

Nextcloud Talk for Android — a self-hosted audio/video and chat communication app. Connects to a Nextcloud server backend. Written primarily in Kotlin (some legacy Java), targets API 26+ (minSdk 26, targetSdk 36).

## Build Commands

```bash
# Assemble a debug APK (F-Droid flavor, no Google services)
./gradlew assembleGenericDebug

# Assemble with Google Play services (push notifications)
./gradlew assembleGplayDebug

# Run all unit tests
./gradlew test

# Run a single unit test class
./gradlew testGenericDebugUnitTest --tests "com.nextcloud.talk.utils.SomeTest"

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Static analysis — all checks (spotbugs, lint, ktlint, detekt)
./gradlew check

# Individual checks
./gradlew ktlintCheck
./gradlew ktlintFormat   # auto-fix
./gradlew detekt
./gradlew lint

# Install git hooks (run once after cloning)
./gradlew installGitHooks

# Clean build
./gradlew clean assembleGenericDebug
```

Build output: `app/build/outputs/apk/`

## Build Flavors

| Flavor    | App ID                  | Purpose                            |
|-----------|-------------------------|------------------------------------|
| `generic` | `eu.xeniacloud.talk`    | Build without Google services      |
| `gplay`   | `eu.xeniacloud.talk`    | Google Play (Firebase push notifs) |
| `qa`      | `eu.xeniacloud.talk.qa` | Per-PR testing builds              |

`gplay`-only dependencies (Firebase, play-services-base) use `gplayImplementation`. Avoid introducing Play-only dependencies into `generic` code paths. `generic` builds do not support Google push notifications.

## Architecture

MVVM with layered architecture:

- **API layer** — `api/NcApi.java` (Retrofit/RxJava2) and `api/NcApiCoroutines.kt` (Retrofit/coroutines).
- **Data layer** — `data/` contains Room DB entities/DAOs (`data/database/`), repository impls (`data/user/`, `repositories/`), and a network monitor. The Room DB is encrypted with SQLCipher.
- **Repository layer** — `repositories/` and `data/user/UsersRepository.kt` are the single source of truth.
- **ViewModel layer** — expose `StateFlow`/`LiveData` to UI. Located in per-feature `viewmodels/` subdirectories.
- **UI layer** — Activities/Fragments per feature. Mix of traditional View/XML and Jetpack Compose (composables live alongside XML layouts in feature packages).

### Dependency Injection

Dagger 2 (via AutoDagger2). App component: `application/NextcloudTalkApplication.kt` (`@AutoComponent`). Modules in `dagger/modules/`: `RestModule`, `DatabaseModule`, `DaosModule`, `RepositoryModule`, `ViewModelModule`, `ManagerModule`, `UtilsModule`.

Use `@Inject` for Activities/Fragments/Services/BroadcastReceivers. For all other components, prefer constructor injection.

### Feature Packages (under `com/nextcloud/talk/`)

- `conversationlist/` — main screen after login (actively being Compose-migrated, see below)
- `chat/` — `ChatActivity`, message input, voice recording, scheduled messages
- `call/` — WebRTC participant modeling, MCU/non-MCU strategies
- `webrtc/` — low-level WebRTC: `PeerConnectionWrapper`, `WebSocketInstance`, audio
- `signaling/` — `SignalingMessageReceiver`, `SignalingMessageSender`, typed notifiers
- `conversationinfo/` / `conversationinfoedit/` — room settings
- `account/` — login, account verification
- `settings/` — app settings
- `jobs/` — WorkManager background workers
- `services/` — `CallForegroundService`
- `ui/theme/` — Nextcloud theming applied to Material components

### Signaling Architecture

Two modes selected at runtime based on server capabilities:
- **No-MCU** (P2P mesh): `call/LocalStateBroadcasterNoMcu.kt`, `call/MessageSenderNoMcu.kt`
- **MCU** (media server): `call/LocalStateBroadcasterMcu.java`, `call/MessageSenderMcu.java`

`signaling/SignalingMessageReceiver.java` dispatches to typed notifiers (`CallParticipantMessageNotifier`, `WebRtcMessageNotifier`, etc.).

**When changing participant or call state handling, always verify both MCU and no-MCU paths — a change that works in one mode can silently break the other.**

## Active Work: Compose Migration of `conversationlist/`

`ConversationsListActivity` is being incrementally migrated to Jetpack Compose. The plan is in `docs/compose-migration-conversations-list.md`. Steps 1–7 are complete (ViewModel state consolidation, status banners, empty states, FAB, notification warning card, federation invitation card, shimmer loading, conversation item composable). Steps 8–10 (LazyColumn list, toolbar/search bar, full Activity handover) are pending.

**Convention:** During the migration each component is replaced one at a time so the app remains fully functional after every step. New composables go in `conversationlist/ui/`. The existing `FlexibleAdapter`/`RecyclerView` is kept until Step 8.

## Code Style

- Line length: **120 characters**
- Standard Android Studio formatter with EditorConfig.
- Kotlin preferred for new code; legacy Java still present.
- Do not use decorative section-divider comments of any kind (e.g. `// ── Title ───`, `// ------`, `// ======`).
- Every new file must end with exactly one empty trailing line (no more, no less).
- All new files require an SPDX license header:

  Kotlin/Java:
  ```kotlin
  /*
   * Nextcloud Talk - Android Client
   *
   * SPDX-FileCopyrightText: <year> Nextcloud GmbH and Nextcloud contributors
   * SPDX-License-Identifier: GPL-3.0-or-later
   */
  ```

  XML:
  ```xml
  <!--
    ~ Nextcloud Talk - Android Client
    ~
    ~ SPDX-FileCopyrightText: <year> Nextcloud GmbH and Nextcloud contributors
    ~ SPDX-License-Identifier: GPL-3.0-or-later
  -->
  ```

- Translations via Transifex — only modify `values/strings.xml`, never translated `values-*/strings.xml` files.

## File Naming

Layout/menu files follow the component they belong to:

| Component        | Class Name             | File Name                       |
|------------------|------------------------|---------------------------------|
| Activity         | `UserProfileActivity`  | `activity_user_profile.xml`     |
| Fragment         | `SignUpFragment`       | `fragment_sign_up.xml`          |
| Dialog           | `ChangePasswordDialog` | `dialog_change_password.xml`    |
| AdapterView item | —                      | `item_person.xml`               |
| Partial layout   | —                      | `partial_stats_bar.xml`         |

## Design

- Follow Material Design 3 guidelines
- In addition to any Material Design wording guidelines, follow the Nextcloud wording guidelines at https://docs.nextcloud.com/server/latest/developer_manual/design/foundations.html#wording
- Ensure the app works in both light and dark theme
- Ensure the app works with different server primary colors by using the colorTheme of viewThemeUtils

## After Making Changes

After finishing code changes, run `./gradlew detekt ktlintCheck` and fix any new errors or warnings before considering the task done.

## Static Analysis

- **detekt**: config in `detekt.yml` (maxIssues: 80)
- **ktlint**: via `org.jlleitschuh.gradle.ktlint` plugin
- **SpotBugs**: filter in `spotbugs-filter.xml`; FindSecBugs and fb-contrib active
- **lint**: HTML report at `app/build/reports/lint/lint.html`

## Testing

- **Unit tests**: `app/src/test/` — JUnit 4/5, Mockito, Robolectric, MockWebServer. Uses `useJUnitPlatform()`.
- **Instrumented tests**: `app/src/androidTest/` — Espresso. Integration tests need real server credentials in `gradle.properties` (`NC_TEST_SERVER_BASEURL`, etc.).
- **Room migrations**: if you change the schema, add or update migration tests under `androidTest/data/`. See `data/source/local/TalkDatabase.kt` for migration declarations.
- **App startup workers**: `NextcloudTalkApplication.kt` schedules periodic workers (`CapabilitiesWorker`, signaling/WebSocket workers) at startup. Worker scheduling changes can cause subtle startup regressions.

## Commits

- PRs in this fork target `main`, the default branch of `Krateos-BV/talk-android`. Upstream `nextcloud/talk-android` is on `master`, where backports use `/backport to stable-X.Y` in a PR comment.

- Commits are not signed off in this fork — do not use `git commit -s` or add a `Signed-off-by` trailer (see "Developer Certificate of Origin" above).

- Commit messages must follow the [Conventional Commits v1.0.0 specification](https://www.conventionalcommits.org/en/v1.0.0/#specification) — e.g. `feat(chat): add voice message playback`, `fix(call): handle MCU disconnect gracefully`.

- Every commit made with AI assistance must include an `Assisted-by` trailer identifying the coding agent and model:

  ```
  Assisted-by: Claude Code:claude-sonnet-4-6
  Assisted-by: Copilot:claude-sonnet-4-6
  ```

  General pattern: `Assisted-by: <coding-agent>:<model-version>`
