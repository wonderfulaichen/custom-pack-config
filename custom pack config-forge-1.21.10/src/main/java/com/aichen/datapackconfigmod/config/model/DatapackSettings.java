package com.aichen.datapackconfigmod.config.model;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据包配置模型
 * 表示单个数据包的配置设置
 */
public class DatapackSettings {

    // 数据包ID
    private String datapackId;

    // 数据包显示名称
    private String displayName;

    // 是否启用此数据包
    private boolean enabled = true;

    // 自定义配置值（键值对）
    private Map<String, String> customValues = new HashMap<>();

    // 配置元数据（键值对，存储配置类型等信息）
    private Map<String, ConfigMetadata> configMetadata = new HashMap<>();

    // 文件路径映射（配置键 -> 目标文件路径）
    private Map<String, String> filePathMappings = new HashMap<>();

    // 配置位置映射（配置键 -> 文件位置列表）
    // 支持一个参数对应多个文件位置，实现批量修改
    private Map<String, List<ConfigLocation>> configLocations = new HashMap<>();

    /**
     * 配置文件位置类
     * 表示一个配置项在文件中的具体位置
     */
    public static class ConfigLocation {
        private String filePath;  // 目标文件路径
        private String key;       // JSON路径或配置键
        private String description; // 位置描述（可选）

        public ConfigLocation(String filePath, String key) {
            this.filePath = filePath;
            this.key = key;
        }

        public ConfigLocation(String filePath, String key, String description) {
            this.filePath = filePath;
            this.key = key;
            this.description = description;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 配置元数据类
     */
    public static class ConfigMetadata {
        private String type;  // 配置类型: boolean, int, double, string, slider, enum
        private double minValue;
        private double maxValue;
        private double step;
        private String description;  // 配置项下方的说明文本
        private String tooltip;  // 鼠标悬停提示文本
        private String category;  // 配置分类，支持多级（如 "游戏设置/难度设置"）
        private String filePath;  // 目标文件路径（如 "data/example/functions/setup.mcfunction"）
        private Map<String, String> localizedText;  // 本地化文本（语言代码 -> 文本）
        private Map<String, String> localizedTooltipText;  // 本地化tooltip文本（语言代码 -> 文本）
        private String[] enumValues;  // 枚举类型的可选值
        private String defaultValue;  // 默认值（从datapack_config.json读取）

        // 本地化键（可选，用于从数据包语言文件读取）
        private String displayNameKey;  // 显示名称的本地化键
        private String descriptionKey;  // 描述的本地化键
        private String tooltipKey;  // tooltip的本地化键

        public ConfigMetadata(String type) {
            this.type = type;
            this.minValue = 0;
            this.maxValue = 1000;
            this.step = 1;
            this.enumValues = null;
            this.category = null;  // 默认为 null，表示不分类
            this.localizedText = new HashMap<>();
            this.localizedTooltipText = new HashMap<>();
            this.defaultValue = null;  // 默认为 null
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public double getMinValue() {
            return minValue;
        }

        public void setMinValue(double minValue) {
            this.minValue = minValue;
        }

        public double getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(double maxValue) {
            this.maxValue = maxValue;
        }

        public double getStep() {
            return step;
        }

        public void setStep(double step) {
            this.step = step;
        }

        public String[] getEnumValues() {
            return enumValues;
        }

        public void setEnumValues(String[] enumValues) {
            this.enumValues = enumValues;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        public Map<String, String> getLocalizedTooltipText() {
            return localizedTooltipText;
        }

        public void setLocalizedTooltipText(Map<String, String> localizedTooltipText) {
            this.localizedTooltipText = localizedTooltipText;
        }

        public void addLocalizedTooltipText(String langCode, String text) {
            this.localizedTooltipText.put(langCode, text);
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public Map<String, String> getLocalizedText() {
            return localizedText;
        }

        public void setLocalizedText(Map<String, String> localizedText) {
            this.localizedText = localizedText;
        }

        public void addLocalizedText(String langCode, String text) {
            this.localizedText.put(langCode, text);
        }

        public String getDisplayNameKey() {
            return displayNameKey;
        }

        public void setDisplayNameKey(String displayNameKey) {
            this.displayNameKey = displayNameKey;
        }

        public String getDescriptionKey() {
            return descriptionKey;
        }

        public void setDescriptionKey(String descriptionKey) {
            this.descriptionKey = descriptionKey;
        }

        public String getTooltipKey() {
            return tooltipKey;
        }

        public void setTooltipKey(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }

        /**
         * 获取本地化显示文本
         * @return 本地化后的文本
         */
        public String getLocalizedDisplayName() {
            // 尝试从游戏语言文件获取
            if (displayNameKey != null && !displayNameKey.isEmpty()) {
                try {
                    Language lang = Language.getInstance();
                    String localized = lang.getOrDefault(displayNameKey);
                    if (!localized.equals(displayNameKey)) {
                        return localized;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续尝试内联文本
                }
            }

            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }

            // 尝试匹配当前语言的 name 文本
            String localized = localizedText.get("name_" + currentLangCode);
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 回退到英语 name
            localized = localizedText.get("name_en_us");
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 如果没有 name 文本，回退到空字符串
            return "";
        }

        /**
         * 获取本地化描述文本
         * @return 本地化后的描述
         */
        public String getLocalizedDescription() {
            // 尝试从游戏语言文件获取
            if (descriptionKey != null && !descriptionKey.isEmpty()) {
                try {
                    Language lang = Language.getInstance();
                    String localized = lang.getOrDefault(descriptionKey);
                    if (!localized.equals(descriptionKey)) {
                        return localized;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续尝试内联文本
                }
            }

            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }

            // 尝试匹配当前语言的 desc 文本
            String localized = localizedText.get("desc_" + currentLangCode);
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 回退到英语 desc
            localized = localizedText.get("desc_en_us");
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 返回空字符串而不是 null
            return "";
        }

        /**
         * 获取本地化分类显示名称（新格式，支持categoryDisplayNames）
         * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
         * @return 本地化后的分类显示名称
         */
        public String getLocalizedCategoryDisplayName(Map<String, String> datapackLocalizedText) {
            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
            
            // 尝试从数据包本地化文本获取分类显示名称（新格式）
            String localized = null;
            if (datapackLocalizedText != null && category != null) {
                localized = datapackLocalizedText.get("category_display_" + currentLangCode + "_" + category);
                if (localized == null || localized.isEmpty()) {
                    localized = datapackLocalizedText.get("category_display_en_us_" + category);
                }
            }
            
            // 如果没有数据包级别的本地化，使用配置项级别的本地化
            if (localized == null || localized.isEmpty()) {
                localized = localizedText.get("category_display_" + currentLangCode + "_" + category);
                if (localized == null || localized.isEmpty()) {
                    localized = localizedText.get("category_display_en_us_" + category);
                }
            }
            
            // 回退到旧格式（category_<语言代码>）
            if (localized == null || localized.isEmpty()) {
                localized = localizedText.get("category_" + currentLangCode);
                if (localized == null || localized.isEmpty()) {
                    localized = localizedText.get("category_en_us");
                }
            }
            
            // 如果没有本地化文本，返回原始分类名称
            if (localized == null || localized.isEmpty()) {
                return category != null ? category : "";
            }
            
            return localized;
        }
        
        /**
         * 获取本地化分类显示名称（重载方法，不带数据包本地化文本参数）
         * @return 本地化后的分类显示名称
         */
        public String getLocalizedCategoryDisplayName() {
            return getLocalizedCategoryDisplayName(null);
        }
        
        /**
         * 获取本地化分类文本（旧方法，兼容旧格式）
         * @return 本地化后的分类名称
         */
        public String getLocalizedCategory() {
            return getLocalizedCategoryDisplayName();
        }

        /**
         * 获取本地化tooltip文本
         * @return 本地化后的tooltip
         */
        public String getLocalizedTooltip() {
            // 尝试从游戏语言文件获取
            if (tooltipKey != null && !tooltipKey.isEmpty()) {
                try {
                    Language lang = Language.getInstance();
                    String localized = lang.getOrDefault(tooltipKey);
                    if (!localized.equals(tooltipKey)) {
                        return localized;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续尝试内联文本
                }
            }

            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }

            // 首先尝试从 localizedTooltipText 获取（存储tooltip专用文本）
            String localized = localizedTooltipText.get(currentLangCode);
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 回退到英语
            localized = localizedTooltipText.get("en_us");
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 然后尝试从 localizedText 获取（存储通用本地化文本）
            localized = localizedText.get("tooltip_" + currentLangCode);
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 回退到英语
            localized = localizedText.get("tooltip_en_us");
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }

            // 返回tooltip字段作为回退
            return tooltip != null ? tooltip : "";
        }
        
        /**
         * 获取本地化的范围文本
         * @param min 最小值
         * @param max 最大值
         * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
         * @return 本地化后的范围文本
         */
        public String getLocalizedRange(double min, double max, Map<String, String> datapackLocalizedText) {
            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
            
            // 尝试从数据包本地化文本获取范围文本
            String rangeText = null;
            if (datapackLocalizedText != null) {
                rangeText = datapackLocalizedText.get("range_" + currentLangCode);
                if (rangeText == null || rangeText.isEmpty()) {
                    rangeText = datapackLocalizedText.get("range_en_us");
                }
            }
            
            // 如果没有数据包级别的本地化，使用配置项级别的本地化
            if (rangeText == null || rangeText.isEmpty()) {
                rangeText = localizedText.get("range_" + currentLangCode);
                if (rangeText == null || rangeText.isEmpty()) {
                    rangeText = localizedText.get("range_en_us");
                }
            }
            
            if (rangeText == null || rangeText.isEmpty()) {
                // 默认文本
                if (currentLangCode.startsWith("zh")) {
                    rangeText = "范围";
                } else {
                    rangeText = "Range";
                }
            }
            
            return rangeText + ": " + min + " - " + max;
        }
        
        /**
         * 获取本地化的范围文本（重载方法，不带数据包本地化文本参数）
         * @param min 最小值
         * @param max 最大值
         * @return 本地化后的范围文本
         */
        public String getLocalizedRange(double min, double max) {
            return getLocalizedRange(min, max, null);
        }
        
        /**
         * 获取本地化的滑块提示文本
         * @param min 最小值
         * @param max 最大值
         * @param step 步长
         * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
         * @return 本地化后的滑块提示文本
         */
        public String getLocalizedSliderTooltip(double min, double max, double step, Map<String, String> datapackLocalizedText) {
            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
            
            // 尝试从数据包本地化文本获取滑块提示
            String sliderTooltip = null;
            if (datapackLocalizedText != null) {
                sliderTooltip = datapackLocalizedText.get("slider_tooltip_" + currentLangCode);
                if (sliderTooltip == null || sliderTooltip.isEmpty()) {
                    sliderTooltip = datapackLocalizedText.get("slider_tooltip_en_us");
                }
            }
            
            // 如果没有数据包级别的本地化，使用配置项级别的本地化
            if (sliderTooltip == null || sliderTooltip.isEmpty()) {
                sliderTooltip = localizedText.get("slider_tooltip_" + currentLangCode);
                if (sliderTooltip == null || sliderTooltip.isEmpty()) {
                    sliderTooltip = localizedText.get("slider_tooltip_en_us");
                }
            }
            
            if (sliderTooltip == null || sliderTooltip.isEmpty()) {
                // 默认文本
                if (currentLangCode.startsWith("zh")) {
                    sliderTooltip = "拖动滑块调整数值";
                } else {
                    sliderTooltip = "Drag slider to adjust value";
                }
            }
            
            return sliderTooltip + ", " + getLocalizedRange(min, max, datapackLocalizedText) + ", " + getLocalizedStepText(step, datapackLocalizedText);
        }
        
        /**
         * 获取本地化的滑块提示文本（重载方法，不带数据包本地化文本参数）
         * @param min 最小值
         * @param max 最大值
         * @param step 步长
         * @return 本地化后的滑块提示文本
         */
        public String getLocalizedSliderTooltip(double min, double max, double step) {
            return getLocalizedSliderTooltip(min, max, step, null);
        }
        
        /**
         * 获取本地化的步长文本
         * @param step 步长值
         * @param datapackLocalizedText 数据包级别的本地化文本映射（可选）
         * @return 本地化后的步长文本
         */
        public String getLocalizedStepText(double step, Map<String, String> datapackLocalizedText) {
            // 获取当前游戏语言
            String currentLangCode = "en_us"; // 默认英语
            try {
                currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
            
            // 尝试从数据包本地化文本获取步长文本
            String stepText = null;
            if (datapackLocalizedText != null) {
                stepText = datapackLocalizedText.get("step_" + currentLangCode);
                if (stepText == null || stepText.isEmpty()) {
                    stepText = datapackLocalizedText.get("step_en_us");
                }
            }
            
            // 如果没有数据包级别的本地化，使用配置项级别的本地化
            if (stepText == null || stepText.isEmpty()) {
                stepText = localizedText.get("step_" + currentLangCode);
                if (stepText == null || stepText.isEmpty()) {
                    stepText = localizedText.get("step_en_us");
                }
            }
            
            if (stepText == null || stepText.isEmpty()) {
                // 默认文本
                if (currentLangCode.startsWith("zh")) {
                    stepText = "步长";
                } else {
                    stepText = "Step";
                }
            }
            
            return stepText + ": " + step;
        }
    }

    // 数据包本地化文本映射
    private Map<String, String> datapackLocalizedText = new HashMap<>();

    /**
     * 获取数据包本地化文本
     * @return 本地化文本映射
     */
    public Map<String, String> getDatapackLocalizedText() {
        return datapackLocalizedText;
    }

    /**
     * 设置数据包本地化文本
     * @param localizedText 本地化文本映射
     */
    public void setDatapackLocalizedText(Map<String, String> localizedText) {
        this.datapackLocalizedText = localizedText;
    }

    /**
     * 添加数据包本地化文本
     * @param langCode 语言代码（如 "zh_cn", "en_us"）
     * @param text 文本
     */
    public void addDatapackLocalizedText(String langCode, String text) {
        this.datapackLocalizedText.put(langCode, text);
    }

    /**
     * 获取本地化的数据包显示名称
     * @return 本地化后的名称
     */
    public String getLocalizedDisplayName() {
        // 获取当前游戏语言
        String currentLangCode = "en_us"; // 默认英语
        try {
            currentLangCode = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }

        // 尝试匹配当前语言
        String localized = datapackLocalizedText.get(currentLangCode);
        if (localized != null && !localized.isEmpty()) {
            return localized;
        }

        // 回退到英语
        localized = datapackLocalizedText.get("en_us");
        if (localized != null && !localized.isEmpty()) {
            return localized;
        }

        // 使用默认显示名称
        return displayName;
    }

    /**
     * 构造函数
     * @param datapackId 数据包ID
     * @param displayName 显示名称
     */
    public DatapackSettings(String datapackId, String displayName) {
        this.datapackId = datapackId;
        this.displayName = displayName;
    }

    /**
     * 获取数据包ID
     * @return 数据包ID
     */
    public String getDatapackId() {
        return datapackId;
    }

    /**
     * 设置数据包ID
     * @param datapackId 数据包ID
     */
    public void setDatapackId(String datapackId) {
        this.datapackId = datapackId;
    }

    /**
     * 获取显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置显示名称
     * @param displayName 显示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取自定义配置值
     * @return 自定义配置值映射
     */
    public Map<String, String> getCustomValues() {
        return customValues;
    }

    /**
     * 设置自定义配置值
     * @param customValues 自定义配置值映射
     */
    public void setCustomValues(Map<String, String> customValues) {
        this.customValues = customValues;
    }

    /**
     * 添加自定义配置值
     * @param key 键
     * @param value 值
     */
    public void addCustomValue(String key, String value) {
        this.customValues.put(key, value);
    }

    /**
     * 获取自定义配置值
     * @param key 键
     * @return 值，如果不存在返回null
     */
    public String getCustomValue(String key) {
        return customValues.get(key);
    }

    /**
     * 获取布尔类型的自定义配置值
     * @param key 键
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        String value = customValues.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取整数类型的自定义配置值
     * @param key 键
     * @param defaultValue 默认值
     * @return 整数值
     */
    public int getIntValue(String key, int defaultValue) {
        String value = customValues.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取浮点数类型的自定义配置值
     * @param key 键
     * @param defaultValue 默认值
     * @return 浮点数值
     */
    public double getDoubleValue(String key, double defaultValue) {
        String value = customValues.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 添加配置元数据
     * @param key 键
     * @param metadata 元数据
     */
    public void addConfigMetadata(String key, ConfigMetadata metadata) {
        this.configMetadata.put(key, metadata);
    }

    /**
     * 获取配置元数据
     * @param key 键
     * @return 元数据，如果不存在返回null
     */
    public ConfigMetadata getConfigMetadata(String key) {
        return configMetadata.get(key);
    }

    /**
     * 获取所有配置元数据
     * @return 配置元数据映射
     */
    public Map<String, ConfigMetadata> getAllConfigMetadata() {
        return configMetadata;
    }

    /**
     * 添加文件路径映射
     * @param key 配置键
     * @param filePath 目标文件路径
     */
    public void addFilePathMapping(String key, String filePath) {
        this.filePathMappings.put(key, filePath);
    }

    /**
     * 获取文件路径映射
     * @param key 配置键
     * @return 目标文件路径，如果不存在返回null
     */
    public String getFilePathMapping(String key) {
        return filePathMappings.get(key);
    }

    /**
     * 获取所有文件路径映射
     * @return 文件路径映射
     */
    public Map<String, String> getAllFilePathMappings() {
        return filePathMappings;
    }

    /**
     * 添加配置位置
     * @param configKey 配置键
     * @param location 配置位置
     */
    public void addConfigLocation(String configKey, ConfigLocation location) {
        this.configLocations.computeIfAbsent(configKey, k -> new ArrayList<>()).add(location);
    }

    /**
     * 获取配置位置列表
     * @param configKey 配置键
     * @return 配置位置列表，如果不存在返回空列表
     */
    public List<ConfigLocation> getConfigLocations(String configKey) {
        if (configLocations == null) {
            return new ArrayList<>();
        }
        return configLocations.getOrDefault(configKey, new ArrayList<>());
    }

    /**
     * 获取所有配置位置映射
     * @return 配置位置映射
     */
    public Map<String, List<ConfigLocation>> getAllConfigLocations() {
        return configLocations;
    }

    /**
     * 设置配置位置映射
     * @param configLocations 配置位置映射
     */
    public void setConfigLocations(Map<String, List<ConfigLocation>> configLocations) {
        this.configLocations = configLocations;
    }

    /**
     * 判断配置是否有多个位置
     * @param configKey 配置键
     * @return 如果有多个位置返回true
     */
    public boolean hasMultipleLocations(String configKey) {
        if (configLocations == null) {
            return false;
        }
        List<ConfigLocation> locations = configLocations.get(configKey);
        return locations != null && locations.size() > 1;
    }
}
