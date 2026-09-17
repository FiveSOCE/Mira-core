# MiraCore

Shared infrastructure and presentation authority for the Mira Paper server suite.

MiraCore provides common messaging, cooldowns, service discovery, player profiles, notifications, audit history, diagnostics, maintenance controls, rewards, starter guides and shared presentation services so individual Mira plugins do not need to duplicate the same foundation.

## Current Release

**v0.6.0** — compatible with Paper/Minecraft **1.21.11 through 26.2** using Java 21 bytecode.

[View releases](https://github.com/FiveSOCE/Mira-core/releases)

## Requirements

- Paper 1.21.11 through 26.2
- Java 21
- LuckPerms optional/recommended for rank-aware join formatting and managed permission grants
- EssentialsX optional integration for centrally managed command presentation

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

The output JAR is created in `build/libs/`.

---

MiraCore is intended to be the common foundation for the wider **Mira Suite**. Individual Mira modules should consume Core services where appropriate instead of creating competing global systems.