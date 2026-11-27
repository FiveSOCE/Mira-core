# MiraCore

MiraCore is the shared infrastructure and server-presentation foundation for the **Mira Minecraft plugin ecosystem**. It provides messaging, registered service APIs, player profiles, cooldowns, audit logs, diagnostics, maintenance, rewards, starter kits/guides, join-message ownership, paid repairs and an optional EssentialsX presentation bridge.

## Download the correct server version

| Server target | Java | JAR | Build |
| --- | --- | --- | --- |
| **Paper 1.18.2** | Java 17 | `MiraCore_1.18.2.jar` | [Legacy downloads](https://github.com/FiveSOCE/Mira-core/releases/tag/dev-1.18.2) |
| **Paper 1.21.11–26.2** | Java 21 baseline; server may require Java 25 | `MiraCore_1.21.11-26.2.jar` | [Modern downloads](https://github.com/FiveSOCE/Mira-core/releases/tag/compat-modern-1.21.11-26.2) |

**Important:** The modern binary currently targets Paper 1.21.11 as its lowest compiled API, with a 26.2 compile check. **Paper 1.19–1.21.10 are not yet certified.** The legacy build is compiled for Java 17 / Paper 1.18.2. Compilation is not equivalent to full runtime feature-parity testing.

Install **one** MiraCore JAR in `plugins/`, not both. Remove older MiraCore JARs before restarting. Back up `plugins/MiraCore/` before changing versions.

[Legacy source](https://github.com/FiveSOCE/Mira-core/tree/compat/1.18.2) · [Modern source](https://github.com/FiveSOCE/Mira-core/tree/main) · [All releases](https://github.com/FiveSOCE/Mira-core/releases)

## Dependencies and integrations

- **Required:** Paper matching the chosen version; correct Java runtime.
- **Required Mira plugins:** None. MiraCore is the shared foundation; it does not require another Mira plugin.
- **Optional:** LuckPerms for join prefixes and starter-group permission provisioning.
- **Optional:** EssentialsX for locale/message synchronisation and the paid repair economy. Repairs needing economy payment require a functioning EssentialsX economy.
- **Other Mira plugins:** Many declare `depend: [MiraCore]` and will not load if MiraCore does not load first.

## Commands and permissions

| Command | Permission | Function |
| --- | --- | --- |
| `/miracore status` (alias `/mcore`) | `miracore.admin` | Suite module status |
| `/miracore test` | `miracore.admin` | Diagnostic checks |
| `/miracore why <player> <permission>` | `miracore.admin` | Permission investigation |
| `/miracore audit [query]` | `miracore.admin` | Audit history |
| `/miracore profiles` | `miracore.admin` | Player profile information |
| `/miracore maintenance ...` | `miracore.admin` | Schedule or manage maintenance |
| `/miracore updates [refresh]` | `miracore.admin` | Report-only release checker |
| `/miracore essentials [sync]` | `miracore.admin` | EssentialsX presentation settings |
| `/miracore reload` | `miracore.admin` | Reload configuration |
| `/guides` | `miracore.guides` | Open configurable guide library |
| `/rewards` | `miracore.rewards` | Claim queued rewards |
| `/claim <code>` | `miracore.rewards` | Redeem claim codes |
| `/fix hand` | `miracore.fix.hand` | Paid held-item repair |
| `/fix all` | `miracore.fix.all` | Paid inventory repairs and cooldown |

Other permission nodes:

| Node | Default | Meaning |
| --- | --- | --- |
| `miracore.admin` | OP | Administrative suite commands |
| `miracore.maintenance.bypass` | OP | Join during maintenance |
| `miracore.rewards` | Everyone | Rewards and claim codes |
| `miracore.guides` | Everyone | Guides |
| `miracore.fix.hand` | OP | Allow `/fix hand` |
| `miracore.fix.all` | OP | Allow `/fix all` |
| `miracore.fix.cooldown.bypass` | OP | Ignore the `/fix all` cooldown |
| `miracore.cooldowns.bypass` | OP | Bypass pearl/gapple cooldown restrictions |

Configurable integrations and cooldown bypass permissions may add additional access rules. Review `config.yml` for server-specific changes.

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