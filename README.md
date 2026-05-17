# Custom Pack Config Mod

**一次性安装数据包，对所有世界生效。**

Minecraft 原版的数据包只能放在每个世界的 `datapacks/` 文件夹里——换世界就要重新配置，对于整合包作者和服务端管理员来说非常麻烦。

这个模组就是为了解决这个问题：在 `.minecraft/` 下开一个 `global_datapacks/` 文件夹，放进去的数据包**全局永久生效**，不再需要逐个世界配置。

---

## 为什么需要这个模组？

| 场景 | 原版 | 用这个模组 |
|------|------|-----------|
| 安装了地形生成数据包 | 每个新世界都要去 `.minecraft/saves/世界名/datapacks/` 手动放一份 | 放 `global_datapacks/` 一次，所有世界自动加载 |
| 服务端装功能数据包 | 每个存档目录都要操作一次 | `global_datapacks/` 全局共享，一劳永逸 |
| 玩家自己玩 | 创建世界时忘记选数据包就要退出重来 | 强制加载，不必操心 |
| 覆盖原版机制 | 优先级不确定，可能被原版覆盖 | 加载在最高优先级（TOP），稳定覆盖 |

**一句话：把数据包扔进去，就不用管了。**

---

## 功能

### 全局强制加载
- 将数据包放入 `.minecraft/global_datapacks/`，游戏启动时**自动强制启用**
- 支持 **文件夹** 和 **.zip 压缩包** 两种格式
- 所有数据包以 **最高优先级**（`Pack.Position.TOP`）加载，稳定覆盖原版和模组数据
- 加载结果输出到日志，每个数据包成功/失败都清晰可见

### 图形配置界面
- 集成 Cloth Config 图形界面，在 Mods 列表中点击 Config 即可打开
- 修改后自动持久化到 TOML 配置文件，重启不丢失

---

## 安装

1. 确保已安装 **Minecraft Forge 47.4+**（对应 1.20.1）
2. 将模组 `.jar` 放入 `.minecraft/mods/`
3. （可选）安装 **Cloth Config for Forge 11.x** 以使用配置界面

## 使用

### 全局数据包

只需要三步：

```
1. 启动一次游戏（模组会自动创建 global_datapacks/ 文件夹）
2. 把你的数据包拖进去
3. 启动游戏，完成。
```

目录结构示意：

```
.minecraft/
├── global_datapacks/     ★ 放这里，自动生效
│   ├── my_datapack/          ← 文件夹格式
│   └── another_pack.zip      ← zip 压缩包格式
├── mods/
│   └── custom_pack_config-1.0.0-1.20.1.jar
└── ...
```

> 注意：数据包必须是合法的 Minecraft 数据包格式（包含 `pack.mcmeta`），否则会被自动跳过并在日志中给出警告。

---

## 开发

基于 Minecraft Forge MDK，使用 Gradle 构建。

```bash
./gradlew build
```

编译产物在 `build/libs/` 目录下。

## 依赖

| 依赖 | 版本 | 必要性 |
|------|------|--------|
| Minecraft Forge | 47.4+ | 必需 |
| Cloth Config for Forge | 11.x | 可选（推荐） |

## 许可

LGPL 2.1 — 与 Minecraft Forge 相同的许可证。详情见 [LICENSE.txt](LICENSE.txt)。
