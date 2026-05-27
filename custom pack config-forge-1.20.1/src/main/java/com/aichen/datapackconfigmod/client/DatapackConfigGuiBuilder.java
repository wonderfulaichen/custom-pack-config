package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.aichen.datapackconfigmod.manager.DatapackFileModifier;
import com.aichen.datapackconfigmod.manager.DatapackSettingsManager;
import com.mojang.logging.LogUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import com.aichen.datapackconfigmod.client.EnumSelectorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据包配置GUI构建器
 * 动态构建每个数据包的配置界面
 */
public class DatapackConfigGuiBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构建数据包配置类别（新设计：每个数据包独立分类）
     * @param builder 配置构建器
     * @param entryBuilder 配置项构建器
     */
    public static void buildDatapackConfigCategories(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 开始构建数据包配置分类");

        // 重新扫描数据包以获取当前实际存在的数据包
        List<String> currentDatapacks = scanCurrentDatapacks();
        DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 当前扫描到 {} 个数据包", currentDatapacks.size());

        // 获取所有数据包设置
        Map<String, DatapackSettings> allSettings;
        try {
            allSettings = DatapackSettingsManager.getAllSettings();
        } catch (Exception e) {
            DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 获取数据包设置失败", e);
            ConfigCategory errorCategory = builder.getOrCreateCategory(Component.translatable("config.datapackconfigmod.category.datapacks"));
            errorCategory.addEntry(entryBuilder.startTextDescription(
                Component.literal("§c获取数据包设置时出错")
            ).build());
            return;
        }

        // 过滤掉不存在的数据包设置（只显示当前实际存在的数据包）
        List<DatapackSettings> validSettings = new ArrayList<>();
        for (DatapackSettings settings : allSettings.values()) {
            String datapackId = settings.getDatapackId();
            
            // 只显示当前实际存在的数据包
            if (currentDatapacks.contains(datapackId)) {
                validSettings.add(settings);
                LOGGER.info("[DatapackConfigMod] 显示有效的数据包配置: {}", datapackId);
            } else {
                LOGGER.info("[DatapackConfigMod] 过滤掉不存在的数据包配置显示: {}", datapackId);
            }
        }

        if (validSettings.isEmpty()) {
            ConfigCategory datapacks = builder.getOrCreateCategory(Component.translatable("config.datapackconfigmod.category.datapacks"));
            datapacks.addEntry(entryBuilder.startTextDescription(
                Component.translatable("config.datapackconfigmod.no_datapacks")
            ).build());
            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 没有找到有效的数据包配置");
            return;
        }

        DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 找到 {} 个有效的数据包配置", validSettings.size());

        // 为每个数据包创建独立的配置分类
        for (DatapackSettings settings : validSettings) {
            try {
                buildDatapackCategory(builder, entryBuilder, settings);
            } catch (Exception e) {
                DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 构建数据包 {} 的配置失败", settings.getDatapackId(), e);
            }
        }

        DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 数据包配置分类构建完成");
    }

    /**
     * 扫描当前实际存在的数据包
     * @return 当前存在的数据包ID列表（带命名空间前缀）
     */
    private static List<String> scanCurrentDatapacks() {
        List<String> currentDatapacks = new ArrayList<>();
        
        try {
            // 获取数据包文件夹路径
            java.nio.file.Path datapacksFolder = getDatapacksFolder();
            if (datapacksFolder == null || !java.nio.file.Files.exists(datapacksFolder)) {
                LOGGER.warn("[DatapackConfigMod] 数据包文件夹不存在");
                return currentDatapacks;
            }

            // 扫描文件夹中的所有ZIP文件
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(datapacksFolder)) {
                files.filter(path -> {
                    String fileName = path.getFileName().toString();
                    // 只扫描ZIP文件
                    return fileName.toLowerCase().endsWith(".zip");
                }).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    // 添加命名空间前缀，与数据包扫描时的格式保持一致
                    String datapackId = DataPackConfigMod.MODID + ":" + fileName;
                    currentDatapacks.add(datapackId);
                    LOGGER.debug("[DatapackConfigMod] 扫描到数据包: {} (文件: {})", datapackId, fileName);
                });
            }
        } catch (Exception e) {
            LOGGER.error("[DatapackConfigMod] 扫描数据包失败", e);
        }
        
        return currentDatapacks;
    }

    /**
     * 获取数据包文件夹路径
     */
    private static java.nio.file.Path getDatapacksFolder() {
        // 使用与数据包扫描相同的文件夹路径
        return java.nio.file.Paths.get("config", "datapack-config-mod");
    }

    /**
     * 从数据包ID中提取文件名
     * 例如："datapack_config_mod:example_datapack.zip" -> "example_datapack.zip"
     */
    private static String extractFileName(String datapackId) {
        if (datapackId == null || datapackId.isEmpty()) {
            return datapackId;
        }
        
        // 如果包含冒号，说明有命名空间前缀
        int colonIndex = datapackId.indexOf(':');
        if (colonIndex > 0) {
            return datapackId.substring(colonIndex + 1);
        }
        
        return datapackId;
    }

    /**
     * 构建单个数据包的配置分类（支持嵌套子类别）
     * @param builder 配置构建器
     * @param entryBuilder 配置项构建器
     * @param settings 数据包设置
     */
    private static void buildDatapackCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder, DatapackSettings settings) {
        String datapackId = settings.getDatapackId();
        // 移除 .zip 后缀显示
        String displayName = removeZipExtension(settings.getLocalizedDisplayName());

        // 为每个数据包创建独立的分类
        ConfigCategory datapackCategory = builder.getOrCreateCategory(
            Component.literal(displayName)
        );
        
        // 获取数据包本地化文本（用于分类显示名称等）
        Map<String, String> datapackLocalizedText = settings.getDatapackLocalizedText();

        // 启用/禁用开关（添加数据包ID工具提示）
        final DatapackSettings settingsRef = settings;
        datapackCategory.addEntry(entryBuilder.startBooleanToggle(
            Component.translatable("config.datapackconfigmod.datapack.enabled"),
            settingsRef.isEnabled()
        ).setDefaultValue(true)
        .setTooltip(Component.translatable("config.datapackconfigmod.datapack.enabled.tooltip"))
        .setSaveConsumer(newValue -> {
            // 保存启用状态
            settingsRef.setEnabled(newValue);
            DatapackSettingsManager.addOrUpdateSetting(settingsRef);
            
            // 记录启用状态变更
            if (DataPackConfigMod.LOGGER.isInfoEnabled()) {
                LOGGER.info("[DatapackConfigGuiBuilder] 数据包 {} 启用状态已更新为: {}", 
                    settingsRef.getDatapackId(), newValue);
            }
            
            // 立即应用启用状态变更
            // 对于禁用的数据包，需要从当前世界的数据包列表中移除
            // 对于启用的数据包，需要重新加载数据包
            try {
                // 获取当前世界的数据包管理器
                net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    net.minecraft.server.packs.repository.PackRepository packRepository = server.getPackRepository();
                    
                    if (newValue) {
                        // 启用数据包：重新加载数据包
                        LOGGER.info("[DatapackConfigGuiBuilder] 重新加载启用的数据包: {}", settingsRef.getDatapackId());
                        // 这里需要重新触发数据包加载事件
                    } else {
                        // 禁用数据包：从当前世界的数据包列表中移除
                        LOGGER.info("[DatapackConfigGuiBuilder] 禁用数据包: {}", settingsRef.getDatapackId());
                        // 这里需要从当前世界的数据包列表中移除
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[DatapackConfigGuiBuilder] 应用数据包启用状态变更失败: {}", e.getMessage());
            }
        }).build());

        // 按分类组织配置项（修复：使用配置定义而不是customValues）
        Map<String, DatapackSettings.ConfigMetadata> allMetadatas = settingsRef.getAllConfigMetadata();
        LOGGER.debug("[DatapackConfigGuiBuilder] 配置元数据数量: {}", allMetadatas.size());

        if (!allMetadatas.isEmpty()) {
            // 第一步：按 category 分组配置项
            Map<String, List<ConfigItem>> categorizedItems = new HashMap<>();
            List<ConfigItem> uncategorizedItems = new ArrayList<>();

            // 使用配置定义（metadata）而不是customValues来构建界面
            for (Map.Entry<String, DatapackSettings.ConfigMetadata> metaEntry : allMetadatas.entrySet()) {
                String key = metaEntry.getKey();
                DatapackSettings.ConfigMetadata metadata = metaEntry.getValue();

                // 获取当前值：如果有自定义值就用自定义值，否则用元数据中的默认值
                String currentValue = settingsRef.getCustomValues().getOrDefault(key, 
                    metadata != null ? metadata.getDefaultValue() : "");

                LOGGER.debug("[DatapackConfigGuiBuilder] 处理配置项: key={}, value={}, metadata={}",
                                              key, currentValue, metadata != null ? "有" : "无");

                ConfigItem item = new ConfigItem(key, currentValue, metadata);

                // 检查是否有分类（metadata 可能为 null）
                boolean hasCategory = metadata != null
                    && metadata.getCategory() != null
                    && !metadata.getCategory().isEmpty();

                if (hasCategory) {
                    String category = metadata.getCategory();
                    categorizedItems.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
                } else {
                    uncategorizedItems.add(item);
                }
            }

            LOGGER.debug("[DatapackConfigGuiBuilder] 未分类配置项: {}", uncategorizedItems.size());
            LOGGER.debug("[DatapackConfigGuiBuilder] 已分类配置组: {}", categorizedItems.size());

            // 第二步：添加未分类的配置项（直接添加到主分类）
            for (ConfigItem item : uncategorizedItems) {
                try {
                    LOGGER.debug("[DatapackConfigGuiBuilder] 添加未分类配置: {} = {}", item.key, item.value);
                    Object configEntry = buildConfigEntry(entryBuilder, datapackId, item.key, item.value, item.metadata);
                    if (configEntry != null) {
                        datapackCategory.addEntry((me.shedaniel.clothconfig2.api.AbstractConfigListEntry)configEntry);
                        LOGGER.debug("[DatapackConfigGuiBuilder] 配置项已添加到分类: {}", item.key);
                        
                        // 添加配置项说明（显示在配置项下方）
                        addConfigDescription(entryBuilder, datapackCategory, item.metadata, item.key);
                    } else {
                        LOGGER.warn("[DatapackConfigGuiBuilder] 未分类配置项构建失败: {}，但继续处理其他配置项", item.key);
                    }
                } catch (Exception e) {
                    LOGGER.error("[DatapackConfigGuiBuilder] 构建未分类配置项失败: {}，跳过此配置项", item.key, e);
                    // 添加错误提示配置项
                    datapackCategory.addEntry(entryBuilder.startTextDescription(
                        Component.literal("§c配置项 " + item.key + " 构建失败，请查看游戏日志")
                    ).build());
                }
            }

            // 第三步：添加已分类的配置项（使用子类别实现折叠/展开功能）
            for (Map.Entry<String, List<ConfigItem>> categoryEntry : categorizedItems.entrySet()) {
                String categoryPath = categoryEntry.getKey();
                List<ConfigItem> items = categoryEntry.getValue();

                // 获取本地化的分类显示名称（查找有categoryDisplayNames的配置项）
                String localizedCategoryPath = categoryPath;
                for (ConfigItem item : items) {
                    if (item.metadata != null) {
                        String localized = item.metadata.getLocalizedCategoryDisplayName(datapackLocalizedText);
                        // 如果找到本地化名称（与原始分类不同），使用它
                        if (localized != null && !localized.isEmpty() && !localized.equals(categoryPath)) {
                            localizedCategoryPath = localized;
                            break;
                        }
                    }
                }

                DataPackConfigMod.LOGGER.info("[DatapackConfigGuiBuilder] 创建分类: {}, 包含 {} 个配置项", localizedCategoryPath, items.size());

                // 使用子类别创建可折叠的分类
                SubCategoryBuilder subCategoryBuilder = entryBuilder.startSubCategory(
                    Component.literal("§b" + localizedCategoryPath)
                );

                // 添加配置项到子类别
                for (ConfigItem item : items) {
                    try {
                        DataPackConfigMod.LOGGER.info("[DatapackConfigGuiBuilder] 添加配置: {} = {}", item.key, item.value);
                        Object configEntry = buildConfigEntry(entryBuilder, datapackId, item.key, item.value, item.metadata);
                        if (configEntry != null) {
                            subCategoryBuilder.add((me.shedaniel.clothconfig2.api.AbstractConfigListEntry)configEntry);
                            
                            // 添加配置项说明（显示在配置项下方）
                            addConfigDescriptionToSubCategory(entryBuilder, subCategoryBuilder, item.metadata, item.key);
                        } else {
                            LOGGER.warn("[DatapackConfigGuiBuilder] 配置项构建失败: {}，但继续处理其他配置项", item.key);
                        }
                    } catch (Exception e) {
                        LOGGER.error("[DatapackConfigGuiBuilder] 构建配置项失败: {}，跳过此配置项", item.key, e);
                        // 添加错误提示配置项
                        subCategoryBuilder.add(entryBuilder.startTextDescription(
                            Component.literal("§c配置项 " + item.key + " 构建失败，请查看游戏日志")
                        ).build());
                    }
                }

                // 将子类别添加到主分类
                datapackCategory.addEntry(subCategoryBuilder.build());
                DataPackConfigMod.LOGGER.info("[DatapackConfigGuiBuilder] 子类别已添加: {}", categoryPath);
            }
        } else {
            LOGGER.debug("[DatapackConfigGuiBuilder] 警告: 没有配置元数据！数据包 {} 将无法配置", datapackId);
        }
    }

    /**
     * 移除 .zip 后缀
     */
    private static String removeZipExtension(String name) {
        if (name != null && name.toLowerCase().endsWith(".zip")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    /**
     * 配置项包装类（新增：用于临时存储配置项信息）
     */
    private static class ConfigItem {
        String key;
        String value;
        DatapackSettings.ConfigMetadata metadata;

        ConfigItem(String key, String value, DatapackSettings.ConfigMetadata metadata) {
            this.key = key;
            this.value = value;
            this.metadata = metadata;
        }
    }

    /**
     * 创建嵌套子类别（新增：支持多级路径如 "game_settings/difficulty_settings"）
     * @param entryBuilder 配置项构建器
     * @param categoryPath 分类路径（支持多级，用 / 分隔）
     * @param metadata 配置元数据
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 子类别构建器
     */
    private static SubCategoryBuilder createNestedSubCategory(ConfigEntryBuilder entryBuilder, String categoryPath, DatapackSettings.ConfigMetadata metadata, Map<String, String> datapackLocalizedText) {
        String[] pathParts = categoryPath.split("/");
        
        // Cloth Config 的 SubCategoryBuilder 不支持真正的嵌套子类别
        // 所以我们使用命名约定来模拟多级结构：将完整路径用箭头连接显示
        if (pathParts.length > 1) {
            // 多级路径，用箭头连接显示（如：游戏设置 → 难度设置）
            // 使用本地化方法获取每个部分的名称
            String localizedPath = metadata != null ? metadata.getLocalizedCategoryDisplayName(datapackLocalizedText) : categoryPath;
            if (localizedPath == null || localizedPath.isEmpty()) {
                localizedPath = categoryPath;
            }
            String fullPath = String.join(" → ", localizedPath.split("/"));
            return entryBuilder.startSubCategory(Component.literal(fullPath));
        } else {
            // 单级路径，直接显示
            String localizedPath = metadata != null ? metadata.getLocalizedCategoryDisplayName(datapackLocalizedText) : categoryPath;
            if (localizedPath == null || localizedPath.isEmpty()) {
                localizedPath = categoryPath;
            }
            return entryBuilder.startSubCategory(Component.literal(localizedPath));
        }
    }

    /**
     * 构建单个配置项（修改：返回配置项构建器，不直接添加到类别）
     * @param entryBuilder 配置项构建器
     * @param datapackId 数据包ID
     * @param key 配置键
     * @param value 配置值
     * @param metadata 配置元数据
     * @return 配置项构建器，如果失败返回 null
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildConfigEntry(ConfigEntryBuilder entryBuilder,
                                       String datapackId, String key, String value,
                                       DatapackSettings.ConfigMetadata metadata) {
        LOGGER.debug("[DatapackConfigGuiBuilder] 构建配置项: datapackId={}, key={}, value={}, metadata={}",
                                      datapackId, key, value, metadata != null ? "有" : "无");

        // 确定配置类型
        String type = metadata != null ? metadata.getType() : inferType(value);
        LOGGER.debug("[DatapackConfigGuiBuilder] 配置类型: {}", type);

        me.shedaniel.clothconfig2.api.AbstractConfigListEntry entry = null;

        // 使用传统的 switch 语句，兼容 Java 17
        switch (type) {
            case "slider":
                entry = buildSliderEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "double_slider":
                entry = buildDoubleSliderEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "enum":
                entry = buildEnumEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "enum_selector":
                entry = buildEnumSelectorEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "boolean":
                entry = buildBooleanEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "int":
                entry = buildIntEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "double":
                entry = buildDoubleEntry(entryBuilder, datapackId, key, value, metadata);
                break;
            case "color":
                try {
                    LOGGER.debug("[DatapackConfigGuiBuilder] 开始构建颜色配置项: {} (值: {})", key, value);
                    entry = ColorPickerBuilder.build(entryBuilder, datapackId, key, value, metadata);
                    LOGGER.debug("[DatapackConfigGuiBuilder] 颜色配置项构建成功: {}", key);
                } catch (Exception e) {
                    LOGGER.error("[DatapackConfigGuiBuilder] 构建颜色配置项失败: {} (值: {}), 错误: {}\n使用字符串输入框替代", key, value, e.getMessage());
                    LOGGER.debug("[DatapackConfigGuiBuilder] 详细错误信息:", e);
                    entry = buildStringEntry(entryBuilder, datapackId, key, value, metadata);
                }
                break;
            case "dropdown_menu":
                // 如果DropdownMenuBuilder不存在，使用枚举下拉菜单替代
                try {
                    entry = buildEnumEntry(entryBuilder, datapackId, key, value, metadata);
                } catch (Exception e) {
                    LOGGER.error("[DatapackConfigGuiBuilder] 构建下拉菜单配置项失败: {}, 使用字符串输入框替代", key, e);
                    entry = buildStringEntry(entryBuilder, datapackId, key, value, metadata);
                }
                break;
            case "string":
            default:
                entry = buildStringEntry(entryBuilder, datapackId, key, value, metadata);
                break;
        }

        LOGGER.debug("[DatapackConfigGuiBuilder] 配置项构建结果: {}", entry != null ? "成功" : "失败");
        return entry;
    }

    /**
     * 构建滑块配置项（修复：使用startIntSlider创建真正的滑块）
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildSliderEntry(ConfigEntryBuilder entryBuilder,
                                        String datapackId, String key, String value,
                                        DatapackSettings.ConfigMetadata metadata) {
        double min = metadata != null ? metadata.getMinValue() : 0.0;
        double max = metadata != null ? metadata.getMaxValue() : 100.0;
        double step = metadata != null ? metadata.getStep() : 1.0;

        // 确保 min <= max
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        int intValue;
        int intMin = (int) min;
        int intMax = (int) max;

        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            intValue = (intMin + intMax) / 2;
        }

        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 修复：使用startIntSlider创建滑块（优化：在闭包外获取设置引用）
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String rangeText = metadata != null ? metadata.getLocalizedRange(intMin, intMax, datapackLocalizedText) : ("Range: " + intMin + " - " + intMax);
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7" + rangeText;
        } else {
            tooltipText = new StringBuilder(tooltipText).append("\n§7").append(rangeText).toString();
        }
        
        // 添加批量修改位置信息（如果有多个位置）
        tooltipText = buildBatchModificationTooltip(currentSettings, key, tooltipText);

        return entryBuilder.startIntSlider(
            Component.literal(displayName),
            intValue,
            intMin,
            intMax
        ).setDefaultValue((intMin + intMax) / 2)
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newValue -> {
            currentSettings.addCustomValue(key, String.valueOf(newValue));
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 获取枚举值的本地化显示名称列表
     * @param metadata 配置元数据
     * @param enumValues 枚举值数组
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 本地化后的显示名称列表
     */
    private static java.util.List<String> getLocalizedEnumDisplayNames(DatapackSettings.ConfigMetadata metadata, 
                                                                       String[] enumValues, 
                                                                       Map<String, String> datapackLocalizedText) {
        if (enumValues == null) {
            return java.util.Collections.emptyList();
        }
        
        // 获取当前游戏语言
        String currentLangCode = "en_us"; // 默认英语
        try {
            currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        
        java.util.List<String> displayNames = new java.util.ArrayList<>();
        for (String enumValue : enumValues) {
            // 尝试从数据包本地化文本获取显示名称
            String displayName = null;
            if (datapackLocalizedText != null) {
                displayName = datapackLocalizedText.get("enum_display_" + currentLangCode + "_" + enumValue);
                if (displayName == null || displayName.isEmpty()) {
                    displayName = datapackLocalizedText.get("enum_display_en_us_" + enumValue);
                }
            }
            
            // 如果没有数据包级别的本地化，使用配置项级别的本地化
            if (displayName == null || displayName.isEmpty()) {
                displayName = metadata.getLocalizedText().get("enum_display_" + currentLangCode + "_" + enumValue);
                if (displayName == null || displayName.isEmpty()) {
                    displayName = metadata.getLocalizedText().get("enum_display_en_us_" + enumValue);
                }
            }
            
            // 如果没有找到本地化显示名称，使用枚举值本身
            if (displayName == null || displayName.isEmpty()) {
                displayName = enumValue;
            }
            
            displayNames.add(displayName);
        }
        
        return displayNames;
    }

    /**
     * 构建枚举选择器配置项（修复：使用startStringDropdownMenu创建下拉菜单）
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildEnumEntry(ConfigEntryBuilder entryBuilder,
                                      String datapackId, String key, String value,
                                      DatapackSettings.ConfigMetadata metadata) {
        if (metadata == null || metadata.getEnumValues() == null) {
            return buildStringEntry(entryBuilder, datapackId, key, value, metadata);
        }

        String[] enumValues = metadata.getEnumValues();
        int currentIndex = 0;
        for (int i = 0; i < enumValues.length; i++) {
            if (enumValues[i].equals(value)) {
                currentIndex = i;
                break;
            }
        }

        // 修复：使用startStringDropdownMenu创建下拉菜单
        // 使用本地化的显示名称作为选项显示，但保存时使用原始枚举值

        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 修复：使用startStringDropdownMenu创建下拉菜单（优化：在闭包外获取设置引用）
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取本地化的显示名称列表
        java.util.List<String> localizedDisplayNames = getLocalizedEnumDisplayNames(metadata, enumValues, datapackLocalizedText);
        
        // 创建枚举值到显示名称的映射和反向映射
        java.util.Map<String, String> enumToDisplayMap = new java.util.HashMap<>();
        java.util.Map<String, String> displayToEnumMap = new java.util.HashMap<>();
        for (int i = 0; i < enumValues.length; i++) {
            String displayName_i = i < localizedDisplayNames.size() ? localizedDisplayNames.get(i) : enumValues[i];
            enumToDisplayMap.put(enumValues[i], displayName_i);
            displayToEnumMap.put(displayName_i, enumValues[i]);
        }
        
        // 获取当前值的显示名称
        String currentDisplayValue = enumToDisplayMap.getOrDefault(value, value);
        
        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String optionsText = metadata != null ? getLocalizedOptionsText(metadata, enumValues, datapackLocalizedText) : ("Options: " + String.join(", ", enumValues));
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7" + optionsText;
        } else {
            tooltipText += "\n§7" + optionsText;
        }
        
        // 添加批量修改位置信息（如果有多个位置）
        tooltipText = buildBatchModificationTooltip(currentSettings, key, tooltipText);

        return entryBuilder.startStringDropdownMenu(
            Component.literal(displayName),
            currentDisplayValue
        ).setDefaultValue(localizedDisplayNames.get(0))
        .setSelections(localizedDisplayNames)
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newDisplayValue -> {
            // 将显示名称转换回原始枚举值保存
            String originalValue = displayToEnumMap.getOrDefault(newDisplayValue, newDisplayValue);
            currentSettings.addCustomValue(key, originalValue);
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 构建枚举选择器配置项（真正的点击切换按钮模式）
     * 使用Cloth Config的SelectorBuilder实现循环切换按钮
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildEnumSelectorEntry(ConfigEntryBuilder entryBuilder,
                                      String datapackId, String key, String value,
                                      DatapackSettings.ConfigMetadata metadata) {
        if (metadata == null || metadata.getEnumValues() == null) {
            return buildStringEntry(entryBuilder, datapackId, key, value, metadata);
        }

        String[] enumValues = metadata.getEnumValues();
        
        // 获取本地化显示名称和描述
        String displayName = key;
        String description = "";

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 获取设置引用
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取本地化的显示名称列表
        java.util.List<String> localizedDisplayNames = getLocalizedEnumDisplayNames(metadata, enumValues, datapackLocalizedText);
        
        // 创建枚举值到显示名称的映射
        java.util.Map<String, String> enumToDisplayMap = new java.util.HashMap<>();
        for (int i = 0; i < enumValues.length; i++) {
            String displayName_i = i < localizedDisplayNames.size() ? localizedDisplayNames.get(i) : enumValues[i];
            enumToDisplayMap.put(enumValues[i], displayName_i);
        }
        
        // 获取tooltip文本
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String clickToToggleText = metadata != null ? getLocalizedClickToToggleText(metadata, datapackLocalizedText) : "Click button to toggle options";
        String currentSupportText = metadata != null ? getLocalizedCurrentSupportText(metadata, datapackLocalizedText) : "Currently supported";
        // 使用本地化名称显示选项
        String optionsDisplayText = metadata != null ? getLocalizedOptionsText(metadata, enumValues, datapackLocalizedText) : ("Options: " + String.join(" → ", localizedDisplayNames));
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§a" + clickToToggleText + " §7- " + currentSupportText + ": " + String.join(" → ", localizedDisplayNames);
        } else {
            tooltipText += "\n§7" + optionsDisplayText;
        }
        
        // 添加批量修改位置信息
        tooltipText = buildBatchModificationTooltip(currentSettings, key, tooltipText);

        // 使用startSelector创建真正的点击切换按钮
        return entryBuilder.startSelector(
            Component.literal(displayName),
            enumValues,
            value
        ).setDefaultValue(enumValues[0])
        .setTooltip(Component.literal(tooltipText))
        .setNameProvider(strValue -> Component.literal(enumToDisplayMap.getOrDefault(strValue, strValue)))
        .setSaveConsumer(newValue -> {
            currentSettings.addCustomValue(key, newValue);
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 构建布尔开关配置项
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildBooleanEntry(ConfigEntryBuilder entryBuilder,
                                         String datapackId, String key, String value,
                                         DatapackSettings.ConfigMetadata metadata) {
        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 优化：在闭包外获取设置引用
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String toggleStateText = metadata != null ? getLocalizedToggleStateText(metadata, datapackLocalizedText) : "Click to toggle switch state";
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7" + toggleStateText;
        }
        
        // 添加批量修改位置信息（如果有多个位置）
        tooltipText = buildBatchModificationTooltip(currentSettings, key, tooltipText);

        return entryBuilder.startBooleanToggle(
            Component.literal(displayName),
            Boolean.parseBoolean(value)
        ).setDefaultValue(false)
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newValue -> {
            currentSettings.addCustomValue(key, String.valueOf(newValue));
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 构建整数输入框配置项
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildIntEntry(ConfigEntryBuilder entryBuilder,
                                     String datapackId, String key, String value,
                                     DatapackSettings.ConfigMetadata metadata) {
        int min = (int) (metadata != null ? metadata.getMinValue() : 0);
        int max = (int) (metadata != null ? metadata.getMaxValue() : 1000);

        // 确保 min <= max
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        int intValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            intValue = (min + max) / 2;
        }

        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 优化：在闭包外获取设置引用
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String rangeText = metadata != null ? metadata.getLocalizedRange(min, max, datapackLocalizedText) : ("Range: " + min + " - " + max);
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7" + rangeText;
        } else {
            tooltipText = new StringBuilder(tooltipText).append("\n§7").append(rangeText).toString();
        }
        
        // 添加批量修改位置信息（如果有多个位置）
        tooltipText = buildBatchModificationTooltip(currentSettings, key, tooltipText);

        return entryBuilder.startIntField(
            Component.literal(displayName),
            intValue
        ).setDefaultValue(0)
        .setMin(min).setMax(max)
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newValue -> {
            currentSettings.addCustomValue(key, String.valueOf(newValue));
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 构建浮点数输入框配置项
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildDoubleEntry(ConfigEntryBuilder entryBuilder,
                                        String datapackId, String key, String value,
                                        DatapackSettings.ConfigMetadata metadata) {
        double min = metadata != null ? metadata.getMinValue() : 0.0;
        double max = metadata != null ? metadata.getMaxValue() : 1000.0;

        // 确保 min <= max
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        double doubleValue;
        try {
            doubleValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            doubleValue = (min + max) / 2;
        }

        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 优化：在闭包外获取设置引用
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String rangeText = metadata != null ? metadata.getLocalizedRange(min, max, datapackLocalizedText) : ("Range: " + min + " - " + max);
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7" + rangeText;
        } else {
            tooltipText = new StringBuilder(tooltipText).append("\n§7").append(rangeText).toString();
        }

        return entryBuilder.startDoubleField(
            Component.literal(displayName),
            doubleValue
        ).setDefaultValue(0.0)
        .setMin(min).setMax(max)
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newValue -> {
            currentSettings.addCustomValue(key, String.valueOf(newValue));
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 构建浮点数滑块配置项（自定义实现）
     * 由于Cloth Config API没有直接提供浮点数滑块，我们通过输入框+滑块逻辑组合实现
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildDoubleSliderEntry(ConfigEntryBuilder entryBuilder,
                                             String datapackId, String key, String value,
                                             DatapackSettings.ConfigMetadata metadata) {
        double min = metadata != null ? metadata.getMinValue() : 0.0;
        double max = metadata != null ? metadata.getMaxValue() : 100.0;
        double step = metadata != null ? metadata.getStep() : 0.1;

        // 确保 min <= max
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        // 确保step大于0
        if (step <= 0) {
            step = 0.1;
        }

        double doubleValue;
        try {
            doubleValue = Double.parseDouble(value);
            // 确保值在范围内
            if (doubleValue < min) doubleValue = min;
            if (doubleValue > max) doubleValue = max;
        } catch (NumberFormatException e) {
            doubleValue = (min + max) / 2;
        }

        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 优化：在闭包外获取设置引用
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取数据包本地化文本
        Map<String, String> datapackLocalizedText = currentSettings != null ? currentSettings.getDatapackLocalizedText() : null;
        
        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        String sliderTooltip = metadata != null ? metadata.getLocalizedSliderTooltip(min, max, step, datapackLocalizedText) : ("Drag slider to adjust value, Range: " + min + " - " + max + ", Step: " + step);
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7" + sliderTooltip;
        } else {
            String rangeText = metadata != null ? metadata.getLocalizedRange(min, max, datapackLocalizedText) : ("Range: " + min + " - " + max);
            String stepText = metadata != null ? getLocalizedStepText(metadata, step, datapackLocalizedText) : ("Step: " + step);
            tooltipText = new StringBuilder(tooltipText).append("\n§7").append(rangeText).append(", ").append(stepText).toString();
        }
        
        // 添加批量修改位置信息（如果有多个位置）
        tooltipText = buildBatchModificationTooltip(currentSettings, key, tooltipText);

        // 使用startIntSlider创建整数滑块，然后转换为浮点数显示
        // 计算滑块的范围和精度
        int sliderMin = 0;
        int sliderMax = (int) ((max - min) / step);
        int sliderValue = (int) ((doubleValue - min) / step);
        
        // 确保滑块范围合理
        if (sliderMax <= 0) sliderMax = 100;
        if (sliderValue < sliderMin) sliderValue = sliderMin;
        if (sliderValue > sliderMax) sliderValue = sliderMax;
        
        // 创建final变量供lambda表达式使用
        final double finalMin = min;
        final double finalMax = max;
        final double finalStep = step;
        
        return entryBuilder.startIntSlider(
            Component.literal(displayName),
            sliderValue,
            sliderMin,
            sliderMax
        ).setDefaultValue((sliderMin + sliderMax) / 2)
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newSliderValue -> {
            // 将滑块值转换为实际的浮点数值
            double actualValue = finalMin + (newSliderValue * finalStep);
            // 确保值在范围内
            if (actualValue < finalMin) actualValue = finalMin;
            if (actualValue > finalMax) actualValue = finalMax;
            
            currentSettings.addCustomValue(key, String.valueOf(actualValue));
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        })
        .setTextGetter(sliderPos -> {
            // 显示实际的浮点数值而不是滑块值
            double displayValue = finalMin + (sliderPos * finalStep);
            return Component.literal(String.format("%.2f", displayValue));
        })
        .build();
    }

    /**
     * 构建文本输入框配置项
     * 修改：返回配置项构建器，不直接添加到类别
     */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry buildStringEntry(ConfigEntryBuilder entryBuilder,
                                        String datapackId, String key, String value,
                                        DatapackSettings.ConfigMetadata metadata) {
        // 获取本地化显示名称和描述（修复：处理 null 值）
        String displayName = key;  // 默认使用配置键
        String description = "";   // 默认空描述

        if (metadata != null) {
            String localizedDisplay = metadata.getLocalizedDisplayName();
            if (localizedDisplay != null && !localizedDisplay.isEmpty()) {
                displayName = localizedDisplay;
            }

            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null) {
                description = localizedDesc;
            }
        }

        // 优化：在闭包外获取设置引用
        final DatapackSettings currentSettings = DatapackSettingsManager.getSetting(datapackId);
        if (currentSettings == null) {
            return entryBuilder.startTextDescription(Component.literal("§c配置加载失败")).build();
        }

        // 获取tooltip文本（鼠标悬停提示）
        String tooltipText = metadata != null ? metadata.getLocalizedTooltip() : "";
        if (tooltipText == null || tooltipText.isEmpty()) {
            tooltipText = "§7输入文本值";
        }

        return entryBuilder.startStrField(
            Component.literal(displayName),
            value
        ).setDefaultValue("")
        .setTooltip(Component.literal(tooltipText))
        .setSaveConsumer(newValue -> {
            currentSettings.addCustomValue(key, newValue);
            DatapackSettingsManager.addOrUpdateSetting(currentSettings);
        }).build();
    }

    /**
     * 应用配置到数据包文件（新增）
     * @param datapackId 数据包ID
     */
    public static void applyConfigToDatapack(String datapackId) {
        LOGGER.debug("[DatapackConfigGuiBuilder] >>>>> 开始 applyConfigToDatapack >>>>>");
        LOGGER.debug("[DatapackConfigGuiBuilder] 数据包ID: {}", datapackId);

        try {
            DataPackConfigMod.LOGGER.info("[DatapackConfigGuiBuilder] 开始应用配置到数据包: {}", datapackId);
            java.io.File datapackFolder = DatapackFileModifier.getDatapackFolder(datapackId);
            if (datapackFolder != null) {
                LOGGER.debug("[DatapackConfigGuiBuilder] 找到数据包文件: {}", datapackFolder.getAbsolutePath());
                LOGGER.debug("[DatapackConfigGuiBuilder] 是否为ZIP: {}", datapackFolder.getName().endsWith(".zip"));
                LOGGER.debug("[DatapackConfigGuiBuilder] 调用 DatapackFileModifier.applyDatapackConfig");
                DatapackFileModifier.applyDatapackConfig(datapackId, datapackFolder);
                LOGGER.debug("[DatapackConfigGuiBuilder] DatapackFileModifier.applyDatapackConfig 返回");
                DataPackConfigMod.LOGGER.info("[DatapackConfigGuiBuilder] 配置应用完成");
            } else {
                DataPackConfigMod.LOGGER.error("[DatapackConfigGuiBuilder] 未找到数据包文件: {}", datapackId);
            }
        } catch (Exception e) {
            DataPackConfigMod.LOGGER.error("[DatapackConfigGuiBuilder] 应用配置到数据包失败: {}", datapackId, e);
        }

        LOGGER.debug("[DatapackConfigGuiBuilder] <<<<< 结束 applyConfigToDatapack <<<<<");
    }

    /**
     * 生成批量修改位置的工具提示文本
     * @param settings 数据包设置
     * @param key 配置键
     * @param baseTooltip 基础工具提示文本
     * @return 增强后的工具提示文本
     */
    private static String buildBatchModificationTooltip(DatapackSettings settings, String key, String baseTooltip) {
        String tooltipText = baseTooltip;
        
        // 获取配置位置列表
        List<DatapackSettings.ConfigLocation> locations = settings.getConfigLocations(key);
        
        // 如果有多个位置，添加批量修改信息
        if (locations.size() > 1) {
            // 简化提示：只显示位置数量，避免tooltip过长
            tooltipText += "\n\n§e批量修改位置 (" + locations.size() + "个)";
            
            // 由于Cloth Config的tooltip是静态的，无法实时检测按键
            // 这里提供一个通用的提示信息
            tooltipText += "\n§7（详细信息可在配置文件中查看）";
        }
        
        return tooltipText;
    }
    
    /**
     * 获取本地化的选项文本
     * @param metadata 配置元数据
     * @param enumValues 枚举值数组
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 本地化后的选项文本
     */
    private static String getLocalizedOptionsText(DatapackSettings.ConfigMetadata metadata, String[] enumValues, Map<String, String> datapackLocalizedText) {
        if (metadata == null) {
            return "Options: " + String.join(", ", enumValues);
        }
        
        // 获取当前游戏语言
        String currentLangCode = "en_us"; // 默认英语
        try {
            currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        
        // 尝试从数据包本地化文本获取选项前缀
        String optionsPrefix = null;
        if (datapackLocalizedText != null) {
            optionsPrefix = datapackLocalizedText.get("options_" + currentLangCode);
            if (optionsPrefix == null || optionsPrefix.isEmpty()) {
                optionsPrefix = datapackLocalizedText.get("options_en_us");
            }
        }
        
        // 如果没有数据包级别的本地化，使用配置项级别的本地化
        if (optionsPrefix == null || optionsPrefix.isEmpty()) {
            optionsPrefix = metadata.getLocalizedText().get("options_" + currentLangCode);
            if (optionsPrefix == null || optionsPrefix.isEmpty()) {
                optionsPrefix = metadata.getLocalizedText().get("options_en_us");
            }
        }
        
        if (optionsPrefix == null || optionsPrefix.isEmpty()) {
            // 默认文本
            if (currentLangCode.startsWith("zh")) {
                optionsPrefix = "选项";
            } else {
                optionsPrefix = "Options";
            }
        }
        
        return optionsPrefix + ": " + String.join(", ", enumValues);
    }
    
    /**
     * 获取本地化的"点击切换"文本
     * @param metadata 配置元数据
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 本地化后的文本
     */
    private static String getLocalizedClickToToggleText(DatapackSettings.ConfigMetadata metadata, Map<String, String> datapackLocalizedText) {
        if (metadata == null) {
            return "Click button to toggle options";
        }
        
        // 获取当前游戏语言
        String currentLangCode = "en_us"; // 默认英语
        try {
            currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        
        // 尝试从数据包本地化文本获取
        String clickText = null;
        if (datapackLocalizedText != null) {
            clickText = datapackLocalizedText.get("click_to_toggle_" + currentLangCode);
            if (clickText == null || clickText.isEmpty()) {
                clickText = datapackLocalizedText.get("click_to_toggle_en_us");
            }
        }
        
        // 如果没有数据包级别的本地化，使用配置项级别的本地化
        if (clickText == null || clickText.isEmpty()) {
            clickText = metadata.getLocalizedText().get("click_to_toggle_" + currentLangCode);
            if (clickText == null || clickText.isEmpty()) {
                clickText = metadata.getLocalizedText().get("click_to_toggle_en_us");
            }
        }
        
        if (clickText == null || clickText.isEmpty()) {
            // 默认文本
            if (currentLangCode.startsWith("zh")) {
                clickText = "点击按钮切换选项";
            } else {
                clickText = "Click button to toggle options";
            }
        }
        
        return clickText;
    }
    
    /**
     * 获取本地化的"当前支持"文本
     * @param metadata 配置元数据
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 本地化后的文本
     */
    private static String getLocalizedCurrentSupportText(DatapackSettings.ConfigMetadata metadata, Map<String, String> datapackLocalizedText) {
        if (metadata == null) {
            return "Currently supported";
        }
        
        // 获取当前游戏语言
        String currentLangCode = "en_us"; // 默认英语
        try {
            currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        
        // 尝试从数据包本地化文本获取
        String supportText = null;
        if (datapackLocalizedText != null) {
            supportText = datapackLocalizedText.get("current_support_" + currentLangCode);
            if (supportText == null || supportText.isEmpty()) {
                supportText = datapackLocalizedText.get("current_support_en_us");
            }
        }
        
        // 如果没有数据包级别的本地化，使用配置项级别的本地化
        if (supportText == null || supportText.isEmpty()) {
            supportText = metadata.getLocalizedText().get("current_support_" + currentLangCode);
            if (supportText == null || supportText.isEmpty()) {
                supportText = metadata.getLocalizedText().get("current_support_en_us");
            }
        }
        
        if (supportText == null || supportText.isEmpty()) {
            // 默认文本
            if (currentLangCode.startsWith("zh")) {
                supportText = "当前支持";
            } else {
                supportText = "Currently supported";
            }
        }
        
        return supportText;
    }
    
    /**
     * 获取本地化的"切换开关状态"文本
     * @param metadata 配置元数据
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 本地化后的文本
     */
    private static String getLocalizedToggleStateText(DatapackSettings.ConfigMetadata metadata, Map<String, String> datapackLocalizedText) {
        if (metadata == null) {
            return "Click to toggle switch state";
        }
        
        // 获取当前游戏语言
        String currentLangCode = "en_us"; // 默认英语
        try {
            currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        
        // 尝试从数据包本地化文本获取
        String toggleText = null;
        if (datapackLocalizedText != null) {
            toggleText = datapackLocalizedText.get("toggle_state_" + currentLangCode);
            if (toggleText == null || toggleText.isEmpty()) {
                toggleText = datapackLocalizedText.get("toggle_state_en_us");
            }
        }
        
        // 如果没有数据包级别的本地化，使用配置项级别的本地化
        if (toggleText == null || toggleText.isEmpty()) {
            toggleText = metadata.getLocalizedText().get("toggle_state_" + currentLangCode);
            if (toggleText == null || toggleText.isEmpty()) {
                toggleText = metadata.getLocalizedText().get("toggle_state_en_us");
            }
        }
        
        if (toggleText == null || toggleText.isEmpty()) {
            // 默认文本
            if (currentLangCode.startsWith("zh")) {
                toggleText = "点击切换开关状态";
            } else {
                toggleText = "Click to toggle switch state";
            }
        }
        
        return toggleText;
    }
    
    /**
     * 获取本地化的步长文本
     * @param metadata 配置元数据
     * @param step 步长值
     * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
     * @return 本地化后的步长文本
     */
    private static String getLocalizedStepText(DatapackSettings.ConfigMetadata metadata, double step, Map<String, String> datapackLocalizedText) {
        if (metadata == null) {
            return "Step: " + step;
        }
        
        return metadata.getLocalizedStepText(step, datapackLocalizedText);
    }

    /**
     * 添加配置项说明到主分类（显示在配置项下方）
     * @param entryBuilder 配置项构建器
     * @param category 主分类
     * @param metadata 配置元数据
     * @param key 配置键（用于日志）
     */
    private static void addConfigDescription(ConfigEntryBuilder entryBuilder, ConfigCategory category,
                                             DatapackSettings.ConfigMetadata metadata, String key) {
        if (metadata != null) {
            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null && !localizedDesc.isEmpty()) {
                // 添加说明文本，使用灰色小字体显示
                category.addEntry(entryBuilder.startTextDescription(
                    Component.literal("§7§o" + localizedDesc)
                ).build());
                LOGGER.debug("[DatapackConfigGuiBuilder] 配置项 {} 添加说明: {}", key, localizedDesc);
            }
        }
    }

    /**
     * 添加配置项说明到子分类（显示在配置项下方）
     * @param entryBuilder 配置项构建器
     * @param subCategoryBuilder 子分类构建器
     * @param metadata 配置元数据
     * @param key 配置键（用于日志）
     */
    private static void addConfigDescriptionToSubCategory(ConfigEntryBuilder entryBuilder, SubCategoryBuilder subCategoryBuilder,
                                                         DatapackSettings.ConfigMetadata metadata, String key) {
        if (metadata != null) {
            String localizedDesc = metadata.getLocalizedDescription();
            if (localizedDesc != null && !localizedDesc.isEmpty()) {
                // 添加说明文本，使用灰色小字体显示
                subCategoryBuilder.add(entryBuilder.startTextDescription(
                    Component.literal("§7§o" + localizedDesc)
                ).build());
                LOGGER.debug("[DatapackConfigGuiBuilder] 配置项 {} 添加说明到子分类: {}", key, localizedDesc);
            }
        }
    }

    /**
     * 自动推断配置类型
     */
    private static String inferType(String value) {
        if (isBoolean(value)) {
            return "boolean";
        } else if (isInteger(value)) {
            return "int";
        } else if (isDouble(value)) {
            return "double";
        }
        return "string";
    }

    /**
     * 检查是否为布尔值
     */
    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    /**
     * 检查是否为整数
     */
    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查是否为浮点数
     */
    private static boolean isDouble(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            // 确保包含小数点，以区分整数和浮点数
            return value.contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
