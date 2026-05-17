package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.aichen.datapackconfigmod.manager.DatapackSettingsManager;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropdownMenuBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DropdownMenuBuilder.class);
    public static AbstractConfigListEntry<?> build(
        ConfigEntryBuilder entryBuilder,
        String datapackId,
        String key,
        String value,
        DatapackSettings.ConfigMetadata metadata
    ) {
        // 防御性参数验证
        if (entryBuilder == null) {
            throw new IllegalArgumentException("entryBuilder cannot be null");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        if (value == null) {
            value = ""; // 兜底：使用空字符串
        }

        //1. 从metadata获取选项列表（使用enumValues）
        String[] enumValues = metadata != null && metadata.getEnumValues() != null ?
            metadata.getEnumValues() :
            new String[]{value};
        
        if (enumValues.length == 0) {
            // 没有选项，返回文本输入框
            return entryBuilder.startStrField(
                LocalizationHelper.getDisplayName(key, metadata),
                value
            ).build();
        }

        // 2. 本地化处理
        Component displayName = LocalizationHelper.getDisplayName(key, metadata);
        
        // 创建枚举值到显示名称的映射
        java.util.Map<String, String> enumToDisplayMap = new java.util.HashMap<>();
        for (String enumValue : enumValues) {
            enumToDisplayMap.put(enumValue, LocalizationHelper.getOptionText(key, enumValue).getString());
        }

        // 3. 构建选择器（使用startSelector替代startDropdownMenu）
        String defaultValue = enumValues != null && enumValues.length > 0 ? enumValues[0] : value;
        return entryBuilder.startSelector(
            displayName,
            enumValues,
            value
        ).setDefaultValue(defaultValue)
        .setTooltip(LocalizationHelper.getDropdownTooltip(key, metadata))
        .setNameProvider(strValue -> Component.literal(enumToDisplayMap.getOrDefault(strValue, strValue)))
        .setSaveConsumer(newValue -> {
            saveDropdownValue(datapackId, key, newValue);
        })
        .build();
    }

    private static void saveDropdownValue(String datapackId, String key, String value) {
        // 防御性检查：确保必要参数不为null
        if (datapackId == null || datapackId.isEmpty() || key == null || key.isEmpty()) {
            return; // 静默失败，避免崩溃
        }
        if (value == null) {
            value = ""; // 兜底：使用空字符串
        }

        try {
            DatapackSettings settings = DatapackSettingsManager.getSetting(datapackId);
            if (settings != null) {
                settings.addCustomValue(key, value);
                DatapackSettingsManager.addOrUpdateSetting(settings);
                LOGGER.debug("[DropdownMenuBuilder] Saved dropdown value: {} = {}", key, value);
            } else {
                LOGGER.warn("[DropdownMenuBuilder] Unable to find settings for datapack: {}", datapackId);
            }
        } catch (Exception e) {
            LOGGER.error("[DropdownMenuBuilder] Failed to save dropdown value: {} = {}", key, value, e);
        }
    }
}
