package com.aichen.datapackconfigmod.parser;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据包配置解析器
 * 解析数据包内的配置文件（datapack-config.json）
 */
public class DatapackConfigParser {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    
    // 配置文件名称（支持两种格式：datapack-config.json 和 datapack_config.json）
    private static final String[] CONFIG_FILE_NAMES = {"datapack-config.json", "datapack_config.json"};
    
    /**
     * 解析数据包配置
     * @param datapackFile 数据包文件或文件夹
     * @param datapackId 数据包ID
     * @return 数据包配置，如果没有配置返回null
     */
    public static DatapackSettings parseConfig(File datapackFile, String datapackId) {
        try {
            File configFile = findConfigFile(datapackFile);

            if (configFile == null || !configFile.exists()) {
                return null;
            }

            // 读取配置文件
            String jsonContent = Files.readString(configFile.toPath());
            JsonObject configJson = JsonParser.parseString(jsonContent).getAsJsonObject();

            // 解析基本信息
            String displayName = datapackFile.getName();
            
            // 创建配置对象
            DatapackSettings settings = new DatapackSettings(datapackId, displayName);
            
            if (configJson.has("displayName")) {
                if (configJson.get("displayName").isJsonPrimitive()) {
                    // 简单字符串格式
                    displayName = configJson.get("displayName").getAsString();
                    settings.setDisplayName(displayName);
                } else if (configJson.get("displayName").isJsonObject()) {
                    // 本地化对象格式，使用第一个值作为默认显示名称
                    JsonObject displayNameObj = configJson.getAsJsonObject("displayName");
                    for (Map.Entry<String, JsonElement> entry : displayNameObj.entrySet()) {
                        displayName = entry.getValue().getAsString();
                        settings.setDisplayName(displayName);
                        break; // 使用第一个值作为默认
                    }
                    
                    // 存储本地化文本
                    for (Map.Entry<String, JsonElement> entry : displayNameObj.entrySet()) {
                        settings.addDatapackLocalizedText(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }

            // 解析是否启用
            if (configJson.has("enabled")) {
                settings.setEnabled(configJson.get("enabled").getAsBoolean());
            }

            // 解析数据包本地化文本（新增）
            if (configJson.has("localizedText") && configJson.get("localizedText").isJsonObject()) {
                JsonObject localizedObj = configJson.getAsJsonObject("localizedText");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : localizedObj.entrySet()) {
                    JsonElement element = entry.getValue();
                    if (element.isJsonPrimitive()) {
                        // 简单字符串值
                        settings.addDatapackLocalizedText(entry.getKey(), element.getAsString());
                    } else if (element.isJsonObject()) {
                        // 嵌套对象，存储为JSON字符串
                        settings.addDatapackLocalizedText(entry.getKey(), element.toString());
                    }
                }
            }
            
            // 解析UI本地化文本（用于范围、步长等UI元素）
            if (configJson.has("uiLocalizedText") && configJson.get("uiLocalizedText").isJsonObject()) {
                JsonObject uiLocalizedObj = configJson.getAsJsonObject("uiLocalizedText");
                for (Map.Entry<String, com.google.gson.JsonElement> langEntry : uiLocalizedObj.entrySet()) {
                    String langCode = langEntry.getKey();
                    JsonElement langElement = langEntry.getValue();
                    if (langElement.isJsonObject()) {
                        // 遍历该语言下的所有UI本地化项
                        JsonObject langObj = langElement.getAsJsonObject();
                        for (Map.Entry<String, com.google.gson.JsonElement> textEntry : langObj.entrySet()) {
                            // 存储为 ui_<语言代码>_<键名> 的格式
                            String uiKey = textEntry.getKey() + "_" + langCode;
                            settings.addDatapackLocalizedText(uiKey, textEntry.getValue().getAsString());
                        }
                    }
                }
            }

            // 解析自定义值
            if (configJson.has("customValues") && configJson.get("customValues").isJsonObject()) {
                JsonObject customValues = configJson.getAsJsonObject("customValues");
                Map<String, String> valueMap = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : customValues.entrySet()) {
                    valueMap.put(entry.getKey(), entry.getValue().getAsString());
                }

                settings.setCustomValues(valueMap);
            }

            // 解析typedConfig（增强版配置，支持类型标注）
            if (configJson.has("typedConfig") && configJson.get("typedConfig").isJsonObject()) {
                parseTypedConfig(settings, configJson.getAsJsonObject("typedConfig"));
            }
            
            // 解析config（兼容旧版格式）
            if (configJson.has("config") && configJson.get("config").isJsonObject()) {
                parseConfigSection(settings, configJson.getAsJsonObject("config"));
            }

            if (DataPackConfigMod.LOGGER.isInfoEnabled()) {
                LOGGER.info("[DatapackConfigMod] 成功解析数据包配置: {}", datapackId);
            }

            return settings;

        } catch (IOException e) {
            LOGGER.error("[DatapackConfigMod] 读取数据包配置文件失败: {}", datapackId, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[DatapackConfigMod] 解析数据包配置失败: {}", datapackId, e);
            return null;
        }
    }

    /**
     * 解析config字段（兼容示例数据包格式）
     * @param settings 数据包配置
     * @param config config JSON对象
     */
    private static void parseConfigSection(DatapackSettings settings, JsonObject config) {
        for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
            String key = entry.getKey();
            JsonObject configObj = entry.getValue().getAsJsonObject();
            
            if (!configObj.has("default")) {
                LOGGER.warn("[DatapackConfigMod] 配置 {} 没有default字段，跳过", key);
                continue;
            }
            
            // 获取默认值（仅用于元数据，不直接添加到customValues）
            String defaultValue = configObj.get("default").getAsString();
            
            // 创建元数据
            String type = configObj.has("type") ? configObj.get("type").getAsString() : "string";
            DatapackSettings.ConfigMetadata metadata = new DatapackSettings.ConfigMetadata(type);
            
            // 将默认值存储在元数据中，但不添加到customValues
            // customValues 应该只存储从配置文件读取的实际值或用户修改的值
            metadata.setDefaultValue(defaultValue);
            
            // 解析min/max/step（数值类型）
            if (configObj.has("min")) {
                metadata.setMinValue(configObj.get("min").getAsDouble());
            }
            if (configObj.has("max")) {
                metadata.setMaxValue(configObj.get("max").getAsDouble());
            }
            if (configObj.has("step")) {
                metadata.setStep(configObj.get("step").getAsDouble());
            }
            
            // 解析枚举值（枚举类型）- 枚举值是固定的内部标识符，不随语言变化
            if (configObj.has("enumValues") && configObj.get("enumValues").isJsonArray()) {
                JsonArray enumArray = configObj.getAsJsonArray("enumValues");
                String[] enumValues = new String[enumArray.size()];
                for (int i = 0; i < enumArray.size(); i++) {
                    enumValues[i] = enumArray.get(i).getAsString();
                }
                metadata.setEnumValues(enumValues);
            }
            
            // 解析枚举显示名称（用于UI显示本地化，可选）
            if (configObj.has("enumDisplayNames") && configObj.get("enumDisplayNames").isJsonObject()) {
                JsonObject displayNamesObj = configObj.getAsJsonObject("enumDisplayNames");
                for (Map.Entry<String, JsonElement> langEntry : displayNamesObj.entrySet()) {
                    String langCode = langEntry.getKey();
                    if (langEntry.getValue().isJsonObject()) {
                        JsonObject langDisplayNames = langEntry.getValue().getAsJsonObject();
                        for (Map.Entry<String, JsonElement> nameEntry : langDisplayNames.entrySet()) {
                            // 存储为 enum_display_<语言代码>_<枚举值> 的格式
                            metadata.addLocalizedText("enum_display_" + langCode + "_" + nameEntry.getKey(), nameEntry.getValue().getAsString());
                        }
                    }
                }
            }
            
            // 解析分类（内部标识符）
            if (configObj.has("category")) {
                if (configObj.get("category").isJsonPrimitive()) {
                    // 简单字符串格式，作为内部标识符
                    metadata.setCategory(configObj.get("category").getAsString());
                } else if (configObj.get("category").isJsonObject()) {
                    // 本地化对象格式（旧格式，兼容处理）
                    JsonObject categoryObj = configObj.getAsJsonObject("category");
                    // 存储第一个值作为默认分类名称
                    String defaultCategory = null;
                    for (Map.Entry<String, JsonElement> categoryEntry : categoryObj.entrySet()) {
                        String categoryText = categoryEntry.getValue().getAsString();
                        metadata.addLocalizedText("category_" + categoryEntry.getKey(), categoryText);
                        if (defaultCategory == null) {
                            defaultCategory = categoryText;
                        }
                    }
                    metadata.setCategory(defaultCategory);
                }
            }
            
            // 解析分类显示名称（用于UI显示本地化）
            if (configObj.has("categoryDisplayNames") && configObj.get("categoryDisplayNames").isJsonObject()) {
                JsonObject displayNamesObj = configObj.getAsJsonObject("categoryDisplayNames");
                for (Map.Entry<String, JsonElement> langEntry : displayNamesObj.entrySet()) {
                    String langCode = langEntry.getKey();
                    if (langEntry.getValue().isJsonObject()) {
                        JsonObject langDisplayNames = langEntry.getValue().getAsJsonObject();
                        for (Map.Entry<String, JsonElement> nameEntry : langDisplayNames.entrySet()) {
                            // 存储为 category_display_<语言代码>_<分类标识> 的格式
                            metadata.addLocalizedText("category_display_" + langCode + "_" + nameEntry.getKey(), nameEntry.getValue().getAsString());
                        }
                    }
                }
            }
            
            // 解析locations（新增：支持多文件位置，实现批量修改）
            if (configObj.has("locations") && configObj.get("locations").isJsonArray()) {
                JsonArray locationsArray = configObj.getAsJsonArray("locations");
                for (int i = 0; i < locationsArray.size(); i++) {
                    JsonObject locationObj = locationsArray.get(i).getAsJsonObject();
                    if (locationObj.has("filePath") && locationObj.has("key")) {
                        String locationFilePath = locationObj.get("filePath").getAsString();
                        String locationKey = locationObj.get("key").getAsString();
                        String locationDesc = locationObj.has("description") ? 
                            locationObj.get("description").getAsString() : null;
                        
                        DatapackSettings.ConfigLocation location = new DatapackSettings.ConfigLocation(
                            locationFilePath, locationKey, locationDesc
                        );
                        settings.addConfigLocation(key, location);
                        
                        LOGGER.debug("[DatapackConfigMod] 添加配置位置: {} -> {} ({})", key, locationFilePath, locationKey);
                    }
                }
            }
            // 向后兼容：解析filePath（旧格式）
            else if (configObj.has("filePath")) {
                String filePath = configObj.get("filePath").getAsString();
                metadata.setFilePath(filePath);
                settings.addFilePathMapping(key, filePath);
                
                // 同时添加到locations（用于统一处理）
                String locationKey = configObj.has("key") ? configObj.get("key").getAsString() : key;
                DatapackSettings.ConfigLocation location = new DatapackSettings.ConfigLocation(filePath, locationKey);
                settings.addConfigLocation(key, location);
            }
            
            // 向后兼容：解析key（JSON路径，旧格式）
            if (configObj.has("key")) {
                // 存储key到filePathMappings，供修改文件时使用
                settings.addFilePathMapping(key + ".json_key", configObj.get("key").getAsString());
            }
            
            // 解析本地化名称和描述
            if (configObj.has("name") && configObj.get("name").isJsonObject()) {
                JsonObject nameObj = configObj.getAsJsonObject("name");
                for (Map.Entry<String, JsonElement> nameEntry : nameObj.entrySet()) {
                    metadata.addLocalizedText("name_" + nameEntry.getKey(), nameEntry.getValue().getAsString());
                }
            }
            if (configObj.has("description") && configObj.get("description").isJsonObject()) {
                JsonObject descObj = configObj.getAsJsonObject("description");
                for (Map.Entry<String, JsonElement> descEntry : descObj.entrySet()) {
                    metadata.addLocalizedText("desc_" + descEntry.getKey(), descEntry.getValue().getAsString());
                }
            }
            
            // 解析tooltip（鼠标悬停提示）
            if (configObj.has("tooltip")) {
                if (configObj.get("tooltip").isJsonPrimitive()) {
                    // 简单字符串格式
                    metadata.setTooltip(configObj.get("tooltip").getAsString());
                } else if (configObj.get("tooltip").isJsonObject()) {
                    // 本地化对象格式
                    JsonObject tooltipObj = configObj.getAsJsonObject("tooltip");
                    for (Map.Entry<String, JsonElement> tooltipEntry : tooltipObj.entrySet()) {
                        metadata.addLocalizedTooltipText(tooltipEntry.getKey(), tooltipEntry.getValue().getAsString());
                    }
                }
            }
            
            settings.addConfigMetadata(key, metadata);
        }
    }

    /**
     * 解析增强版配置（支持类型标注）
     * @param settings 数据包配置
     * @param typedConfig typedConfig JSON对象
     */
    private static void parseTypedConfig(DatapackSettings settings, JsonObject typedConfig) {
        for (Map.Entry<String, JsonElement> entry : typedConfig.entrySet()) {
            String key = entry.getKey();
            JsonObject configObj = entry.getValue().getAsJsonObject();

            if (!configObj.has("value")) {
                continue;
            }

            // 获取值
            String value = configObj.get("value").getAsString();
            settings.addCustomValue(key, value);

            // 创建元数据
            String type = configObj.has("type") ? configObj.get("type").getAsString() : "auto";
            DatapackSettings.ConfigMetadata metadata = new DatapackSettings.ConfigMetadata(type);

            // 解析min/max/step（数值类型）
            if (configObj.has("min")) {
                metadata.setMinValue(configObj.get("min").getAsDouble());
            }
            if (configObj.has("max")) {
                metadata.setMaxValue(configObj.get("max").getAsDouble());
            }
            if (configObj.has("step")) {
                metadata.setStep(configObj.get("step").getAsDouble());
            }

            // 解析枚举值（枚举类型）- 枚举值是固定的内部标识符，不随语言变化
            if (configObj.has("enumValues") && configObj.get("enumValues").isJsonArray()) {
                JsonArray enumArray = configObj.getAsJsonArray("enumValues");
                String[] enumValues = new String[enumArray.size()];
                for (int i = 0; i < enumArray.size(); i++) {
                    enumValues[i] = enumArray.get(i).getAsString();
                }
                metadata.setEnumValues(enumValues);
            }
            
            // 解析枚举显示名称（用于UI显示本地化，可选）
            if (configObj.has("enumDisplayNames") && configObj.get("enumDisplayNames").isJsonObject()) {
                JsonObject displayNamesObj = configObj.getAsJsonObject("enumDisplayNames");
                for (Map.Entry<String, JsonElement> langEntry : displayNamesObj.entrySet()) {
                    String langCode = langEntry.getKey();
                    if (langEntry.getValue().isJsonObject()) {
                        JsonObject langDisplayNames = langEntry.getValue().getAsJsonObject();
                        for (Map.Entry<String, JsonElement> nameEntry : langDisplayNames.entrySet()) {
                            // 存储为 enum_display_<语言代码>_<枚举值> 的格式
                            metadata.addLocalizedText("enum_display_" + langCode + "_" + nameEntry.getKey(), nameEntry.getValue().getAsString());
                        }
                    }
                }
            }

            // 解析名称（显示在配置项标题，支持本地化）
            if (configObj.has("name")) {
                if (configObj.get("name").isJsonPrimitive()) {
                    // 简单字符串格式
                    metadata.setDescription(configObj.get("name").getAsString());
                } else if (configObj.get("name").isJsonObject()) {
                    // 本地化对象格式
                    JsonObject nameObj = configObj.getAsJsonObject("name");
                    for (Map.Entry<String, JsonElement> nameEntry : nameObj.entrySet()) {
                        metadata.addLocalizedText("name_" + nameEntry.getKey(), nameEntry.getValue().getAsString());
                    }
                }
            }

            // 解析描述（显示在配置项下方的简短说明）
            if (configObj.has("description")) {
                if (configObj.get("description").isJsonPrimitive()) {
                    // 简单字符串格式
                    metadata.setDescription(configObj.get("description").getAsString());
                } else if (configObj.get("description").isJsonObject()) {
                    // 本地化对象格式
                    JsonObject descObj = configObj.getAsJsonObject("description");
                    for (Map.Entry<String, JsonElement> descEntry : descObj.entrySet()) {
                        metadata.addLocalizedText("desc_" + descEntry.getKey(), descEntry.getValue().getAsString());
                    }
                }
            }
            
            // 解析tooltip（鼠标悬停提示）
            if (configObj.has("tooltip")) {
                if (configObj.get("tooltip").isJsonPrimitive()) {
                    // 简单字符串格式
                    metadata.setTooltip(configObj.get("tooltip").getAsString());
                } else if (configObj.get("tooltip").isJsonObject()) {
                    // 本地化对象格式
                    JsonObject tooltipObj = configObj.getAsJsonObject("tooltip");
                    for (Map.Entry<String, JsonElement> tooltipEntry : tooltipObj.entrySet()) {
                        metadata.addLocalizedTooltipText(tooltipEntry.getKey(), tooltipEntry.getValue().getAsString());
                    }
                }
            }

            // 解析分类（内部标识符）
            if (configObj.has("category")) {
                if (configObj.get("category").isJsonPrimitive()) {
                    // 简单字符串格式，作为内部标识符
                    metadata.setCategory(configObj.get("category").getAsString());
                } else if (configObj.get("category").isJsonObject()) {
                    // 本地化对象格式（旧格式，兼容处理）
                    JsonObject categoryObj = configObj.getAsJsonObject("category");
                    // 存储第一个值作为默认分类名称
                    String defaultCategory = null;
                    for (Map.Entry<String, JsonElement> categoryEntry : categoryObj.entrySet()) {
                        String categoryText = categoryEntry.getValue().getAsString();
                        metadata.addLocalizedText("category_" + categoryEntry.getKey(), categoryText);
                        if (defaultCategory == null) {
                            defaultCategory = categoryText;
                        }
                    }
                    metadata.setCategory(defaultCategory);
                }
            }
            
            // 解析分类显示名称（用于UI显示本地化）
            if (configObj.has("categoryDisplayNames") && configObj.get("categoryDisplayNames").isJsonObject()) {
                JsonObject displayNamesObj = configObj.getAsJsonObject("categoryDisplayNames");
                for (Map.Entry<String, JsonElement> langEntry : displayNamesObj.entrySet()) {
                    String langCode = langEntry.getKey();
                    if (langEntry.getValue().isJsonObject()) {
                        JsonObject langDisplayNames = langEntry.getValue().getAsJsonObject();
                        for (Map.Entry<String, JsonElement> nameEntry : langDisplayNames.entrySet()) {
                            // 存储为 category_display_<语言代码>_<分类标识> 的格式
                            metadata.addLocalizedText("category_display_" + langCode + "_" + nameEntry.getKey(), nameEntry.getValue().getAsString());
                        }
                    }
                }
            }

            // 解析文件路径（新增：支持修改数据包文件）
            if (configObj.has("filePath")) {
                String filePath = configObj.get("filePath").getAsString();
                metadata.setFilePath(filePath);
                settings.addFilePathMapping(key, filePath);
            }

            // 解析本地化键（新增：支持从数据包语言文件读取）
            if (configObj.has("displayNameKey")) {
                metadata.setDisplayNameKey(configObj.get("displayNameKey").getAsString());
            }
            if (configObj.has("descriptionKey")) {
                metadata.setDescriptionKey(configObj.get("descriptionKey").getAsString());
            }

            // 解析内联本地化文本（新增：支持在配置文件中直接定义多语言文本）
            if (configObj.has("localizedText") && configObj.get("localizedText").isJsonObject()) {
                JsonObject localizedObj = configObj.getAsJsonObject("localizedText");
                // 检查是否为当前配置项的本地化文本
                if (localizedObj.has(key) && localizedObj.get(key).isJsonObject()) {
                    JsonObject enumLocalizedObj = localizedObj.getAsJsonObject(key);
                    for (Map.Entry<String, com.google.gson.JsonElement> enumEntry : enumLocalizedObj.entrySet()) {
                        // 存储为 "配置项_枚举值" -> "本地化文本" 格式
                        metadata.addLocalizedText(key + "_" + enumEntry.getKey(), enumEntry.getValue().getAsString());
                    }
                }
            }

            settings.addConfigMetadata(key, metadata);
        }
    }
    
    /**
     * 查找配置文件
     * @param datapackFile 数据包文件或文件夹
     * @return 配置文件，如果不存在返回null
     */
    private static File findConfigFile(File datapackFile) {
        // 如果是文件夹
        if (datapackFile.isDirectory()) {
            for (String configFileName : CONFIG_FILE_NAMES) {
                File configFile = new File(datapackFile, configFileName);
                if (configFile.exists()) {
                    return configFile;
                }
            }
            return null;
        }

        // 如果是.zip文件，尝试从zip中读取配置文件
        if (datapackFile.getName().endsWith(".zip")) {
            // 使用try-with-resources自动管理FileSystem资源
            try (java.nio.file.FileSystem zipFs = java.nio.file.FileSystems.newFileSystem(
                datapackFile.toPath(),
                (ClassLoader) null
            )) {
                
                // 记录ZIP文件结构调试信息
                if (DataPackConfigMod.LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[DatapackConfigMod] 检查ZIP文件结构: {}", datapackFile.getName());
                    
                    // 列出ZIP文件中的所有文件
                    Path rootPath = zipFs.getPath("/");
                    if (Files.isDirectory(rootPath)) {
                        try (var stream = Files.walk(rootPath)) {
                            stream.forEach(path -> {
                                if (Files.isRegularFile(path)) {
                                    LOGGER.debug("[DatapackConfigMod] ZIP文件内容: {}", path);
                                }
                            });
                        }
                    }
                }
                
                // 首先检查根目录下的配置文件
                for (String configFileName : CONFIG_FILE_NAMES) {
                    Path configPath = zipFs.getPath(configFileName);
                    if (Files.exists(configPath)) {
                        LOGGER.debug("[DatapackConfigMod] 在ZIP文件根目录找到配置文件: {} -> {}", 
                            datapackFile.getName(), configFileName);
                        // 创建临时文件来存储zip中的配置内容
                        Path tempFile = Files.createTempFile("datapack-config-", ".json");
                        Files.copy(configPath, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        // 返回临时文件（标记为需要删除）
                        return tempFile.toFile();
                    }
                }
                
                // 检查zip文件内部结构，是否包含数据包的标准结构
                // 如果是标准数据包结构，配置文件可能在根目录，也可能在data文件夹内
                Path rootPath = zipFs.getPath("/");
                if (Files.isDirectory(rootPath)) {
                    // 检查zip文件是否包含pack.mcmeta（标准数据包结构）
                    Path packMetaPath = zipFs.getPath("pack.mcmeta");
                    if (Files.exists(packMetaPath)) {
                        LOGGER.debug("[DatapackConfigMod] ZIP文件包含标准数据包结构: {}", datapackFile.getName());
                        
                        // 检查data文件夹内是否有配置文件
                        Path dataPath = zipFs.getPath("data");
                        if (Files.exists(dataPath) && Files.isDirectory(dataPath)) {
                            // 遍历data文件夹下的所有命名空间
                            try (var stream = Files.list(dataPath)) {
                                var namespaceIterator = stream.filter(Files::isDirectory).iterator();
                                
                                while (namespaceIterator.hasNext()) {
                                    Path namespacePath = namespaceIterator.next();
                                    for (String configFileName : CONFIG_FILE_NAMES) {
                                        Path configPath = namespacePath.resolve(configFileName);
                                        if (Files.exists(configPath)) {
                                            LOGGER.debug("[DatapackConfigMod] 在ZIP文件data文件夹找到配置文件: {} -> {}", 
                                                datapackFile.getName(), configPath);
                                            
                                            try {
                                                // 创建临时文件来存储zip中的配置内容
                                                Path tempFile = Files.createTempFile("datapack-config-", ".json");
                                                Files.copy(configPath, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                                // 返回临时文件（标记为需要删除）
                                                return tempFile.toFile();
                                            } catch (IOException e) {
                                                LOGGER.error("[DatapackConfigMod] 从ZIP文件复制配置文件失败: {}", datapackFile.getName(), e);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                LOGGER.debug("[DatapackConfigMod] ZIP文件没有找到配置文件: {}", datapackFile.getName());
                return null;
                
            } catch (IOException e) {
                LOGGER.error("[DatapackConfigMod] 读取zip数据包配置失败: {}", datapackFile.getName(), e);
                return null;
            }
        }

        return null;
    }
    
    /**
     * 生成数据包配置模板（动态生成，无硬编码）
     * @param datapackId 数据包ID（如 "example"）
     * @return 配置模板JSON字符串
     */
    public static String generateConfigTemplate(String datapackId) {
        JsonObject template = new JsonObject();
        
        // 显示名称
        template.addProperty("displayName", "My Datapack");
        
        // 数据包ID
        template.addProperty("datapackId", datapackId);
        
        // 是否启用
        template.addProperty("enabled", true);
        
        // 配置项示例（动态生成路径）
        JsonObject config = new JsonObject();
        
        // 示例：布尔值配置
        JsonObject enableFeature = new JsonObject();
        enableFeature.addProperty("type", "boolean");
        enableFeature.addProperty("default", "true");
        enableFeature.addProperty("filePath", "data/" + datapackId + "/config/main.json");
        enableFeature.addProperty("key", "enable_feature");
        config.add("enable_feature", enableFeature);
        
        // 示例：整数滑块
        JsonObject spawnRate = new JsonObject();
        spawnRate.addProperty("type", "slider");
        spawnRate.addProperty("default", "50");
        spawnRate.addProperty("min", 0);
        spawnRate.addProperty("max", 100);
        spawnRate.addProperty("step", 1);
        spawnRate.addProperty("filePath", "data/" + datapackId + "/config/spawning.json");
        spawnRate.addProperty("key", "spawn_rate");
        config.add("spawn_rate", spawnRate);
        
        template.add("config", config);
        
        return GSON.toJson(template);
    }
    
    /**
     * 验证配置是否有效
     * @param configFile 配置文件
     * @return 是否有效
     */
    public static boolean isValidConfig(File configFile) {
        if (!configFile.exists()) {
            return false;
        }
        
        try {
            String jsonContent = Files.readString(configFile.toPath());
            JsonObject configJson = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            // 至少需要displayName字段
            return configJson.has("displayName");
        } catch (Exception e) {
            return false;
        }
    }
}
