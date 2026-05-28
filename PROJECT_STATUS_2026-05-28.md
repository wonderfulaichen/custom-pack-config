# Datapack Config Mod - 项目状态总结
## 2026-05-28 19:32

---

## 1 架构现状

项目目前存在 **两套代码库**：

### ✅ 旧项目（工作版本）
```
📁 数据包配置mod/              ← 稳定可用的 1.20.1 版本
├── src/main/java/             ← 23 个 Java 文件
├── build.gradle               ← Forge 47.4.10 + Gradle 8.8
├── settings.gradle
├── gradlew / gradlew.bat
├── README.md / QUICKSTART.md  ← 文档
├── epicterrain_extracted/     ← EpicTerrain 数据包配置
└── epicterrain_compatible_configured.zip
```
- **状态**: 0.0.2 正式版本，Forge 1.20.1 单版本
- **构建**: 可用 `./gradlew build` 编译
- **文档**: 含中英双语完整使用指南

### 🚧 新项目（MultiLoader 开发中）
```
📁 custom-pack-config/         ← MultiLoader 架构（未提交 git）
├── shared/                    ← 16 个跨版本共享 Java 文件 ✅
├── forge-1.20.1/              ← 7 个版本特有文件   ✅ 编译成功
├── forge-1.21.4/              ← 7 个版本特有文件   ❌ 编译失败
├── .gradle-tools/             ← Gradle 9.5.1 + JDK 25
├── gradle.properties
└── build.gradle
```
- **状态**: 架构骨架已完成，代码已拆分

---

## 2 编译状态

| 子项目 | 结果 | 产物 |
|--------|------|------|
| forge-1.20.1 (Gradle 8.8 + JDK 17) | ✅ 成功 | `datapack_config_mod-0.0.2-1.20.1.jar` |
| forge-1.21.4 (Gradle 9.5.1 + JDK 21) | ❌ 失败 | `build/classes/` 为空，无 jar 输出 |

### ❌ forge-1.21.4 编译阻塞（核心问题）

**根源**: ForgeGradle 7.x (7.0.3) 在 Gradle 9.5.1 上 `minecraft {}` 块的类路径注入机制失效。

**已尝试方案**:
| # | 方案 | 结果 |
|---|------|------|
| 1 | ForgeGradle 默认 minecraft { runs {} } | ❌ compileJava 找不到 net.minecraft.* |
| 2 | 降级 Gradle 8.x | ❌ ForgeGradle 7.x 强制要求 Gradle 9.3+ |
| 3 | 改用 `minecraft.dependency()` 注入 | ❌ 类路径仍失败 |
| 4 | Mavenizer 本地 repo + `implementation` 直接依赖 | ❌ 依赖解析通过但编译时类路径仍无效 |

**推荐解决方向**: 
- 在用户环境中直接运行 `gradlew`（绕过沙箱限制）
- 或用 ForgeGradle 6.x + Gradle 8.x 替代 7.x + 9.x 方案

---

## 3 EpicTerrain 数据包配置（已完成）

| 项目 | 内容 |
|------|------|
| 配置项 | 22 个 |
| 分类 | 8 个（世界尺度/河流动性/海洋深度/山体高度/洞穴高度/群系参数/群系尺度/湖流生成） |
| 特殊功能 | 数字数组索引 key 路径（如 `argument/spline/points/1/location`） |
| 批量修改 | 跨文件同步（如 world_scale_xz 同时修改 c/1.json + c/1a.json） |
| 数据包文件 | `epicterrain_extracted/datapack_config.json` |

---

## 4 待完成清单

### 🔴 高优先级
1. **[编译] 修复 forge-1.21.4 编译** — 核心阻塞，卡住 MultiLoader 交付
2. **[Git] 提交 custom-pack-config/** — 新项目尚未 init git 和提交

### 🟡 中优先级
3. **[文档] 更新 README** — 根目录 README 仍只描述旧版 1.20.1
4. **[清理] 旧项目 src/** — 与 custom-pack-config 并存，考虑归档或清理

### 🟢 低优先级
5. **[扩展] 覆盖 1.21.x 中间版本** — 当前只有 1.20.1 和 1.21.4
6. **[测试] 运行单元测试** — 从未执行过测试
7. **[CI] 配置 GitHub Actions 自动构建**

---

## 5 技术栈记录

### forge-1.20.1
| 项目 | 值 |
|------|-----|
| JDK | 17 |
| Gradle | 8.8 |
| ForgeGradle | 6.x (org.parchmentmc.librarian.forgegradle) |
| Forge | 47.4.10 |
| Mappings | Parchment 2023.09.03-1.20.1 |
| Cloth Config | 11.1.118 |

### forge-1.21.4
| 项目 | 值 |
|------|-----|
| JDK | 21 |
| Gradle | 9.5.1（本地 `.gradle-tools/gradle-9.5.1/`） |
| ForgeGradle | 7.x [7.0.3, 8) |
| Forge | 54.1.14 |
| Mappings | Official 1.21.4 |
| Cloth Config | 17.0.144 |
| JDK 25 路径 | `.gradle-tools/jdk-25.0.4+2`（为 Gradle 9.5.1 toolchain 准备） |

---

*生成于 2026-05-28 19:32，为开启新对话准备。完整内容也写入 `.workbuddy/memory/2026-05-28.md` 及 `custom-pack-config/.workbuddy/memory/MEMORY.md`*
