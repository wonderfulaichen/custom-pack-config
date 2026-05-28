package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.aichen.datapackconfigmod.manager.DatapackSettingsManager;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ColorPickerBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorPickerBuilder.class);
    
    // 颜色验证常量 - 根据 Cloth Config 实际行为调整
    // Cloth Config 在 alpha=false 时显示 6 位，alpha=true 时显示 8 位
    private static final String COLOR_PATTERN_RGB = "^#[0-9A-Fa-f]{6}$";
    private static final String COLOR_PATTERN_ARGB = "^#[0-9A-Fa-f]{8}$";
    
    // 验证结果类
    private static class ValidationResult {
        boolean hasError = false;
        String errorMessage = null;
        int color = 0;
        boolean hasAlpha = false;
    }
    
    // 错误消息常量
    private static final Map<String, String> FALLBACK_TEXTS = Map.of(
        "color.empty_value", "颜色值不能为空",
        "color.invalid_prefix", "颜色格式错误：必须以#开头",
        "color.invalid_length", "颜色格式错误：RGB格式应为#RRGGBB(7位)，ARGB格式应为#AARRGGBB(9位)",
        "color.invalid_chars", "颜色格式错误：只能包含0-9和A-F字符",
        "color.alpha_range", "透明度值超出范围：应在00-FF之间",
        "color.parse_error", "颜色解析失败：无效的十六进制格式"
    );
    
    public static AbstractConfigListEntry<?> build(
        ConfigEntryBuilder entryBuilder,
        String datapackId,
        String key,
        String value,
        DatapackSettings.ConfigMetadata metadata
    ) {
        // 1. 参数验证和默认值处理
        ValidationResult validation = validateColorValue(value, metadata, key);
        boolean hasAlpha = validation.hasAlpha;
        int color = validation.color;
        
        // 添加调试信息，确认颜色模式判断
        LOGGER.debug("[ColorPickerBuilder] Building color picker for key: {}, hasAlpha: {}, value: {}", 
            key, hasAlpha, value);
        
        // 如果初始值无效，记录警告并使用默认值
        if (validation.hasError) {
            LOGGER.warn("[ColorPickerBuilder] Initial color value invalid for {}: {}. Using default: {}. Error: {}",
                key, value, formatColor(getDefaultColor(hasAlpha), hasAlpha), validation.errorMessage);
            color = getDefaultColor(hasAlpha);
        }
        
        // 2. 错误信息处理
        Component displayName = getDisplayName(key, metadata);

        // 3. 计算正确的默认值（从配置文件读取或使用默认值）
        int defaultValue = parseDefaultColor(metadata, hasAlpha);
        
        // 4. 构建颜色选择器，添加错误处理
        // 关键修复：根据Cloth Config文档，setAlphaMode(true)显示8位ARGB，不调用或false显示6位RGB
        var builder = entryBuilder.startColorField(displayName, color)
            .setDefaultValue(defaultValue)  // 重置为数据包配置的默认值
            .setAlphaMode(hasAlpha)  // 关键修复：正确设置alpha模式
            
            // 添加颜色预览框 - 关键修复：确保显示颜色预览
            .setTooltip(Component.literal("点击选择颜色"))
            
            // 确保颜色选择器正确构建
            .setSaveConsumer(newValue -> {
                // 关键修复：直接保存Cloth Config返回的原始值，不进行格式转换
                // Cloth Config内部已根据alpha模式正确处理了格式
                int colorValue = newValue;
                
                // 验证颜色值是否有效
                ValidationResult saveValidation = validateColorValue(formatColor(colorValue, hasAlpha), metadata, key);
                
                if (saveValidation.hasError) {
                    LOGGER.warn("[ColorPickerBuilder] Invalid color value rejected: {} = {} (Error: {}). Saving default instead.", 
                        key, formatColor(colorValue, hasAlpha), saveValidation.errorMessage);
                    // ✅ 参数错误跳转：保存默认值而不是无效值
                    String defaultColor = metadata.getDefaultValue();
                    if (defaultColor == null) {
                        defaultColor = formatColor(getDefaultColor(hasAlpha), hasAlpha);
                    }
                    saveColorValue(datapackId, key, defaultColor);
                    LOGGER.info("[ColorPickerBuilder] Saved default color: {} = {}", key, defaultColor);
                } else {
                    // 关键修复：保存原始颜色值，让Cloth Config处理显示格式
                    // 使用Cloth Config的格式化逻辑，确保格式一致
                    String colorToSave = hasAlpha ? 
                        String.format("#%08X", colorValue & 0xFFFFFFFFL) : 
                        String.format("#%06X", colorValue & 0xFFFFFF);
                    saveColorValue(datapackId, key, colorToSave);
                    LOGGER.debug("[ColorPickerBuilder] Saved color value: {} = {} (hasAlpha: {})", 
                        key, colorToSave, hasAlpha);
                }
            });
            
        // 添加错误处理 - 关键修复：确保错误处理正确
        builder.setErrorSupplier(newValue -> {
            // Cloth Config 的颜色选择器总是返回 ARGB 格式的 int
            // 我们需要根据配置类型格式化后验证
            String formattedColor = formatColor(newValue, hasAlpha);
            ValidationResult realtimeValidation = validateColorValue(formattedColor, metadata, key);
            if (realtimeValidation.hasError) {
                LOGGER.debug("[ColorPickerBuilder] Real-time validation failed for {}: {}", key, realtimeValidation.errorMessage);
                return java.util.Optional.of(Component.literal(realtimeValidation.errorMessage));
            }
            return java.util.Optional.empty(); // 无错误
        });
        
        // 只在有错误消息时设置tooltip
        if (validation.errorMessage != null) {
            builder.setTooltip(Component.literal("§c" + validation.errorMessage));
        }
        
        return builder.build();
    }

    private static int parseColor(String hex, boolean hasAlpha) {
        // 关键修复：参考Cloth Config官方实现
        // 1. 直接解析字符串为8位整数（内部始终使用8位存储）
        // 2. 验证逻辑在validateColorValue()中处理
        try {
            String hexPart = hex.substring(1); // 去掉 #
            
            // 直接解析为整数，内部始终是8位格式 0xAARRGGBB
            // 对于RGB模式，如果输入是6位，解析时会自动在前面补0（Alpha=0）
            // 但我们在validateColorValue()中会验证RGB模式不允许有Alpha
            return (int) Long.parseLong(hexPart, 16);
        } catch (NumberFormatException e) {
            return getDefaultColor(hasAlpha);
        }
    }

    /**
     * 解析配置文件中的默认颜色值
     * 重要：Cloth Config的reset功能比较的是字符串格式，不是解析后的颜色值！
     * 关键修复：不要修改默认值字符串，直接解析并验证
     */
    private static int parseDefaultColor(DatapackSettings.ConfigMetadata metadata, boolean hasAlpha) {
        if (metadata == null) {
            return getDefaultColor(hasAlpha);
        }
        
        String defaultColorStr = metadata.getDefaultValue();
        if (defaultColorStr == null || defaultColorStr.trim().isEmpty()) {
            return getDefaultColor(hasAlpha);
        }
        
        // 关键修复：不要修改默认值字符串格式
        // Cloth Config内部会自动处理RGB/ARGB格式转换
        // 我们只需要确保默认值能够正确解析即可
        
        // 验证默认值格式
        ValidationResult validation = validateColorValue(defaultColorStr, metadata, null);
        if (validation.hasError) {
            LOGGER.warn("[ColorPickerBuilder] Invalid default color value: {}. Using fallback default. Error: {}", 
                defaultColorStr, validation.errorMessage);
            return getDefaultColor(hasAlpha);
        }
        
        // 返回解析后的颜色值，Cloth Config会根据alpha模式正确处理显示
        return validation.color;
    }
    
    /**
     * 获取默认颜色值
     * RGB模式：蓝色（#FF5733，Alpha=0）
     * ARGB模式：半透明蓝色（#80FF5733）
     * 
     * 关键：RGB模式Alpha必须为0，ARGB模式Alpha可以是0-255
     */
    private static int getDefaultColor(boolean hasAlpha) {
        if (hasAlpha) {
            return 0x80FF5733; // 半透明蓝色（Alpha=80）
        } else {
            return 0x00FF5733; // 不透明蓝色（Alpha=0，遵循Cloth Config规则）
        }
    }

    /**
     * 格式化颜色值为字符串（用于保存到数据包）
     * 关键修复：RGB模式必须返回6位格式，ARGB模式返回8位格式
     * Cloth Config返回的colorValue始终是8位ARGB格式，需要正确处理
     */
    private static String formatColor(int color, boolean hasAlpha) {
        if (hasAlpha) {
            // ARGB模式：返回完整的8位格式（#AARRGGBB）
            return String.format("#%08X", color & 0xFFFFFFFFL);
        } else {
            // RGB模式：从8位ARGB中提取6位RGB（移除Alpha通道）
            // Cloth Config返回的colorValue格式：0xAARRGGBB
            // 对于RGB模式，只保留RRGGBB部分
            int rgbPart = color & 0x00FFFFFF; // 移除Alpha通道
            return String.format("#%06X", rgbPart);
        }
    }

    private static void saveColorValue(String datapackId, String key, String value) {
        try {
            DatapackSettings settings = DatapackSettingsManager.getSetting(datapackId);
            if (settings != null) {
                settings.addCustomValue(key, value);
                DatapackSettingsManager.addOrUpdateSetting(settings);
                LOGGER.debug("[ColorPickerBuilder] Saved color value: {} = {}", key, value);
            } else {
                LOGGER.warn("[ColorPickerBuilder] Unable to find settings for datapack: {}", datapackId);
            }
        } catch (Exception e) {
            LOGGER.error("[ColorPickerBuilder] Failed to save color value: {} = {}", key, value, e);
        }
    }
    
    /**
     * 获取用户友好的颜色类型名称
     */
    private static String getColorTypeName(boolean hasAlpha) {
        return hasAlpha ? "ARGB (带透明度)" : "RGB";
    }
    
    /**
     * 判断颜色配置是否有透明度通道
     * 优先级：1.配置项ID > 2.默认值格式 > 3.配置项名称 > 4.默认RGB模式
     */
    private static boolean hasAlphaChannel(DatapackSettings.ConfigMetadata metadata, String configKey) {
        if (metadata == null) return false;
        
        // 添加调试信息
        LOGGER.debug("[ColorPickerBuilder] Checking alpha channel for key: {}, metadata: {}", 
            configKey, metadata != null ? "not null" : "null");
        
        // 1. 优先从配置项ID判断（最可靠）
        if (configKey != null) {
            String lowerKey = configKey.toLowerCase();
            LOGGER.debug("[ColorPickerBuilder] Key analysis: {} -> contains 'argb': {}, contains 'rgb': {}", 
                lowerKey, lowerKey.contains("argb"), lowerKey.contains("rgb"));
            
            if (lowerKey.contains("argb") || lowerKey.contains("alpha") || lowerKey.contains("transparent")) {
                LOGGER.debug("[ColorPickerBuilder] Key indicates ARGB mode");
                return true;
            }
            if (lowerKey.contains("rgb") && !lowerKey.contains("argb")) {
                LOGGER.debug("[ColorPickerBuilder] Key indicates RGB mode (no alpha)");
                return false; // 明确是RGB模式
            }
        }
        
        // 2. 从默认值格式判断
        String defaultValue = metadata.getDefaultValue();
        if (defaultValue != null && defaultValue.startsWith("#")) {
            LOGGER.debug("[ColorPickerBuilder] Default value analysis: {} -> length: {}", 
                defaultValue, defaultValue.length());
            
            if (defaultValue.length() == 9) {
                // 9位格式 #AARRGGBB 表示有透明度
                LOGGER.debug("[ColorPickerBuilder] Default value indicates ARGB mode (9 chars)");
                return true;
            } else if (defaultValue.length() == 7) {
                // 7位格式 #RRGGBB 表示无透明度
                LOGGER.debug("[ColorPickerBuilder] Default value indicates RGB mode (7 chars)");
                return false;
            }
        }
        
        // 3. 从配置项名称或描述中判断类型
        String name = metadata.getLocalizedDisplayName();
        if (name != null) {
            String lowerName = name.toLowerCase();
            LOGGER.debug("[ColorPickerBuilder] Name analysis: {} -> contains 'argb': {}", 
                lowerName, lowerName.contains("argb"));
            
            if (lowerName.contains("argb") || lowerName.contains("透明度") || lowerName.contains("透明")) {
                LOGGER.debug("[ColorPickerBuilder] Name indicates ARGB mode");
                return true;
            }
        }
        
        // 4. 默认情况下，假设为RGB模式（无透明度）
        LOGGER.debug("[ColorPickerBuilder] Defaulting to RGB mode (no alpha)");
        return false;
    }
    
    /**
     * 获取颜色格式示例
     */
    private static String getColorFormatExample(boolean hasAlpha) {
        return hasAlpha ? "#AARRGGBB (例如: #80FF5733)" : "#RRGGBB (例如: #FF5733)";
    }
    
    private static ValidationResult validateColorValue(String value, DatapackSettings.ConfigMetadata metadata, String configKey) {
        ValidationResult result = new ValidationResult();
        
        // 修复：从配置项名称或默认值格式判断颜色类型
        result.hasAlpha = hasAlphaChannel(metadata, configKey);
        
        // 默认值
        result.color = getDefaultColor(result.hasAlpha);
        
        // 基本验证
        String trimmedValue = validateBasicInput(value, result);
        if (trimmedValue == null) return result;
        
        // 格式验证
        if (!validateFormat(trimmedValue, result)) {
            return result;
        }
        
        // 字符验证
        if (!validateHexCharacters(trimmedValue, result)) {
            return result;
        }
        
        // 解析颜色值
        parseColorValue(trimmedValue, result);
        
        return result;
    }
    
    /**
     * 验证基本输入（空值、前缀等）
     */
    private static String validateBasicInput(String value, ValidationResult result) {
        if (value == null || value.trim().isEmpty()) {
            result.hasError = true;
            result.errorMessage = String.format("%s: %s. %s: %s",
                FALLBACK_TEXTS.get("color.empty_value"),
                getColorTypeName(result.hasAlpha),
                "正确格式",
                getColorFormatExample(result.hasAlpha)
            );
            return null;
        }
        
        String trimmedValue = value.trim();
        
        // 检查前缀
        if (!trimmedValue.startsWith("#")) {
            result.hasError = true;
            result.errorMessage = String.format("%s. %s: %s",
                FALLBACK_TEXTS.get("color.invalid_prefix"),
                "正确格式",
                getColorFormatExample(result.hasAlpha)
            );
            return null;
        }
        
        return trimmedValue;
    }
    
    /**
     * 验证格式（长度、Alpha通道等）
     */
    private static boolean validateFormat(String trimmedValue, ValidationResult result) {
        int length = trimmedValue.length();
        
        if (result.hasAlpha) {
            return validateARGBFormat(trimmedValue, length, result);
        } else {
            return validateRGBFormat(trimmedValue, length, result);
        }
    }
    
    /**
     * 验证ARGB格式
     */
    private static boolean validateARGBFormat(String trimmedValue, int length, ValidationResult result) {
        if (length != 9) {
            result.hasError = true;
            result.errorMessage = String.format("%s (当前长度: %d, 期望长度: 9). %s: %s",
                FALLBACK_TEXTS.get("color.invalid_length"),
                length,
                "正确格式",
                getColorFormatExample(true)
            );
            return false;
        }
        
        // 检查Alpha通道是否有效（00-FF）
        String alphaPart = trimmedValue.substring(1, 3);
        try {
            int alpha = Integer.parseInt(alphaPart, 16);
            if (alpha < 0 || alpha > 255) {
                result.hasError = true;
                result.errorMessage = String.format("%s (Alpha通道必须在00-FF范围内). %s: %s",
                    FALLBACK_TEXTS.get("color.invalid_alpha"),
                    "正确格式",
                    getColorFormatExample(true)
                );
                return false;
            }
        } catch (NumberFormatException e) {
            result.hasError = true;
            result.errorMessage = String.format("%s (Alpha通道格式错误). %s: %s",
                FALLBACK_TEXTS.get("color.invalid_alpha"),
                "正确格式",
                getColorFormatExample(true)
            );
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证RGB格式
     */
    private static boolean validateRGBFormat(String trimmedValue, int length, ValidationResult result) {
        // RGB模式：严格限制为7位格式 (#RRGGBB)
        if (length != 7) {
            result.hasError = true;
            result.errorMessage = String.format("%s (当前长度: %d, 期望长度: 7). %s: %s",
                FALLBACK_TEXTS.get("color.invalid_length"),
                length,
                "正确格式",
                getColorFormatExample(false)
            );
            return false;
        }
        
        // 自动修复缺少#前缀的情况（6位或8位）
        if (!trimmedValue.startsWith("#")) {
            if (length == 6) {
                trimmedValue = "#" + trimmedValue;
            } else if (length == 8) {
                trimmedValue = "#" + trimmedValue;
            }
        }
        
        return true;
    }
    
    /**
     * 验证十六进制字符
     */
    private static boolean validateHexCharacters(String trimmedValue, ValidationResult result) {
        String hexPart = trimmedValue.substring(1);
        if (!hexPart.matches("[0-9A-Fa-f]+")) {
            result.hasError = true;
            result.errorMessage = String.format("%s. %s: %s",
                FALLBACK_TEXTS.get("color.invalid_chars"),
                "正确格式",
                getColorFormatExample(result.hasAlpha)
            );
            return false;
        }
        
        // 验证透明度范围（仅ARGB模式）
        if (result.hasAlpha) {
            try {
                // ARGB模式：hexPart是8位AARRGGBB，前2位是Alpha
                int alpha = Integer.parseInt(hexPart.substring(0, 2), 16);
                
                // ARGB模式：Alpha应在00-FF之间
                if (alpha < 0 || alpha > 255) {
                    result.hasError = true;
                    result.errorMessage = String.format("%s (当前值: %02X). 透明度应在 00-FF 之间",
                        FALLBACK_TEXTS.get("color.alpha_range"),
                        alpha
                    );
                    return false;
                }
            } catch (NumberFormatException e) {
                result.hasError = true;
                result.errorMessage = FALLBACK_TEXTS.get("color.parse_error");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 解析颜色值
     */
    private static void parseColorValue(String trimmedValue, ValidationResult result) {
        try {
            result.color = parseColor(trimmedValue, result.hasAlpha);
        } catch (Exception e) {
            result.hasError = true;
            result.errorMessage = String.format("%s. %s: %s",
                FALLBACK_TEXTS.get("color.parse_error"),
                "正确格式",
                getColorFormatExample(result.hasAlpha)
            );
        }
    }
    
    private static Component getDisplayName(String key, DatapackSettings.ConfigMetadata metadata) {
        if (metadata != null) {
            String localizedName = metadata.getLocalizedDisplayName();
            if (localizedName != null && !localizedName.isEmpty()) {
                return Component.literal(localizedName);
            }
        }
        return Component.literal(key);
    }
}