# Mira ecosystem: Paper 1.18.2 validation register

This register separates **migration configuration**, **verified build output**, and **in-server functional testing**. An existing branch or workflow is not evidence that a JAR is compatible. The modern 1.21.11–26.2 branches are retained separately.

## Acceptance gates

1. Java 17 build and tests pass: `gradle clean test build` against `1.18.2-R0.1-SNAPSHOT`.
2. The generated versioned JAR contains a valid `plugin.yml` with `api-version: '1.18'` and correct plugin main class.
3. All hard/soft dependency JARs resolve to compatible Java 17 and Paper 1.18.2 builds; avoid compiling against modern Java 21 plugin JARs.
4. The JAR loads on an isolated Paper 1.18.2 server with Java 17 without `UnsupportedClassVersionError`, `NoSuchMethodError`, `NoClassDefFoundError`, or plugin disable errors.
5. Permissions, commands, GUIs, data persistence, and the plugin's primary gameplay/economy scenarios pass.
6. Only after gates 1–5 pass, publish release JAR and update README with a verified download link.

## Compatibility table

| Repository | Source migration | Successful build / artifact | Runtime smoke test |
| --- | --- | --- | --- |
| [Mira-core](https://github.com/FiveSOCE/Mira-core/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Enchants](https://github.com/FiveSOCE/Mira-Enchants/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Spawners](https://github.com/FiveSOCE/Mira-Spawners/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Crates](https://github.com/FiveSOCE/Mira-Crates/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Combat](https://github.com/FiveSOCE/Mira-Combat/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Tags](https://github.com/FiveSOCE/Mira-Tags/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Tab](https://github.com/FiveSOCE/Mira-Tab/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Items](https://github.com/FiveSOCE/Mira-Items/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Kits](https://github.com/FiveSOCE/Mira-Kits/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Redeem](https://github.com/FiveSOCE/Mira-Redeem/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Fly](https://github.com/FiveSOCE/Mira-Fly/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-AuctionHouse](https://github.com/FiveSOCE/Mira-AuctionHouse/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-NPC](https://github.com/FiveSOCE/Mira-NPC/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-shop](https://github.com/FiveSOCE/Mira-shop/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Warps](https://github.com/FiveSOCE/Mira-Warps/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Homes](https://github.com/FiveSOCE/Mira-Homes/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Withdraw](https://github.com/FiveSOCE/Mira-Withdraw/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Pinata](https://github.com/FiveSOCE/Mira-Pinata/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Factions](https://github.com/FiveSOCE/Mira-Factions/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Bounties](https://github.com/FiveSOCE/Mira-Bounties/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Events](https://github.com/FiveSOCE/Mira-Events/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Boosters](https://github.com/FiveSOCE/Mira-Boosters/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Seasons](https://github.com/FiveSOCE/Mira-Seasons/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Leaderboards](https://github.com/FiveSOCE/Mira-Leaderboards/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Playtime](https://github.com/FiveSOCE/Mira-Playtime/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Daily](https://github.com/FiveSOCE/Mira-Daily/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Punishments](https://github.com/FiveSOCE/Mira-Punishments/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Staff](https://github.com/FiveSOCE/Mira-Staff/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Reports](https://github.com/FiveSOCE/Mira-Reports/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Cosmetics](https://github.com/FiveSOCE/Mira-Cosmetics/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Backpacks](https://github.com/FiveSOCE/Mira-Backpacks/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Sellwands](https://github.com/FiveSOCE/Mira-Sellwands/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Collectors](https://github.com/FiveSOCE/Mira-Collectors/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Outposts](https://github.com/FiveSOCE/Mira-Outposts/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-RTP](https://github.com/FiveSOCE/Mira-RTP/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Combat-tag](https://github.com/FiveSOCE/Mira-Combat-tag/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Trades](https://github.com/FiveSOCE/Mira-Trades/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Airdrops](https://github.com/FiveSOCE/Mira-Airdrops/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Chatitems](https://github.com/FiveSOCE/Mira-Chatitems/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Duels](https://github.com/FiveSOCE/Mira-Duels/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Jellylegs](https://github.com/FiveSOCE/Mira-Jellylegs/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Chats](https://github.com/FiveSOCE/Mira-Chats/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Gamba](https://github.com/FiveSOCE/Mira-Gamba/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Rename](https://github.com/FiveSOCE/Mira-Rename/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Loaders](https://github.com/FiveSOCE/Mira-Loaders/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-Coinflip](https://github.com/FiveSOCE/Mira-Coinflip/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |
| [Mira-FP](https://github.com/FiveSOCE/Mira-FP/tree/compat/1.18.2) | CI configured | Not yet verified | Not yet tested |

No successful build or in-server test should be recorded here without checking the actual CI result or server logs.

## Recommended dependency-ordered test batches

- **Foundation:** Mira-core, Mira-NPC, Mira-Redeem, Mira-Warps, Mira-Homes, Mira-RTP.
- **MiraCore consumers:** Mira-Spawners, Mira-Items, Mira-Tags, Mira-Combat, Mira-Kits, Mira-Events, Mira-Seasons, Mira-Boosters.
- **Interdependent:** Mira-Factions, Mira-Shop, Mira-Crates, Mira-Fly, Mira-Leaderboards, Mira-Playtime, Mira-Bounties.
- **Remaining standalone/integrations:** test the other modules separately before full-suite testing.

**Known dependency risk:** Several older 1.18.2 branches refer to `MiraCore_1.18.2.jar` but newer modules still download version-pinned *modern* MiraCore or MiraSeasons artifacts. Those need to be replaced by compatible legacy dependencies or rebuilt from version-matched source before marking a dependent plugin green.

## Manual smoke-test procedure

1. Start with a clean Paper 1.18.2 instance on Java 17 and save its startup log.
2. Install exactly one plugin and its verified required dependencies; restart, rather than `/reload`.
3. Check `/plugins`, console warnings, registered commands and permissions.
4. Exercise normal player, administrator, persistence, permission denial, and recovery paths.
5. Shut down and restart to confirm persistent state loads.
6. Install related modules in the dependency order above and repeat cross-plugin tests.
7. Attach CI run links and test evidence before editing a row to **PASS**.

## Not part of the Java plugin count

- `MIra-Resourcepack` is a resource-pack project requiring a separate Minecraft 1.18.2 format/model conversion.
- `Mira-Main` currently describes a server JAR project rather than a conventional Paper plugin.
