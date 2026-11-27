# MiraCore — Paper 1.18.2 / Java 17

MiraCore provides the Mira ecosystem's shared messaging, services, player profiles, rewards, maintenance, starter guides, repairs, cooldowns, auditing and server presentation.

## Download

**Paper 1.18.2:** [Legacy release page (check for a verified asset)](https://github.com/FiveSOCE/Mira-core/releases/tag/dev-1.18.2)

**Modern Paper 1.21.11–26.2:** [MiraCore_1.21.11-26.2.jar](https://github.com/FiveSOCE/Mira-core/releases/tag/compat-modern-1.21.11-26.2)

Use the version for your server. The legacy source targets Java 17 and Paper API 1.18.2; successful compilation and feature parity in a running server still need verification. Other intermediate Paper versions (1.19–1.21.10) have not been certified.

## Dependencies

- **Required:** Paper 1.18.2 and Java 17.
- **Other Mira plugins:** None required.
- **Optional:** LuckPerms for prefixes/permissions; EssentialsX for configurable message synchronisation and paid repairs using its economy.
- **Install:** Back up `plugins/MiraCore/`; remove any old MiraCore JAR; install **only** `MiraCore_1.18.2.jar` and restart.

## Commands

| Command | Permission | Function |
| --- | --- | --- |
| `/miracore status` | `miracore.admin` | Service and module status |
| `/miracore test` | `miracore.admin` | Diagnostics |
| `/miracore why <player> <permission>` | `miracore.admin` | Permission debugging |
| `/miracore audit [query]` | `miracore.admin` | Audit entries |
| `/miracore profiles` | `miracore.admin` | Player profile info |
| `/miracore maintenance ...` | `miracore.admin` | Maintenance mode |
| `/miracore updates [refresh]` | `miracore.admin` | Read-only update checker |
| `/miracore essentials [sync]` | `miracore.admin` | EssentialsX presentation sync |
| `/miracore reload` | `miracore.admin` | Reload config |
| `/guides` | `miracore.guides` | Guide GUI |
| `/rewards` | `miracore.rewards` | Reward queue |
| `/claim <code>` | `miracore.rewards` | Claim code redemption |
| `/fix hand` | `miracore.fix.hand` | Paid repair |
| `/fix all` | `miracore.fix.all` | Paid inventory repair |

Alias: `/mcore` for `/miracore`.

## Permissions

| Permission | Default | Function |
| --- | --- | --- |
| `miracore.admin` | OP | Administrator commands |
| `miracore.maintenance.bypass` | OP | Rejoin in maintenance |
| `miracore.guides` | All | View guides |
| `miracore.rewards` | All | Rewards and claims |
| `miracore.fix.hand` | OP | Held-item repair |
| `miracore.fix.all` | OP | Inventory repair |
| `miracore.fix.cooldown.bypass` | OP | Ignore repair cooldown |
| `miracore.cooldowns.bypass` | OP | Bypass consumable cooldowns |

## Core Services

MiraCore registers shared services through Bukkit so other Mira plugins can retrieve suite functionality without hidden static coupling.

Current responsibilities include:

- shared `Mira >>` messaging and presentation
- module registration and health state
- shared cooldown service
- persistent player profiles
- audit history and diagnostics
- notifications and pagination helpers
- BossBar presentation
- maintenance scheduling and server-list MOTD authority
- report-only Mira release checking
- persistent reward queue and claim codes
- first-join starter workflow and `/guides`
- server-wide consumable cooldowns
- EssentialsX presentation/message synchronization
- server join-message ownership
- paid `/fix hand` and `/fix all` repair flows

## Join Message Ownership

MiraCore owns the server's public join message.

The default format is:

```text
%rank% %player% Has Joined.
```

`%rank%` uses the player's LuckPerms prefix when available and falls back to the primary group when needed. Legacy Mira MOTD join output is suppressed, and the EssentialsX custom join/new-username join messages are synchronized to empty strings so duplicate global join lines are avoided.

Faction-specific member login notifications are controlled separately by MiraFactions.

## Starter Workflow and Guides

The first-join workflow is configuration driven. Server owners can configure:

- player and console commands on first join
- physical guide books
- `/guides` GUI layout
- book names, metadata and pages

The default Mira setup includes:

- Factions How To
- Custom Enchants Guide
- Events Guide

The configured starting LuckPerms group can also be ensured access to the starter kit and guide library.

## Shared Consumable Cooldowns

MiraCore currently owns configurable native Paper cooldowns for:

- Ender Pearls — default **3 seconds**
- Enchanted Golden Apples — default **5 minutes**

Active cooldowns use Paper's material cooldown overlay and blocked uses show the remaining time on the action bar.

`miracore.cooldowns.bypass` bypasses these controls.

## Repairs — v0.6.0

MiraCore owns the player-facing repair flow:

- `/fix hand` repairs the damaged item in the player's main hand and charges the configured repair cost.
- `/fix all` repairs damaged inventory equipment/items, charges per damaged item and has a default 5-hour cooldown.
- access is permission controlled so MiraItems vouchers or LuckPerms can grant the repair features permanently.

Repair pricing, permissions and cooldown behaviour remain configurable.

## Rewards

### `/rewards`

MiraCore provides a persistent per-player reward queue with:

- paginated reward browsing
- item/command preview
- partial item delivery when only some stacks fit
- overflow retention under the same reward ID
- safe command reward handling
- audit logging

### `/claim <code>`

Global claim codes are configured in `claim-codes.yml`. Codes can require permissions, expire at an absolute timestamp and queue item plus console-command rewards.

## Maintenance and MOTD

MiraCore is the suite authority for normal and maintenance server-list presentation.

Maintenance supports:

- countdown activation
- immediate force activation
- scheduled future windows
- optional automatic end time
- persisted reason/start/end state
- configurable warning thresholds
- bypass-controlled re-entry
- maintenance-specific MOTD lines

When maintenance activates, connected players are deliberately kicked and bypass permission controls who may reconnect afterward.

## EssentialsX Presentation Bridge

MiraCore can own EssentialsX player-facing message text while EssentialsX remains the command/functionality backend.

The bridge:

- imports EssentialsX message keys
- stores the editable MiraCore master properties file
- preserves administrator edits
- adds newly introduced Essentials keys safely
- deploys the generated Essentials locale override
- can enforce one server-wide locale

Useful commands:

```text
/miracore essentials
/miracore essentials sync
```

## Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/miracore status` | `miracore.admin` | Shows registered Mira modules and health. |
| `/miracore test` | `miracore.admin` | Runs diagnostics/self-tests. |
| `/miracore why <player> <permission>` | `miracore.admin` | Explains a player's permission result. |
| `/miracore audit [query]` | `miracore.admin` | Views/searches shared audit history. |
| `/miracore profiles` | `miracore.admin` | Shows shared profile information. |
| `/miracore maintenance ...` | `miracore.admin` | Manages maintenance mode and schedules. |
| `/miracore updates [refresh]` | `miracore.admin` | Checks installed Mira versions against releases. |
| `/miracore essentials [sync]` | `miracore.admin` | Shows/synchronizes the Essentials presentation bridge. |
| `/miracore reload` | `miracore.admin` | Reloads MiraCore configuration. |
| `/guides` | `miracore.guides` | Opens the permanent guide library. |
| `/rewards` | `miracore.rewards` | Opens queued rewards. |
| `/claim <code>` | `miracore.rewards` | Redeems a claim code into the reward queue. |
| `/fix hand` | configured repair permission | Repairs the held item. |
| `/fix all` | configured repair permission | Repairs eligible damaged items with cooldown/cost enforcement. |

Alias: `/mcore`

## Building

```bash
gradle clean build
```

The legacy branch outputs `build/libs/MiraCore-0.6.1-1.18.2.jar`. GitHub Actions publishes this to the `dev-1.18.2` prerelease **only if compilation and verification succeed**. This retains the existing service APIs and feature set rather than substituting a minimal stub.

---

MiraCore is intended to be the common foundation for the wider **Mira Suite**. Individual Mira modules should consume Core services where appropriate instead of creating competing global systems.