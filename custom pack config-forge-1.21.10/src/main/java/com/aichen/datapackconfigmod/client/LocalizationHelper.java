package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class LocalizationHelper {
    private static final Map<String, String> FALLBACK_TEXTS = Map.of(
        "color.format_error", "Invalid color format",
        "dropdown.empty_options", "No options available",
        "dropdown.unknown_option", "Unknown option"
    );

    public static Component getDisplayName(String key, DatapackSettings.ConfigMetadata metadata) {
        // 优先使用metadata中的本地化显示名称
        if (metadata != null && metadata.getLocalizedDisplayName() != null && !metadata.getLocalizedDisplayName().isEmpty()) {
            return Component.literal(metadata.getLocalizedDisplayName());
        }
        // 兜底：使用配置键并格式化（将下划线替换为空格）
        return Component.literal(key.replace("_", " "));
    }

    public static Component getOptionText(String baseKey, String option) {
        // 尝试从metadata中获取本地化的选项文本
        String lookupKey = String.format("dropdown.%s.option.%s", baseKey, option);
        String fallbackText = FALLBACK_TEXTS.getOrDefault(lookupKey, option);
        return Component.literal(fallbackText);
    }

    /**
     * 通用tooltip获取方法（消除代码重复）
     * @param key 配置键
     * @param metadata 配置元数据
     * @param fallbackKey 兜底文本的键
     * @param defaultText 默认文本
     * @return 本地化后的tooltip组件
     */
    private static Component getTooltipWithFallback(String key, 
                                          DatapackSettings.ConfigMetadata metadata, 
                                          String fallbackKey, 
                                          String defaultText) {
        if (metadata != null && metadata.getLocalizedTooltip() != null && !metadata.getLocalizedTooltip().isEmpty()) {
            return Component.literal(metadata.getLocalizedTooltip());
        }
        return Component.literal(FALLBACK_TEXTS.getOrDefault(fallbackKey, defaultText));
    }

    /**
     * 获取颜色选择器的tooltip
     * @param key 配置键
     * @param metadata 配置元数据
     * @return 本地化后的tooltip组件
     */
    public static Component getColorTooltip(String key, DatapackSettings.ConfigMetadata metadata) {
        return getTooltipWithFallback(key, metadata, "color.format_error", "Color picker");
    }

    /**
     * 获取下拉菜单的tooltip
     * @param key 配置键
     * @param metadata 配置元数据
     * @return 本地化后的tooltip组件
     */
    public static Component getDropdownTooltip(String key, DatapackSettings.ConfigMetadata metadata) {
        return getTooltipWithFallback(key, metadata, "dropdown.empty_options", "Select an option");
    }
}