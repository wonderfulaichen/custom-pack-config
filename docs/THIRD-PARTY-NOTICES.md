# Third-Party Notices

本模组（Datapack Config Mod）基于以下第三方项目和库构建。各项目的许可条款如下：

This mod (Datapack Config Mod) is built upon the following third-party projects and libraries. The license terms for each are listed below.

---

## Minecraft

- **Project**: Minecraft
- **Author**: Mojang AB
- **License**: [Mojang EULA](https://www.minecraft.net/en-us/eula)
- **Usage**: 本模组运行于 Minecraft Forge 1.20.1 环境，作为模组游戏平台。
  EN: This mod runs on Minecraft Forge 1.20.1 as the game platform.

**Notice**:
This mod is an unofficial creation and is not endorsed by, sponsored by, nor affiliated with Mojang AB. Minecraft is a registered trademark of Mojang AB.

本模组为非官方作品，未经 Mojang AB 认可、赞助或与其有关联。Minecraft 是 Mojang AB 的注册商标。

---

## Minecraft Forge

- **Project**: Minecraft Forge
- **Author**: Forge Development LLC
- **License**: [GNU Lesser General Public License v2.1](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/LICENSE.txt)
- **Usage**: 模组开发框架与加载器。
  EN: Mod development framework and loader.

### LGPL-2.1 Notice

This mod links dynamically to Minecraft Forge. Under the terms of LGPL-2.1, this does not require this mod itself to be licensed under LGPL-2.1. The mod's own code is licensed separately under the MIT License.

---

## Cloth Config

- **Project**: Cloth Config
- **Author**: shedaniel
- **Repository**: https://github.com/shedaniel/cloth-config
- **License**: [GNU Lesser General Public License v3.0](https://github.com/shedaniel/cloth-config/blob/master/LICENSE.md)
- **Usage**: 提供图形化配置界面（GUI）框架。本模组在运行时动态链接 Cloth Config API。
  EN: Provides the graphical configuration interface (GUI) framework. This mod dynamically links to Cloth Config at runtime.

### LGPL-3.0 Notice

```
Copyright (C) shedaniel

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
USA
```

---

## Parchment Mappings

- **Project**: Parchment Mappings
- **Author**: ParchmentMC
- **Repository**: https://github.com/ParchmentMC/Parchment
- **License**: [CC0 1.0 Universal (CC0 1.0)](https://github.com/ParchmentMC/Parchment/blob/main/LICENSE.txt)
- **Usage**: 开发时使用的参数名映射（Minecraft 反编译后的方法参数名）。
  EN: Parameter name mappings used at development time (Minecraft decompiled method parameter names).

### CC0 1.0 Notice

```
Parchment mappings are released under the terms of the CC0 1.0 Universal (CC0 1.0).

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.
```

---

## Gradle Build Tool

- **Project**: Gradle
- **Author**: Gradle Inc.
- **License**: [Apache License 2.0](https://github.com/gradle/gradle/blob/master/LICENSE)
- **Usage**: 项目构建工具。
  EN: Project build tool.

---

## Notes on License Compatibility

本模组（Datapack Config Mod）的代码采用 **MIT License** 发布。

EN: This mod's own code is released under the **MIT License**.

动态链接至 Cloth Config（LGPL-3.0）不要求本模组整体采用 LGPL-3.0 许可，因为 Cloth Config 的 LGPL-3.0 条款中允许通过"使用该库的正常形式"（即作为独立模块的库引用）来使用该库，而无需将调用方也置于 LGPL-3.0 下。

EN: Dynamic linkage to Cloth Config (LGPL-3.0) does not require this mod to adopt LGPL-3.0, as the LGPL-3.0 permits use of the library through "use of the normal form of the library" (i.e., as a separately compiled library module) without requiring the caller to be licensed under LGPL-3.0.

---

*本文件最后更新 / Last updated: 2026-05-25*
