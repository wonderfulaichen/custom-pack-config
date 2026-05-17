# Custom Pack Config Mod

一个 **Minecraft Forge 1.20.1** 模组，提供**全局数据包自动加载**功能，并支持 **Cloth Config 图形界面配置**。

---

## 功能

### 全局数据包自动加载
- 将数据包放入 `.minecraft/global_datapacks/` 文件夹，游戏启动时**自动强制加载**
- 支持 **文件夹** 和 **.zip 压缩包** 两种格式
- 所有数据包加载顺序设为最高优先级（`TOP`），可覆盖模组和原版数据
- 加载日志清晰输出，方便排查问题

### 图形配置界面
- 集成 Cloth Config 图形界面，在游戏中直接修改配置
- 修改后自动保存到 Forge 配置文件，**重启不丢失**

---

## 安装

1. 确保已安装 **Minecraft Forge 47.4+**（对应 1.20.1）
2. 将模组 `.jar` 文件放入 `.minecraft/mods/` 文件夹
3. （可选）安装 **Cloth Config for Forge 11.x** 以使用图形配置界面

## 使用

### 全局数据包
1. 在 `.minecraft/` 目录下创建 `global_datapacks/` 文件夹（首次运行模组会自动创建）
2. 将你的数据包（文件夹或 .zip）放入该目录
3. **启动游戏**，数据包会自动生效

```
.minecraft/
├── global_datapacks/     ← 把你的数据包放这里
│   ├── my_datapack/      ← 文件夹格式
│   └── another_pack.zip  ← zip 压缩包格式
├── mods/
│   └── custom_pack_config-1.0.0-1.20.1.jar
└── ...
```

### 配置界面
- 在主菜单的 **Mods** 列表中找到 "Custom Pack Config Mod"
- 点击 **Config** 按钮打开配置界面
- 修改配置后点击 **Save** 自动保存

---

## 配置文件

位置：`.minecraft/config/custom_pack_config-common.toml`

```toml
logDirtBlock = true
magicNumber = 42
```

---

## 开发

基于 Minecraft Forge MDK，使用 Gradle 构建。

```bash
./gradlew build
```

编译产物在 `build/libs/` 目录下。

---

## 依赖

| 依赖 | 版本 | 必要性 |
|------|------|--------|
| Minecraft Forge | 47.4+ | 必需 |
| Cloth Config for Forge | 11.x | 可选（推荐） |

---

## 许可

LGPL 2.1 — 与 Minecraft Forge 相同的许可证。详情见 [LICENSE.txt](LICENSE.txt)。
