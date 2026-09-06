## v0.5.1 simplified EssentialsX responses

- Simplifies `/help` headers, command help, usage lines, cooldowns, disabled-command notices, errors and common confirmations.
- Preserves dynamic values such as player names, balances, destinations and cooldown durations.
- Automatically migrates untouched MiraCore-generated Essentials defaults to the new compact wording.
- Preserves any Essentials messages you manually edited in the MiraCore-managed properties file.

## v0.5.1 EssentialsX presentation ownership

MiraCore can now own EssentialsX player-facing command text while EssentialsX remains the command and functionality backend.

- Imports EssentialsX's current bundled `messages.properties`.
- Creates the editable MiraCore master file `plugins/MiraCore/essentials-messages_<locale>.properties`.
- Automatically adds newly introduced EssentialsX message keys on future upgrades without overwriting existing MiraCore edits.
- Applies the configured Mira prefix and primary/secondary style when a message key is first imported.
- Deploys the generated override to `plugins/Essentials/messages/messages_<locale>.properties`.
- Can enforce a single EssentialsX locale and disable per-player locale for consistent server-wide presentation.
- `/miracore essentials` shows bridge status.
- `/miracore essentials sync` republishes the MiraCore message file and reloads EssentialsX.
- MiraCore does not replace EssentialsX command executors, permissions, cooldowns, teleport logic, economy logic or other command behavior.

# MiraCore

MiraCore is the shared infrastructure and API layer for the Mira Paper server suite. It provides common messaging, cooldowns, service discovery, player profiles, notifications, audit logging, diagnostics and module health so individual Mira plugins do not duplicate the same foundation.

## Download

[**Download MiraCore v0.5.1**](https://github.com/FiveSOCE/Mira-core/releases/download/v0.5.1/MiraCore-0.5.1.jar)

[View All Releases](https://github.com/FiveSOCE/Mira-core/releases)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- No third-party dependencies

## How MiraCore Works

MiraCore registers shared services through Bukkit so other Mira plugins can retrieve them with `MiraCoreProvider.require()` instead of relying on hidden static state. Core currently provides a service registry, cooldown service, the suite-wide message/prefix service, module registration and health state, persistent player profiles, notifications, audit history, pagination helpers, permission diagnostics and persistent milestone/achievement support, shared BossBar presentation, maintenance authority, safe report-only release checking, and the persistent suite-wide reward queue/claim-code service.

The module registry lets plugins report whether they are healthy or degraded, while the audit and diagnostics commands give administrators one place to inspect suite behaviour. MiraCore is also the source of the shared `&5&lMira &8>> &r` chat prefix for plugins that use Core messaging.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/miracore status` | `miracore.admin` | Shows registered Mira modules and their current health/status. |
| `/miracore test` | `miracore.admin` | Runs MiraCore diagnostics/self-tests. |
| `/miracore why <player> <permission>` | `miracore.admin` | Debugs whether a player has a permission and why. |
| `/miracore audit [query]` | `miracore.admin` | Views/searches shared Mira audit history. |
| `/miracore profiles` | `miracore.admin` | Shows shared player-profile information/statistics. |
| `/miracore maintenance status` | `miracore.admin` | Shows maintenance state and schedules. |
| `/miracore maintenance on [reason...]` | `miracore.admin` | Starts the configured maintenance countdown, then activates maintenance and kicks every connected player. |
| `/miracore maintenance force [reason...]` | `miracore.admin` | Activates maintenance immediately and kicks every connected player. |
| `/miracore maintenance off` | `miracore.admin` | Ends maintenance and reopens normal joining. |
| `/miracore maintenance schedule <delay> [duration] [reason...]` | `miracore.admin` | Schedules a future maintenance window with countdown warnings, optional auto-reopen duration and a persisted reason. |
| `/miracore maintenance cancel` | `miracore.admin` | Clears the scheduled maintenance window. |
| `/miracore updates [refresh]` | `miracore.admin` | Compares installed Mira module versions with verified GitHub Releases. Never auto-downloads. |
| `/miracore reload` | `miracore.admin` | Reloads MiraCore configuration. |
| `/miracore help` | `miracore.admin` | Shows MiraCore command help. |
| `/rewards` | `miracore.rewards` | Opens the player's paginated queued-reward browser and claim preview. |
| `/claim <code>` | `miracore.rewards` | Redeems an active global claim code into the persistent reward queue. |

Alias: `/mcore`

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miracore.admin` | OP | Allows MiraCore administration, audits, diagnostics and permission debugging. |
| `miracore.rewards` | Everyone | Allows viewing queued rewards and redeeming configured claim codes. |

## Shared Presentation and Operations (0.3.0)

MiraCore now exposes `BossBarService` as the suite-wide boss-bar presentation authority. Other Mira modules can create/update a player-scoped named bar without each plugin maintaining its own unrelated boss-bar implementation.

`MaintenanceService` owns persistent maintenance state, reasons and scheduled activation. When maintenance activates, every connected player is kicked; the bypass permission controls who may rejoin afterward. MiraCore also owns the normal server-list MOTD and maintenance MOTD override.

`UpdateService` compares registered Mira module versions against configured GitHub repositories asynchronously. It is intentionally report-only: MiraCore does not download, replace or hot-swap plugin JARs.

## Shared Reward Platform (0.4.0)

MiraCore now owns the reusable `RewardService` so other Mira plugins can queue rewards without each module building its own delivery store.

### `/rewards`

- persistent per-player reward queue
- paginated reward list
- full item/command preview before claiming
- partial item delivery when only some stacks fit
- overflow remains queued under the same reward ID
- command rewards remain queued if dispatch fails
- claim actions are recorded in the Core audit log

### `/claim <code>`

Global claim codes are configured in `claim-codes.yml`. Codes are case-insensitive, can require a permission and/or expiry timestamp, and may queue item plus console-command rewards.

A code is recorded as used once its reward has been safely queued. This prevents repeated redemption while still allowing inventory overflow to be claimed later through `/rewards`.

## Native MOTD and Maintenance Workflow (0.4.1)

MiraCore now replaces the old standalone MOTD plugin as the authoritative server-list presentation layer.

### Remove the old MOTD JAR

After installing MiraCore v0.5.1, remove the old plugin JAR whose Bukkit plugin name is `MOTD` (for example the previous `Valk MOTD.jar`). Running both is unnecessary and can create competing `ServerListPingEvent` writers. MiraCore logs a warning if it detects that legacy plugin still installed.

### Normal MOTD

`config.yml` now supports:

- two normal server-list MOTD lines
- two maintenance-specific MOTD lines
- optional join MOTD messages
- optional displayed maximum-player override
- optional cached `server-icon.png` loaded from the MiraCore data folder

Maintenance MOTD lines support `%reason%` and `%end%` placeholders.

### Maintenance activation

`/miracore maintenance on [reason...]` starts the configured `maintenance.activation-countdown-seconds` countdown. The default is 30 seconds.

Configured warning thresholds are broadcast while the countdown runs. Defaults are:

`1800, 600, 300, 60, 30, 10, 5, 4, 3, 2, 1` seconds.

When the countdown reaches zero:

1. MiraCore persists maintenance as active.
2. the server-list MOTD switches to the maintenance presentation.
3. **every currently connected player is kicked, including staff/bypass users**.
4. subsequent login attempts are denied unless the player has `miracore.maintenance.bypass`.

This means bypass is deliberately a **re-entry permission**, not protection from the activation kick.

### Scheduling

Examples:

```text
/miracore maintenance on Plugin updates
/miracore maintenance schedule 10m Plugin updates
/miracore maintenance schedule 30m 2h Database maintenance
/miracore maintenance force Emergency maintenance
/miracore maintenance off
```

Maintenance reason, scheduled start and scheduled end are persisted in `maintenance.yml`. Admin-facing scheduled times are displayed in `Australia/Brisbane` time.

Expired maintenance windows are discarded safely rather than briefly activating/kicking players during a late reload.
