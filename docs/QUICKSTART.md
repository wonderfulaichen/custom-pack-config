# Datapack Config Mod - Quick Start Guide

## Introduction

This quick start guide helps you get up and running with the Datapack Config Mod.

### Version Info
- **Current Version**: 0.0.1
- **Optimization**: ✅ Code performance optimization completed
- **Performance**: Thread-safe, delayed saving, memory-optimized
- **Compatibility**: Minecraft Forge 1.20.1

---

## 5-Minute Quick Start

### 1. Install the Mod

1. Download and install Minecraft Forge 1.20.1
2. Download Cloth Config for Forge and place it in the `mods` folder (required — the config UI depends on it)
3. Download this mod and place it in the `mods` folder
4. Launch the game

### 2. Open the Config Screen

There are two ways to open the config screen:

#### Method 1: Main Menu Button
- Open the game's main menu
- Click the "Datapack Config" button in the top-right corner

#### Method 2: Mods List (if Cloth Config is installed)
- Open the game's main menu
- Click "Mods"
- Find "Datapack Config Mod"
- Click the Config button

### 3. Configure Your Datapack

In the config screen:
1. Browse the list of loaded datapacks
2. Select a datapack to configure
3. Adjust the settings
4. Click "Done" to save

---

## Common Config Types

### 1. Boolean Switch (boolean)

**Usage**: Enable/disable features

**Example**:
```json
{
  "enabled": {
    "type": "boolean",
    "default": "true",
    "name": {"zh_cn": "启用功能", "en_us": "Enable Feature"},
    "filePath": "data/example/config.json",
    "key": "enabled"
  }
}
```

### 2. Integer Slider (slider)

**Usage**: Adjust integer values (0-100)

**Example**:
```json
{
  "spawn_rate": {
    "type": "slider",
    "default": "50",
    "min": 0,
    "max": 100,
    "step": 1,
    "name": {"zh_cn": "生成率", "en_us": "Spawn Rate"},
    "filePath": "data/example/config.json",
    "key": "spawn_rate"
  }
}
```

### 3. Float Slider (double_slider)

**Usage**: Adjust floating-point values

**Example**:
```json
{
  "damage_multiplier": {
    "type": "double_slider",
    "default": "1.0",
    "min": 0.1,
    "max": 5.0,
    "step": 0.1,
    "name": {"zh_cn": "伤害倍率", "en_us": "Damage Multiplier"},
    "filePath": "data/example/config.json",
    "key": "damage_multiplier"
  }
}
```

### 4. Dropdown Menu (enum)

**Usage**: Select from predefined options

**Example**:
```json
{
  "difficulty": {
    "type": "enum",
    "default": "normal",
    "enumValues": ["easy", "normal", "hard"],
    "name": {"zh_cn": "难度", "en_us": "Difficulty"},
    "enumDisplayNames": {
      "zh_cn": {"easy": "简单", "normal": "普通", "hard": "困难"},
      "en_us": {"easy": "Easy", "normal": "Normal", "hard": "Hard"}
    },
    "filePath": "data/example/config.json",
    "key": "difficulty"
  }
}
```

### 5. Color Picker (color)

**Usage**: Pick colors

**Example**:
```json
{
  "theme_color": {
    "type": "color",
    "default": "#FF5733",
    "name": {"zh_cn": "主题颜色", "en_us": "Theme Color"},
    "filePath": "data/example/config.json",
    "key": "theme_color"
  }
}
```

---

## Create Your First Config File

### Step 1: Create the Datapack Folder

```
my_datapack/
├── pack.mcmeta
├── datapack_config.json
└── data/
    └── mynamespace/
        └── config.json
```

### Step 2: Create pack.mcmeta

```json
{
  "pack": {
    "pack_format": 15,
    "description": "My Datapack"
  }
}
```

### Step 3: Create the Config File (config.json)

```json
{
  "enabled": true,
  "difficulty": "normal"
}
```

### Step 4: Create datapack_config.json

```json
{
  "metadata": {
    "name": {
      "zh_cn": "我的数据包",
      "en_us": "My Datapack"
    },
    "description": {
      "zh_cn": "我的第一个数据包配置",
      "en_us": "My first datapack config"
    }
  },
  "config": {
    "enabled": {
      "type": "boolean",
      "default": "true",
      "name": {
        "zh_cn": "启用数据包",
        "en_us": "Enable Datapack"
      },
      "filePath": "data/mynamespace/config.json",
      "key": "enabled"
    },
    "difficulty": {
      "type": "enum",
      "default": "normal",
      "enumValues": ["easy", "normal", "hard"],
      "name": {
        "zh_cn": "难度",
        "en_us": "Difficulty"
      },
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
      },
      "filePath": "data/mynamespace/config.json",
      "key": "difficulty"
    }
  }
}
```

### Step 5: Install the Datapack

1. Copy the `my_datapack` folder to the mod's datapack directory
   - Default path: `config/datapack-config-mod/my_datapack`
2. Launch the game
3. Open the config screen
4. View and adjust your settings

---

## FAQ

### Q: Changes not taking effect?

A: Check the following:
1. Make sure the datapack has been reloaded (re-entering the world usually helps)
2. Verify the `filePath` in `datapack_config.json` is correct
3. Check the game log for configuration errors

### Q: Config screen not displaying correctly?

A: Check:
1. JSON syntax is valid (use a JSON validator)
2. All required parameters are present
3. File encoding is UTF-8

### Q: How to modify multiple files with one config entry?

A: Use the `locations` array:

```json
{
  "debug_mode": {
    "type": "boolean",
    "default": "false",
    "locations": [
      {
        "filePath": "data/example/config1.json",
        "key": "debug",
        "description": "Config 1"
      },
      {
        "filePath": "data/example/config2.json",
        "key": "debug",
        "description": "Config 2"
      }
    ]
  }
}
```

### Q: How to modify nested JSON objects?

A: Use `/` as path separator:

```json
{
  "terrain_scale": {
    "type": "double_slider",
    "default": "1.0",
    "filePath": "data/example/terrain.json",
    "key": "terrain/scale"
  }
}
```

Corresponding file content:
```json
{
  "terrain": {
    "scale": 1.0
  }
}
```

### Q: How to add localization?

A: Add multilingual text to your config entries:

```json
{
  "difficulty": {
    "name": {
      "zh_cn": "难度",
      "en_us": "Difficulty"
    },
    "description": {
      "zh_cn": "设置游戏难度",
      "en_us": "Set game difficulty"
    },
    "tooltip": {
      "zh_cn": "点击选择难度",
      "en_us": "Click to select difficulty"
    }
  }
}
```

---

## Next Steps

- 📖 Read the [Full Usage Guide](./FULL_USAGE_GUIDE.md) for all features
- 📦 Check the [Example Datapack](./example_datapack/) to learn from real examples
- 🚀 Start creating your own datapack configs

---

## Getting Help

- 📖 Read the full documentation
- 📦 Study the example datapack
- 💬 Submit an Issue on GitHub

---

**Happy configuring!** 🎉
