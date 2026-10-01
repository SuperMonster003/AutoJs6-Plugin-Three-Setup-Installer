# P2 integration handoff

This earlier partial draft is superseded by `p2-session-evidence.md`, `p2-binder-evidence.md` and
`p2-core-evidence.md`. Its previously uncommitted implementation is now included in the logical P2 commits.
The latest user request authorized implementation; the remaining-work ownership note below is historical.

Written on 2026-10-01 after the install engine review closed. It lists what the uncommitted P2
sources already provide, what the remaining P2 pieces must satisfy, and the checks that close the
milestone. It is a prose guide only; the remaining pieces are written by the maintainer, and the
assistant reviews them.

## What exists

| File | Provides |
| --- | --- |
| `auth/Authorizer.kt` | The `none` / `shizuku` / `root` enum with `privileged`, `DEFAULT_ORDER` (shizuku, root, none), `fromId`, `isAuto`; `AuthorizerState` (`available`, `running`, `granted`, `reason`, `usable`) |
| `auth/AuthorizerResolver.kt` | `resolve(requested, states, order, enabled)`: explicit choice never falls back (`AUTHORIZER_UNAVAILABLE` / `AUTHORIZER_DENIED` / `INVALID_ARGUMENT`), `auto` takes the first usable enabled authorizer and ends at `none` without prompting |
| `auth/AuthorizerStates.kt` | `states(context)`, `state(context, authorizer)` (cheap, never prompts), `request(context, authorizer, timeoutMillis)` (Shizuku permission dialog or root prompt, blocks the worker) |
| `auth/PrivilegedClient.kt` | Process-wide `get(context)`; `acquire(authorizer, timeoutMillis)` binds the UserService or RootService on the main thread and returns the live privileged Binder; current binding behavior is documented in `p2-authorizer-evidence.md` |
