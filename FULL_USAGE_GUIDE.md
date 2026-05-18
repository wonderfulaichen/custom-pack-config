# Datapack Config Mod - Full Usage Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Core Features](#core-features)
3. [How It Works](#how-it-works)
4. [Installation & Configuration](#installation--configuration)
5. [Config Type Reference](#config-type-reference)
6. [Advanced Features](#advanced-features)
7. [Localization Support](#localization-support)
8. [Example Datapack](#example-datapack)
9. [Performance Optimization](#performance-optimization)
10. [FAQ](#faq)
11. [Changelog](#changelog)

---

## Introduction

### What is the Datapack Config Mod?

The Datapack Config Mod is a Forge 1.20.1 utility mod that provides datapack developers with an easy-to-use graphical configuration interface. By integrating the Cloth Config API, players can modify datapack parameters directly in-game without manually editing JSON files or restarting the game.

### Key Features

- **🎨 Graphical Config UI**: Clean, user-friendly interface powered by Cloth Config
- **🔧 10+ Config Types**: Sliders, input fields, dropdown menus, color pickers, and more
- **📝 Real-time Updates**: Changes take effect immediately, no restart required
- **🌍 Multilingual**: Full i18n support for Simplified Chinese and English
- **📦 Batch Editing**: One config entry can modify multiple files at once
- **🔄 Nested Path Support**: JSON nested paths (e.g. `terrain/scale`)
- **💾 ZIP Datapack Support**: Read and modify datapacks in ZIP format
- **🛡️ Safe & Reliable**: Auto-backup, temp file mechanism, comprehensive error handling

---

## Core Features

### 1. Config Management

The mod provides complete config management:

- **Auto-scanning**: Automatically scans the datapack directory for all datapacks
- **Config Loading**: Parses each datapack's config definition automatically
- **UI Building**: Dynamically generates a config screen for each datapack
- **Save & Apply**: Writes config changes back to the datapack files

### 2. Supported Config Types

| Type ID | Data Type | UI Component | Description |
|---------|-----------|--------------|-------------|
| `slider` | Integer | Integer Slider | Drag to adjust integer values |
| `double_slider` | Double | Float Slider | Drag to adjust float values |
| `int` | Integer | Integer Input | Direct integer input |
| `double` | Double | Float Input | Direct float input |
| `boolean` | Boolean | Toggle Switch | Enable/disable toggle |
| `string` | String | Text Input | Text entry |
| `enum` | String | Dropdown Menu | Select from predefined options |
| `enum_selector` | String | Cycle Button | Cycle through options |
| `color` | Integer (HEX) | Color Picker | RGB/ARGB color selection |
| `dropdown_menu` | String | Selector Component | Custom dropdown menu |

### 3. Datapack Format Support

- **Folder format**: Standard datapack folder structure
- **ZIP format**: Auto-extract, modify, and repack
- **Auto-backup**: ZIP files are backed up before modification

---

## How It Works

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Minecraft Game                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                 DataPackConfigMod                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Init       │  │  Config UI   │  │  Datapack    │      │
│  │   Setup      │  │  Builder     │  │  Manager     │      │
│  │   Events     │  │  Parser      │  │  File Editor │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Cloth Config API                               │
│              └─ GUI Framework                               │
└─────────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Datapack Files                                 │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────┐  │
│  │  datapack_     │  │  JSON Files    │  │  Functions    │  │
│  │  config.json   │  │                │  │  .mcfunction  │  │
│  └────────────────┘  └────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Config Flow

#### Phase 1: Initialization

```
Game Start
  ↓
Register Server Events
  ↓
Register Config Events
  ↓
Create Datapack Directory
  ↓
Scan and Load Datapacks
```

#### Phase 2: Config UI Building

```
Open Config Screen
  ↓
Scan Datapack Directory
  ↓
Parse Each Datapack Config
  ↓
Create Independent Category per Datapack
  ↓
Add Enable/Disable Toggle
  ↓
Organize Items by Config Category
  ↓
Create UI Components by Type
```

#### Phase 3: Save & Apply

```
User Clicks "Done"
  ↓
Save Settings to datapack-settings.json
  ↓
Get Datapack Folder Path
  ↓
Iterate All Datapack Settings
  ↓
Apply Config to Datapack Files
  ↓
ZIP Format: Extract → Modify → Repack
```

### File Modification Mechanism

#### JSON File Modification

```javascript
// Original file
{
  "terrain": {
    "scale": 1.0
  }
}

// Config: key = "terrain/scale", value = 2.0
// Resulting file
{
  "terrain": {
    "scale": 2.0
  }
}
```

#### Function File Modification

```mcfunction
# Original file
set debug_enabled false

# Config: key = "debug_enabled", value = true
# Resulting file
set debug_enabled true
```

---

## Installation & Configuration

### 1. Requirements

- Minecraft: 1.20.1
- Forge: 47.2.0 or higher
- Cloth Config for Forge: latest version (recommended)

### 2. Installation Steps

1. Download the mod JAR file
2. Place it in the `mods` folder
3. Ensure Cloth Config for Forge is also installed
4. Launch the game

### 3. Config File Locations

| File | Path | Purpose |
|------|------|---------|
| `datapack-config-mod.toml` | `config/datapack-config-mod.toml` | Forge config file |
| `datapack-settings.json` | `config/datapack-config-mod/datapack-settings.json` | Datapack settings storage |
| `datapack_config.json` | Root of each datapack | Datapack config definition |

### 4. Datapack Directory Path

Default path: `config/datapack-config-mod`

Can be changed in the Forge config file:

```toml
[general]
# Datapack folder path
datapack_folder_path = "config/datapack-config-mod"
```

---

## Config Parameter Reference

### Core Parameters

Each config entry requires these core parameters:

#### 1. `type` (Required)

**Type**: `String`

**Description**: The data type of the config entry, determining the UI component and validation rules.

**Valid values**: `slider`, `double_slider`, `int`, `double`, `boolean`, `string`, `enum`, `enum_selector`, `color`, `dropdown_menu`

**Example**:
```json
"type": "slider"
```

---

#### 2. `default` (Required)

**Type**: `String`

**Description**: Default value of the config entry, must be a string.

**Note**: All default values must be quoted, even numbers.

**Example**:
```json
"default": "50"       // Correct ✓
"default": 50         // Incorrect ✗
"default": "true"     // Correct ✓
"default": true       // Incorrect ✗
"default": "#FFFF5733" // RGB format Correct ✓
"default": "#FF5733"   // ARGB format Correct ✓
```

---

#### 3. `key` (Required or Optional)

**Type**: `String`

**Description**: The identifier for this config entry in the target file.

**Usage**:
- JSON files: JSON path (supports nested paths with `/`)
- Function files: Parameter name in commands
- Text files: Config identifier

**Required**: When using `filePath`, this is required. When using `locations`, this is not needed.

**Example**:
```json
// Simple key
"key": "spawn_rate"

// Nested JSON path
"key": "terrain/scale"

// Deep nesting
"key": "argument1/argument/xz_scale"
```

**Notes**:
- Use `/` as the separator for nested paths
- Each level in the path corresponds to one level in the JSON object
- If a path level doesn't exist, the mod will create it automatically

---

#### 4. `filePath` (Required or Optional)

**Type**: `String`

**Description**: The path to the target file within the datapack.

**Required**: When using `key`, this is required. When using `locations`, this is not needed.

**Path format**:
- Must start with `data/`
- Relative to the datapack root
- Supports JSON and `.mcfunction` files

**Example**:
```json
// JSON config file
"filePath": "data/example/config/settings.json"

// Function file
"filePath": "data/example/functions/setup.mcfunction"

// Deep path
"filePath": "data/example/worldgen/biome/forest.json"
```

**Supported file types**:
- `.json`: JSON config files, supports nested paths
- `.mcfunction`: Function files, supports command parameter modification
- `.txt`: Text files (partial support)

**Notes**:
- Paths are case-sensitive
- The file must exist, otherwise the config cannot be applied
- Use relative paths, not absolute paths

---

#### 5. `locations` (Optional)

**Type**: `Array<Object>`

**Description**: Batch editing — one config entry can modify multiple file locations simultaneously.

**Use case**: When you need to apply a single config value to multiple files or locations.

**Structure**:
```json
"locations": [
  {
    "filePath": "String",      // Target file path
    "key": "String",           // Config key
    "description": "String"    // Description (optional)
  }
]
```

**Example**:
```json
{
  "debug_mode": {
    "type": "boolean",
    "default": "false",
    "locations": [
      {
        "filePath": "data/example/config/debug.json",
        "key": "debug.enabled",
        "description": "Main debug config"
      },
      {
        "filePath": "data/example/config/main.json",
        "key": "debug_mode",
        "description": "Debug toggle in main config"
      },
      {
        "filePath": "data/example/functions/debug.mcfunction",
        "key": "debug_enabled",
        "description": "Debug function control"
      }
    ]
  }
}
```

**Comparison with `filePath` + `key`**:
- `filePath` + `key`: Single location modification, simple setups
- `locations`: Multi-location modification, ideal for global toggles and synchronized parameters

**Notes**:
- `locations` and `filePath`+`key` cannot be used simultaneously
- Each object in the array must have both `filePath` and `key`
- `description` is optional, used for debugging and logging

---

#### 6. `name` (Optional)

**Type**: `Object` (language map)

**Description**: The display title of the config entry in the UI.

**Structure**:
```json
"name": {
  "zh_cn": "简体中文标题",
  "en_us": "English Title"
}
```

**Example**:
```json
"name": {
  "zh_cn": "生物生成率",
  "en_us": "Mob Spawn Rate"
}
```

**Priority**: Higher than `displayNameKey`

**If not configured**: Displays the config entry's key name

---

#### 7. `description` (Optional)

**Type**: `Object` (language map)

**Description**: Explanatory text displayed below the config entry.

**Display location**: Below the config entry name, in smaller gray text.

**Structure**:
```json
"description": {
  "zh_cn": "简体中文描述",
  "en_us": "English Description"
}
```

**Example**:
```json
"description": {
  "zh_cn": "控制生物的生成频率，数值越大生成越快",
  "en_us": "Controls mob spawn frequency, higher values spawn faster"
}
```

**Difference from `tooltip`**:
- `description`: Always visible below the config entry
- `tooltip`: Shown on hover

---

#### 8. `tooltip` (Optional)

**Type**: `Object` (language map)

**Description**: Tooltip text shown on mouse hover.

**Trigger**: Hover over the config entry.

**Structure**:
```json
"tooltip": {
  "zh_cn": "简体中文提示",
  "en_us": "English Tooltip"
}
```

**Example**:
```json
"tooltip": {
  "zh_cn": "拖动滑块调整数值，或点击输入精确值",
  "en_us": "Drag slider to adjust value, or click to input precise value"
}
```

**Difference from `description`**:
- `tooltip`: Shown on hover, suitable for detailed explanations
- `description`: Always visible, suitable for brief notes

---

#### 9. `category` (Optional)

**Type**: `String`

**Description**: Groups config entries under a collapsible sub-category in the UI.

**Example**:
```json
"category": "general"
```

**Usage**: Multiple entries with the same `category` value will be grouped together.

---

#### 10. `min` / `max` (Optional, for numeric types)

**Type**: `Number`

**Description**: Minimum and maximum values for numeric config types (`slider`, `double_slider`, `int`, `double`).

**Example**:
```json
"min": 0,
"max": 100
```

---

#### 11. `step` (Optional, for slider types)

**Type**: `Number`

**Description**: The step/increment for slider types.

**Example**:
```json
"step": 1
```

---

#### 12. `enumValues` (Required for enum types)

**Type**: `Array<String>`

**Description**: The list of selectable values for `enum` and `enum_selector` types.

**Example**:
```json
"enumValues": ["easy", "normal", "hard"]
```

---

#### 13. `enumDisplayNames` (Optional, for enum types)

**Type**: `Object` (language map)

**Description**: Display names for each enum value, supporting localization.

**Example**:
```json
"enumDisplayNames": {
  "zh_cn": {
    "easy": "简单",
    "normal": "普通",
    "hard": "困难"
  },
  "en_us": {
    "easy": "Easy",
    "normal": "Normal",
    "hard": "Hard"
  }
}
```

---

## Config Type Details

### Type Reference Table

| Type | Data Type | UI | Default Value | Notes |
|------|-----------|-----|--------------|-------|
| `slider` | Integer | Slider + Input | Quoted string | Use `min`/`max`/`step` |
| `double_slider` | Double | Slider + Input | Quoted string | Use `min`/`max`/`step` |
| `int` | Integer | Text field | Quoted string | Use `min`/`max` |
| `double` | Double | Text field | Quoted string | Use `min`/`max` |
| `boolean` | Boolean | Toggle switch | `"true"` or `"false"` | |
| `string` | String | Text field | Quoted string | |
| `enum` | String | Dropdown | Quoted value | Use `enumValues` |
| `enum_selector` | String | Cycle button | Quoted value | Use `enumValues` |
| `color` | Integer | Color picker | HEX string | `#RRGGBB` or `#RRGGBBAA` |
| `dropdown_menu` | String | Selector | Quoted value | Custom dropdown |

### Type-Specific Notes

#### slider / double_slider
- Default values must be within `min`-`max` range
- `step` defines the increment per slider movement

#### boolean
- Only accepts `"true"` or `"false"` as defaults
- Case-insensitive when applied to files

#### color
- Supports `#RRGGBB` (RGB) and `#RRGGBBAA` (ARGB) formats
- The value is stored as a Hex integer in the target file
- The color picker provides a visual interface with saturation/brightness sliders

#### enum / enum_selector
- `enum` shows a dropdown menu
- `enum_selector` cycles through options on each click
- `enumDisplayNames` can be localized per language

---

## Advanced Features

### 1. Nested JSON Path Modification

The mod supports deeply nested JSON paths using `/` as the separator.

**Example**: Modify a nested structure

Config entry:
```json
{
  "world_settings": {
    "type": "double_slider",
    "default": "1.0",
    "filePath": "data/mydatapack/worldgen/noise_settings.json",
    "key": "terrain/scale",
    "name": {
      "zh_cn": "地形缩放",
      "en_us": "Terrain Scale"
    },
    "min": 0.1,
    "max": 10.0,
    "step": 0.1
  }
}
```

This will modify the `terrain` → `scale` field in the target JSON file.

### 2. Batch File Modification

Use `locations` to apply one config value to multiple files:

```json
{
  "feature_toggle": {
    "type": "boolean",
    "default": "true",
    "locations": [
      { "filePath": "data/mydatapack/config/server.json", "key": "feature_enabled" },
      { "filePath": "data/mydatapack/config/client.json", "key": "feature_toggle" },
      { "filePath": "data/mydatapack/functions/init.mcfunction", "key": "feature_flag" }
    ],
    "name": {
      "zh_cn": "功能开关",
      "en_us": "Feature Toggle"
    }
  }
}
```

### 3. Global Datapack Loading

Datapacks placed in `config/datapack-config-mod/` are loaded globally at the highest priority level. Players can enable/disable each datapack from the config screen.

**Supported formats**:
- Folder-based datapacks
- `.zip` compressed datapacks (auto-extracted and repacked on modification)

### 4. Config Categories

Use the `category` parameter to organize config entries into collapsible groups:

```json
{
  "spawn_rate": {
    "type": "slider",
    "category": "mob_settings",
    "name": { "en_us": "Spawn Rate" }
  },
  "damage_multiplier": {
    "type": "double_slider",
    "category": "mob_settings",
    "name": { "en_us": "Damage Multiplier" }
  }
}
```

Both entries will appear under a "mob_settings" collapsible section.

---

## Localization Support

The mod fully supports bilingual localization (Simplified Chinese and English).

### Language Resolution Order

1. `name` / `description` / `tooltip` directly in the config entry (highest priority)
2. `displayNameKey` / `descriptionKey` / `tooltipKey` (language keys)
3. Fall back to the default language's value
4. Fall back to the raw key name

### Example: Full Localization

```json
{
  "spawn_rate": {
    "type": "slider",
    "default": "50",
    "name": {
      "zh_cn": "生成率",
      "en_us": "Spawn Rate"
    },
    "description": {
      "zh_cn": "控制生物的生成频率",
      "en_us": "Controls mob spawn frequency"
    },
    "tooltip": {
      "zh_cn": "拖动滑块调整数值",
      "en_us": "Drag the slider to adjust"
    },
    "filePath": "data/example/config.json",
    "key": "spawn_rate",
    "min": 0,
    "max": 100,
    "step": 1
  }
}
```

---

## Example Datapack

A complete example datapack is provided in the `example_datapack/` directory. It demonstrates:

- All config types in action
- Category grouping
- Localized display names
- Multiple file paths

**Structure**:
```
example_datapack/
├── pack.mcmeta
├── datapack_config.json
└── data/
    └── example/
        ├── config/
        │   ├── settings.json
        │   ├── gameplay.json
        │   └── visual.json
        └── functions/
            └── setup.mcfunction
```

---

## Performance Optimization

### Features

- **Thread Safety**: All file operations use synchronized access patterns
- **Delayed Saving**: Changes are queued and saved in batches to reduce I/O
- **Memory Optimization**: Config data is cached and lazy-loaded
- **Efficient JSON Parsing**: Uses streaming parser for large files

### Best Practices

- Keep `datapack_config.json` concise — only define configurable parameters
- Use `category` for logical grouping instead of creating many separate datapacks
- For large datapacks, prefer folder format over ZIP (faster iteration)

---

## FAQ

### Q: Changes not taking effect?

A: Check the following:
1. The datapack may need to be reloaded (try re-entering the world or using `/reload`)
2. Verify the `filePath` in `datapack_config.json` is correct relative to the datapack root
3. Check the game log (`latest.log`) for config-related errors

### Q: Config screen not displaying correctly?

A: Check:
1. The JSON syntax in `datapack_config.json` is valid
2. All required parameters are present for each entry type
3. The file encoding is UTF-8 (no BOM)

### Q: Configuration not saving?

A: Possible causes:
1. File permissions — the game may not have write access
2. Datapack is inside a ZIP file — ensure the ZIP is not corrupted
3. If using global datapack loading, verify the datapack is enabled in the config screen

### Q: How do I make a config entry affect multiple files?

A: Use the `locations` array instead of the `filePath` + `key` pair:

```json
{
  "my_setting": {
    "type": "boolean",
    "default": "true",
    "locations": [
      { "filePath": "data/mypack/file1.json", "key": "setting_a" },
      { "filePath": "data/mypack/file2.json", "key": "setting_b" }
    ]
  }
}
```

### Q: How to modify nested JSON fields?

A: Use `/` as the path separator in the `key` field:

```json
"key": "parent/child/grandchild"
```

This modifies:
```json
{
  "parent": {
    "child": {
      "grandchild": <value>
    }
  }
}
```

### Q: Does the mod affect vanilla Minecraft functionality?

A: No. The mod only modifies files within the configured datapack paths. It does not alter vanilla game mechanics, registries, or world saves.

### Q: Can I use this mod on a server?

A: The mod is primarily client-side for config UI. For server-side datapack configuration, the mod needs to be installed on the server. The global datapack loading feature works on both client and dedicated server environments.

---

## Changelog

### v0.0.1 — Initial Release

- Initial release of Datapack Config Mod
- Datapack config GUI via Cloth Config API
- Support for 10 config types: boolean, int, double, slider, double_slider, string, enum, enum_selector, color, dropdown_menu
- Config categories for collapsible grouping
- Batch modification via `locations` array
- Nested JSON path support (`/` separator)
- Full bilingual localization (zh_cn / en_us)
- Global datapack loading from `config/datapack-config-mod/`
- Main menu shortcut button
- ZIP datapack support with auto-backup
- Thread-safe file operations with delayed saving

---

## Getting Help

- 📖 Read the [Quick Start Guide](./QUICKSTART.md)
- 📦 Study the [Example Datapack](./example_datapack/)
- 💬 Submit an Issue on GitHub

---

**Happy datapacking!** 🎉
