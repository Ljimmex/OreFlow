# OreFlow

[![Paper](https://img.shields.io/badge/Paper-1.21.x-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-9.6-02303A.svg)](https://gradle.org/)
[![Version](https://img.shields.io/badge/Version-0.5.0-green.svg)](CHANGELOG.md)

> Advanced, customizable drop system for Paper 1.21.x servers.

OreFlow replaces vanilla drops from stone-like blocks with a fully configurable drop table, complete with a modern GUI, per-player settings, Action Bar messages, multilingual support, and an admin panel.

---

## Features

- **Custom Drop System** — define drops from stone, andesite, diorite, granite, calcite, tuff, deepslate, and more.
- **Per-Player Settings** — players can toggle drops, cobblestone, drop destination (inventory/ground), and mining EXP.
- **Modern GUI** — fully customizable via `gui.yml`: title, rows, decorations, button slots, and placeholders.
- **Pagination** — automatically paginates when you have more drops than configured slots.
- **Action Bar Messages** — clean Adventure/MiniMessage-based drop notifications.
- **Multilingual Support** — language files in `lang/` (PL, EN, DE included) with live switching via `/oreflow language`.
- **Admin Panel** — `/oreflow admin` lets admins globally enable/disable drops.
- **Fortune Integration** — configurable Fortune I/II/III bonus chances and amounts per drop.
- **Tool Tiers** — enforce minimum pickaxe tiers for drops.
- **Y-Level Restrictions** — limit drops to specific world heights.
- **Silk Touch Support** — plugin drops are blocked when using Silk Touch.
- **Ore Blocking** — vanilla ore drops are disabled; only configured custom drops apply.
- **CobbleX System** — craftable CobbleX exchange item (separate `/cx` command).

---

## Requirements

- **Server:** Paper 1.21.x (or compatible forks)
- **Java:** 21
- **Build Tool:** Gradle 8.7+ (Kotlin DSL)

---

## Installation

1. Download the latest `OreFlow-<version>.jar` from [Releases](../../releases).
2. Place the jar in your server's `plugins/` folder.
3. Start the server.
4. Edit the generated configs in `plugins/OreFlow/` to your liking.
5. Run `/oreflow reload` or restart the server.

---

## Quick Start

1. Open the drop menu: `/oreflow`
2. Right-click a drop to enable/disable it.
3. Left-click a drop to enable/disable its Action Bar message.
4. Use navigation paper buttons to browse pages.
5. Admins can open the global admin panel: `/oreflow admin`

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/oreflow` | Open the drop GUI | `oreflow.gui` |
| `/oreflow reload` | Reload all configs | `oreflow.reload` |
| `/oreflow info` | Show plugin info | `oreflow.info` |
| `/oreflow help` | Show help message | `oreflow.help` |
| `/oreflow cobble` | Toggle cobblestone drop | `oreflow.cobble` |
| `/oreflow toggle <drop> [player]` | Toggle a specific drop | `oreflow.toggle` / `oreflow.toggle.others` |
| `/oreflow language <pl/en/de>` | Change server language | `oreflow.admin` |
| `/oreflow admin` | Open admin drop panel | `oreflow.admin` |
| `/oreflow creativemsg` | Toggle creative mode message | `oreflow.admin` |
| `/oreflow debug [blocks]` | Simulate mining drops | `oreflow.admin` |

### Aliases
- `/of`
- `/drop`

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `oreflow.*` | op | All permissions |
| `oreflow.gui` | true | Open the GUI |
| `oreflow.mine` | true | Receive plugin drops while mining |
| `oreflow.cobble` | true | Toggle cobblestone drop |
| `oreflow.toggle` | true | Toggle own drops |
| `oreflow.toggle.others` | op | Toggle drops for other players |
| `oreflow.reload` | op | Reload configs |
| `oreflow.admin` | op | Admin commands and GUI |
| `oreflow.info` | true | Plugin info command |
| `oreflow.help` | true | Help command |

---

## Configuration Files

All configs are located in `plugins/OreFlow/`.

| File | Purpose |
|------|---------|
| `config.yml` | Main settings: language, worlds, mineable blocks, EXP settings |
| `drops.yml` | Drop definitions: material, chance, amount, Fortune, Y-level, tool, EXP |
| `gui.yml` | GUI layout: title, rows, decorations, drop section, buttons |
| `generators.yml` | Stone generator definitions (WIP) |
| `lang/<code>.yml` | Translations (PL, EN, DE included) |
| `players.yml` | Per-player settings |

> **Note:** OreFlow uses `saveResource(..., false)`, so existing config files are **not overwritten** on startup. Delete a file to regenerate it with default values.

---

## Placeholders

### Drop Item Placeholders (`gui.yml`)

| Placeholder | Description |
|-------------|-------------|
| `{drop}` | Drop key (e.g. `diamond`) |
| `{capitalized_drop}` | Capitalized drop key |
| `{material}` | Bukkit material name |
| `{color}` | Item color hex |
| `{status}` | Enabled/disabled status |
| `{chance}` | Base drop chance |
| `{min}` / `{max}` | Min/max base amount |
| `{fortune_enabled}` | Is Fortune enabled for this drop |
| `{fortune1_chance}` / `{fortune2_chance}` / `{fortune3_chance}` | Fortune bonus chance |
| `{fortune1_min}` / `{fortune2_min}` / `{fortune3_min}` | Fortune min bonus |
| `{fortune1_max}` / `{fortune2_max}` / `{fortune3_max}` | Fortune max bonus |
| `{tool}` | Required tool |
| `{y_level}` | Y-level range |
| `{exp}` | EXP reward |
| `{line}` | Separator line |
| `{toggle_action}` | "enable" / "disable" |

### Message Placeholders (`lang/<code>.yml`)

`{player}`, `{drop}`, `{amount}`, `{material}`, `{exp}`, `{permission}`, `{time}`, `{target}`, `{status}`

---

## Building from Source

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`.

To run a test server:

```bash
./gradlew runServer
```

---

## Roadmap

See [OreFlow_Roadmap.html](OreFlow_Roadmap.html) for the full development roadmap and sprint progress.

---

## Support

Found a bug or have a suggestion?

- Open an [Issue](../../issues)
- Join our Discord *(link coming soon)*

---

## License

This project is proprietary and maintained by **Ljimex**. All rights reserved.

---

## Authors

- **Ljimex** — [GitHub](https://github.com/Ljinmex)
