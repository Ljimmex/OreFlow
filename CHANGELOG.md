# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.7.0] - 2026-06-25

### Added
- Native Paper Brigadier commands via `LifecycleEvents.COMMANDS` for `/oreflow` and `/cx`.
- Client-side tab-complete and argument validation for all commands.
- Optional `[fortune]` argument for `/oreflow debug [blocks] [fortune]`.
- Back and Exit buttons in the Stone Generator crafting GUI.
- Hidden `Unbreaking X` enchant on the Stone Generator item for visual glow.
- Emerald Ore with hidden enchant glow for the CobbleX button in the main menu.

### Changed
- `/oreflow` command now uses native Brigadier tree structure (no more `CommandExecutor` + `String[] args`).
- `/drop` is now a separate registered command that opens the drop GUI directly.
- Stone Generator crafting GUI layout reworked: result moved one slot right with arrow indicator.
- Stone Generator can now be crafted by clicking the result item in its GUI.
- Stone Generator lore cleaned up — removed duplicate name and improved formatting.
- Action Bar message for placing a generator now matches other action bars (`{prefix} » Stoniarka postawiona!`).
- Creative mode message now only appears when breaking blocks with a pickaxe.
- Main Menu GUI panels are now fully filled in all rows.
- Plugin load phase changed from `STARTUP` to `POSTWORLD` to fix recipe registration timing.

### Fixed
- Stone Generator crafting recipe now works reliably in the crafting table.

---

## [0.6.0] - 2026-06-21

### Added
- Main Menu GUI opened by `/oreflow` with buttons for Drop GUI, Stone Generator, and CobbleX.
- Stone Generator crafting GUI with live recipe preview and one-click crafting.
- Craftable Stone Generator with configurable recipe, effects, and regeneration interval.
- Stone Generator data persistence in `generators-data.yml`.
- `StoneGeneratorManager` and `StoneGeneratorListener`.

### Changed
- `/oreflow` now opens the main menu instead of the drop GUI directly.
- Default generator recipe updated to:
  - `R I R / I S I / R P R` (Redstone, Iron Ingot, Stone, Piston).
- `generators.yml` simplified to a single Stone Generator type.
- Bumped plugin version to `0.6.0`.

### Removed
- Generator owner system.
- Support for multiple generator types (andesite, diorite, etc.).

---

## [0.5.0] - 2026-06-21

### Added
- Full GUI customization via `gui.yml` (title, rows, decorations, drop section, buttons).
- GUI pagination with configurable previous/next page paper buttons.
- `/oreflow admin` admin panel for globally enabling/disabling drops.
- Per-drop Action Bar message toggle (left-click in GUI).
- `MessageManager` for centralized message handling.
- Multilingual support with language files in `lang/` (PL, EN, DE included).
- `/oreflow language <pl|en|de>` command to switch server language.
- `settings.language` option in `config.yml`.
- Configurable Action Bar duration and per-message `enabled` flags.
- Ore blocks now always have vanilla drops disabled.

### Changed
- Action Bar messages now loaded from `lang/<code>.yml` instead of being hardcoded.
- All plugin messages standardized through `MessageManager`.
- `drops.yml` updated with realistic Y-level ranges for all drops.

### Removed
- Single `lang.yml` file replaced by `lang/` folder.
- Dedicated EXP Action Bar message (EXP is still granted).

---

## [0.4.0] - 2026-06-XX

### Added
- Per-player settings system (`PlayerSettingsManager`).
- Players can toggle individual drops, drop destination, and mining EXP.
- `/oreflow toggle <drop> [player]` command.
- Action Bar messages for drops, creative mode, and blocked ores.
- Color-coded material names in Action Bar.
- Custom Fortune level configuration per drop (`fortune.levels.1/2/3`).

### Changed
- Reworked `DropManager` to respect per-player settings.

---

## [0.3.0] - 2026-06-XX

### Added
- Fortune system with tier-based pickaxe requirements.
- `/oreflow cobble` command to toggle cobblestone drop.
- `/oreflow` command opens the drop GUI.
- Permission system in `plugin.yml`.
- Tool tier support (`WOODEN` → `NETHERITE`).

### Changed
- BlockBreakListener now respects Silk Touch and tool tiers.

---

## [0.2.0] - 2026-06-XX

### Added
- `BlockBreakListener` to handle block break events.
- `DropManager` for chance-based drops.
- World blacklist/whitelist support.
- Configurable mineable blocks list.
- Y-level and required-tool filters.
- EXP rewards per drop.
- CobbleX system (`/cx` commands and recipes).

---

## [0.1.0] - 2026-06-XX

### Added
- Initial plugin setup with Gradle + Kotlin DSL.
- Paper API 1.21.1-R0.1-SNAPSHOT support.
- Java 21 toolchain.
- `ConfigManager` for multi-file YAML handling.
- Default config generation: `config.yml`, `drops.yml`, `generators.yml`, `lang.yml`.
- `run-paper` plugin for local test server.

---

## Upcoming

### [1.0.0] - Target Release
- Stone Generator Manager & Recipes (Sprint 8).
- Generator placement, ownership, and protection.
- Performance testing and optimization.
- Full documentation and SpigotMC release.
