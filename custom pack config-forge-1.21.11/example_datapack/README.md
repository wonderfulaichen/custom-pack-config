# 示例数据包

这是数据包配置模组的完整示例数据包，展示了模组支持的所有功能。

## 功能分区

本数据包按功能划分为多个配置区域，每个区域展示特定的配置类型和功能：

### 1. 基础设置 (basic_settings)
- **启用数据包** (boolean): 布尔开关示例
- **难度等级** (enum_selector): 枚举选择器示例

### 2. 数值设置 (numeric_settings)
- **滑块示例** (slider): 整数滑块
- **整数输入** (int): 整数输入框
- **浮点滑块** (double_slider): 浮点滑块
- **浮点输入** (double): 浮点输入框

### 3. 选择设置 (choice_settings)
- **下拉菜单** (enum): 枚举下拉菜单
- **枚举选择器** (enum_selector): 按钮切换
- **天气模式** (enum): 更多枚举示例

### 4. 颜色设置 (color_settings)
- **主题颜色** (color): RGB 颜色选择
- **覆盖颜色** (color): ARGB 颜色选择（含透明度）

### 5. 文本设置 (text_settings)
- **欢迎消息** (string): 文本输入
- **启用功能** (boolean): 开关切换

### 6. 生物生成 (mob_spawning)
- **生物生成率** (slider): 滑块控制生成率
- **最大生物数量** (int): 直接输入数量

### 7. 战斗系统 (combat_system)
- **伤害倍率** (double_slider): 伤害倍率滑块
- **防御系数** (double): 防御系数输入

### 8. 高级设置 (advanced_settings)
- **调试模式** (boolean): 批量修改示例（同时修改多个文件）
- **地形缩放** (double_slider): 嵌套路径示例（修改 JSON 嵌套对象）
- **批量测试** (boolean): 多位置修改示例

## 如何使用

### 方法 1: 文件夹格式

1. 将整个 `example_datapack` 文件夹复制到数据包配置模组的数据包文件夹
   - 默认路径: `config/datapack-config-mod/example_datapack`
2. 启动游戏
3. 打开模组配置界面
4. 在配置界面中查看和修改各项配置

### 方法 2: ZIP 格式

1. 将 `example_datapack` 文件夹打包为 ZIP 文件
2. 重命名为 `example_datapack.zip`
3. 将 ZIP 文件复制到数据包配置模组的数据包文件夹
   - 默认路径: `config/datapack-config-mod/example_datapack.zip`
4. 启动游戏
5. 打开模组配置界面

## 配置文件说明

### datapack_config.json
这是核心配置文件，定义了所有配置项的：
- 配置类型（type）
- 默认值（default）
- 范围限制（min, max, step）
- 显示名称和描述（name, description）
- 提示文本（tooltip）
- 目标文件路径（filePath）
- JSON 键路径（key）
- 分类信息（category）
- 本地化文本（name, description, tooltip, enumDisplayNames）

### data/example/config/*.json
这些是实际的配置文件，存储配置项的实际值：
- `basic_settings.json`: 基础设置配置
- `numeric_settings.json`: 数值设置配置
- `choice_settings.json`: 选择设置配置
- `colors.json`: 颜色设置配置
- `text_settings.json`: 文本设置配置
- `spawning.json`: 生物生成配置
- `combat.json`: 战斗系统配置
- `advanced_settings.json`: 高级设置配置

### data/example/functions/debug.mcfunction
示例函数文件，展示如何修改函数文件。

## 功能特点

### ✅ 完整的配置类型支持
- 10 种配置类型，每种都有详细示例

### ✅ 多语言支持
- 简体中文 (zh_cn)
- 英语 (en_us)

### ✅ 分类组织
- 按功能划分为多个分类
- 每个分类有独立的配置项

### ✅ 本地化文本
- 所有配置项都有完整的本地化文本
- 包括名称、描述、提示信息

### ✅ 高级功能展示
- 批量修改：一个配置项修改多个文件
- 嵌套路径：修改 JSON 嵌套对象
- 多位置修改：同时修改多个位置

## 学习资源

1. **配置类型详解**: 查看 `datapack_config.json` 中的每个配置项定义
2. **实际文件**: 查看 `data/example/config/` 中的实际配置文件
3. **完整指南**: 参考 `模组完整使用指南.md` 了解所有功能
4. **源码示例**: 查看项目中的示例代码

## 注意事项

1. 确保 `datapack_config.json` 格式正确
2. 所有 `filePath` 必须指向实际存在的文件
3. JSON 路径使用 `/` 分隔嵌套层级
4. 布尔值使用字符串 "true" 和 "false"
5. 颜色值使用十六进制格式（#RRGGBB 或 #AARRGGBB）

## 自定义和扩展

你可以基于这个示例数据包：
1. 修改配置项以适应你的需求
2. 添加新的配置类型
3. 调整分类结构
4. 修改本地化文本
5. 添加新的配置文件

## 联系与支持

如有问题或建议，请查看：
- 完整使用指南
- 项目文档
- GitHub Issues

---

**版本**: 0.0.1
**最后更新**: 2026-02-01
