# Datapack Config Mod

**让 Minecraft 数据包拥有可视化的图形配置界面。**

数据包作者只需在数据包中放入一个 `datapack_config.json` 配置文件，模组会自动解析并生成 Cloth Config 配置界面，玩家可以直接在游戏里修改数据包的各种参数，修改会自动写入数据包文件。

---

## 功能

### 为数据包提供配置 GUI
- 数据包作者编写 `datapack_config.json`，定义配置项
- 模组自动为每个数据包在 Cloth Config 中创建独立的配置分类
- 支持配置类型：**布尔开关 / 整数 / 浮点数 / 滑块 / 下拉菜单 / 枚举选择器 / 颜色选择器 / 文本输入**
- 支持**分类分组**，将配置项组织到可折叠的子类别中
- 支持**批量修改**：一个配置项可以同时修改数据包内多个文件

### 本地化支持
- 完整的**中英文双语**界面
- 配置项的显示名称、描述、tooltip 均可按语言设置
- 自动匹配游戏当前语言

### 全局数据包加载
- 支持从 `config/datapack-config-mod/` 文件夹自动加载`.zip`数据包
- 可在配置界面中**开关**每个数据包
- 数据包以最高优先级加载

### 主菜单快捷入口
- 在主菜单右上角添加"数据包配置"按钮，一键进入配置界面

---

## 安装

1. 确保已安装 **Minecraft Forge 47.4+**（对应 1.20.1）
2. 将模组 `.jar` 放入 `.minecraft/mods/`
3. 安装 **Cloth Config for Forge 11.x**（必需，配置界面依赖它）

## 使用

### 数据包作者
在你的数据包根目录下创建 `datapack_config.json`，参考 `example_datapack/` 中的示例：

```json
{
  "config": {
    "my_setting": {
      "type": "boolean",
      "default": "true",
      "category": "general",
      "name": {
        "zh_cn": "我的设置",
        "en_us": "My Setting"
      },
      "description": {
        "zh_cn": "这是设置说明",
        "en_us": "This is the setting description"
      },
      "filePath": "data/mydatapack/config/settings.json",
      "key": "my_setting"
    }
  }
}
```

支持的所有类型：`boolean` `int` `double` `slider` `double_slider` `enum` `enum_selector` `string` `color`

### 玩家
- 在**主菜单**右上角点击"数据包配置"按钮
- 或在 **Mods** 列表中找到 "Datapack Config Mod"，点击 **Config**
- 在配置界面中调整各数据包的参数，点击 **保存** 后自动生效

---

## 开发

```bash
./gradlew build
```

编译产物在 `build/libs/` 目录下。

## 依赖

| 依赖 | 版本 | 必要性 |
|------|------|--------|
| Cloth Config for Forge | 11.x | 必需 |
| Minecraft Forge | 47.4+ | 必需 |

## 许可 / License

MIT

---

## 文档 / Documentation

本模组提供中英双语使用指南：

- [快速入门指南 (中文)](https://github.com/wonderfulaichen/custom-pack-config/blob/main/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8%E6%8C%87%E5%8D%97.md)
- [Quick Start Guide (English)](https://github.com/wonderfulaichen/custom-pack-config/blob/main/QUICKSTART.md)
- [完整使用指南 (中文)](https://github.com/wonderfulaichen/custom-pack-config/blob/main/%E6%A8%A1%E7%BB%84%E5%AE%8C%E6%95%B4%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97.md)
- [Full Usage Guide (English)](https://github.com/wonderfulaichen/custom-pack-config/blob/main/FULL_USAGE_GUIDE.md)

---

*Datapack Config Mod — 让数据包配置像改设置一样简单 / Making datapack config as easy as changing settings.*
