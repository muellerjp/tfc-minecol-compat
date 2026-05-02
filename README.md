# MineColonies × TerraFirmaCraft Compat

A compatibility mod that makes [MineColonies](https://github.com/ldtteam/Minecolonies)
correctly recognise [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft)
tool tiers and weapon types.

## The problem

MineColonies determines a tool's tier by reading `Tier.getAttackDamageBonus()`.
In vanilla this returns values in the range 0–4 (wood → netherite).
TFC repurposes that field as a combat-damage multiplier (2.0–9.0), so without
this mod all TFC tools appear massively over-levelled to MineColonies colonists.

Additionally, TFC adds weapon types (mace, javelin) that MineColonies would not
recognise as swords, preventing guards from equipping them.

## What this mod does

**Tool levels** — registers a `Compatibility.registerEquipmentLevelProvider` that
calls `LevelTier.level()` whenever it is available, bypassing `getAttackDamageBonus()`.

**Sword levels** — patches MineColonies' sword level function to run the same
custom-provider chain used by axes, pickaxes, etc., so TFC swords are also levelled
correctly.

**Weapon recognition** — registers a `Compatibility.registerWeaponRecognizer` for
`JavelinItem` and `TFCMaceItem` so guards can equip and use them.  The mace level
is resolved via a reverse-lookup into TFC's metal item registry at runtime.

## Requirements

| Dependency      | Version        |
|-----------------|----------------|
| Minecraft       | 1.21.1         |
| NeoForge        | 21.1.197+      |
| MineColonies    | 0.0.11+        |
| TerraFirmaCraft | latest 1.21.1  |

## Installation

Drop the built jar alongside your MineColonies and TerraFirmaCraft jars in your
`mods/` folder.  No configuration required.

## Equipment level reference

### TFC metal → MineColonies tool level

MineColonies uses tool levels 0–5 to gate what tools colonists can hold.
This mod maps TFC's `LevelTier.level()` values as follows:

| TFC tier | Metals | MC tool level | Vanilla equivalent |
|---|---|:---:|---|
| Stone | Igneous intrusive/extrusive, sedimentary, metamorphic | 0 | Wood / Gold |
| Copper | Copper | 1 | Stone |
| Bronze | Bronze, Bismuth Bronze, Black Bronze | 2 | Iron |
| Wrought Iron | Wrought Iron | 3 | Diamond |
| Steel | Steel | 4 | Netherite |
| Black Steel | Black Steel | 5 | — |
| Blue / Red Steel | Blue Steel, Red Steel | 6 | — |

> Levels 5 and 6 are above the vanilla ceiling but MineColonies level-5 buildings
> accept any tool (max = `Integer.MAX_VALUE`), so black/blue/red-steel tools always
> work at a fully upgraded hut.

### MineColonies building level → accepted tool range

Every colonist accepts tools whose level satisfies `minLevel ≤ toolLevel ≤ maxLevel`.
The **maximum** is set by the hut level; the **minimum** is 0 (wood/gold) for most workers.

| Hut level | Min tool level | Max tool level | Accepted TFC metals |
|:---:|:---:|:---:|---|
| 1 | 0 | 1 | Stone tools, Copper |
| 2 | 0 | 2 | Stone tools, Copper, Bronze variants |
| 3 | 0 | 3 | + Wrought Iron |
| 4 | 0 | 4 | + Steel |
| 5 | 0 | unlimited | + Black Steel, Blue/Red Steel |

> A colonist will **refuse** a tool that exceeds their hut's max level.  Give your
> level-1 miner a copper pickaxe, not a bronze one.

This table applies to all tool-using colonists (Miner → pickaxe, Lumberjack → axe,
Farmer → hoe, Builder → pickaxe/axe/shovel, Guards → sword + armor,
Fisher → fishing rod, Herders → shears/sword).

## Test-playing in development

The Gradle setup includes pre-configured run tasks that launch Minecraft with all
required mods loaded automatically.

### Pointing to your sibling projects

The build needs to know where your local MineColonies and TerraFirmaCraft checkouts
are so it can pick up their built JARs.  The defaults are `../minecolonies` and
`../TerraFirmaCraft` (siblings of this folder).

If your layout differs, pass the paths via `-P` flags:

```bash
./gradlew runClient \
  -PminecoloniesProjectDir=C:/path/to/minecolonies \
  -PtfcProjectDir=C:/path/to/TerraFirmaCraft
```

Or set them permanently by uncommenting and editing these lines in
`gradle.properties`:

```properties
minecoloniesProjectDir=../minecolonies
tfcProjectDir=../TerraFirmaCraft
```

### Prerequisites

Build the sibling projects once (only needed again when their code changes):

```bash
cd ../minecolonies      && ./gradlew build
cd ../TerraFirmaCraft   && ./gradlew build
```

### Running the client

```bash
./gradlew runClient
```

Before launching, Gradle syncs all mod JARs into `run/client/mods/`:
- The MineColonies and TerraFirmaCraft JARs built above.
- Companion mods required at runtime (Structurize, BlockUI, Domum Ornamentum,
  MultiPiston, Patchouli) — downloaded from Maven automatically on first run.

The game directory is `run/client/`.  World saves persist between runs.

### Running a dedicated server

```bash
./gradlew runServer
```

Mod JARs are synced into `run/server/mods/` the same way.  On the first run you
will be asked to accept the Minecraft EULA — create `run/server/eula.txt` with the
single line `eula=true` beforehand to skip the prompt.

The server runs without a GUI (`--nogui`); its data lives in `run/server/`.

### What is loaded

All three mods (MineColonies, TerraFirmaCraft, and this compat mod) plus their
required companions load together in a single session.  Debug-level logging is
enabled by default, which makes it easy to spot FML loading errors or missing
dependency warnings in the console output.

## Building

```bash
./gradlew build
```

The output jar will be in `build/libs/`.

> **Note:** This project compiles against local sibling builds of MineColonies and
> TerraFirmaCraft.  Run `./gradlew build` in each sibling project first, then build
> this one.  See `gradle.properties` to override the default `../minecolonies` and
> `../TerraFirmaCraft` paths if your directory layout differs.

## License

[MIT](LICENSE)
