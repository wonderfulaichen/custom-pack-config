# Datapack Config Mod - 项目状态总结
## 2026-05-28 20:20

---

## 1 架构现状（已清理）

项目现在是 **单一 MultiLoader 代码库**：

```
📁 数据包配置mod/
├── custom-pack-config/         ← MultiLoader 架构（唯一项目）
│   ├── shared/                 ← 16 个跨版本共享 Java 文件
│   ├── forge-1.20.1/           ← 7 个版本特有文件 ✅
│   ├── forge-1.21.4/           ← 7 个版本特有文件 ✅
│   ├── .gradle-tools/          ← Gradle 9.5.1 + JDK 25
│   └── gradle.properties
├── example_datapack/           ← 示例数据包
└── epicterrain_extracted/      ← EpicTerrain 数据包配置
```

---

## 2 编译状态

| 子项目 | 结果 | 产物 |
|--------|------|------|
| forge-1.20.1 (Gradle 8.8 + JDK 17) | ✅ 成功 | `datapack_config_mod-0.0.2-1.20.1.jar` |
| forge-1.21.4 (Gradle 9.5.1 + JDK 21) | ✅ 成功 | `datapack_config_mod-0.0.2-1.21.4.jar` (223KB) |

### ✅ forge-1.21.4 编译修复（2026-05-28 完成）

**根本原因**: ForgeGradle 7.x 与 6.x 构建模式完全不同，需使用官方 MDK 模板配置。

**关键修复**:
- 依赖: `implementation minecraft.dependency(...)` 而非直接 `implementation`
- 仓库: `minecraft.mavenizer(it)` + `fg.forgeMaven` + `fg.minecraftLibsMaven`
- JDK 25: Mavenizer 内部工具链需要，通过 JAVA25_HOME 提供
- API 变更: SubscribeEvent 包路径、getModEventBus()、Screen.hasControlDown()

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

## 4 Git 提交记录

| Commit | 说明 |
|--------|------|
| `b1a7a47` | feat: add custom-pack-config MultiLoader architecture |
| `09a1b97` | fix: forge-1.21.4 compilation - aligned with official MDK template |
| `b69ab78` | chore: cleanup legacy directories and root build files |
| `1c15f52` | docs: update project status |

---

## 5 待完成清单

### 🟡 中优先级
1. **[文档] 更新 README** — 根目录 README 仍只描述旧版 1.20.1

### 🟢 低优先级
2. **[扩展] 覆盖 1.21.x 中间版本** — 当前只有 1.20.1 和 1.21.4
3. **[测试] 运行单元测试** — 从未执行过测试
4. **[CI] 配置 GitHub Actions 自动构建**

---

## 6 技术栈记录

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
| JDK 25 | `.gradle-tools/jdk-25.0.4+2`（Mavenizer 工具链） |

### 构建命令
```bash
# forge-1.20.1
cd custom-pack-config/forge-1.20.1 && ./gradlew build

# forge-1.21.4
cd custom-pack-config/forge-1.21.4
JAVA_HOME="D:/Program Files/Java/zulu21.32.17-ca-jdk21.0.2-win_x64" \
JAVA25_HOME=".../.gradle-tools/jdk-25.0.4+2" \
../.gradle-tools/gradle-9.5.1/bin/gradle build --no-daemon
```

---

*更新于 2026-05-28 20:20，清理旧目录后*
