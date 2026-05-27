package com.aichen.datapackconfigmod.manager;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.DatapackConfig;
import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 数据包文件修改器
 * 负责根据 datapack-config.json 中定义的配置信息，修改数据包内部的文件内容
 *
 * 工作流程：
 * 1. 从 datapack-config.json 读取配置信息（包含 filePath、默认值等元数据）
 * 2. 用户在 GUI 中修改配置值
 * 3. 根据 filePath 字段定位目标文件
 * 4. 直接修改数据包内的文件内容
 */
public class DatapackFileModifier {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 应用数据包配置到实际文件
     * @param datapackId 数据包ID
     * @param datapackFolder 数据包根文件夹（可能是文件夹或zip文件）
     */
    public static void applyDatapackConfig(String datapackId, File datapackFolder) {
        LOGGER.debug("[DatapackFileModifier] >>>>> 进入 applyDatapackConfig >>>>>");
        LOGGER.debug("[DatapackFileModifier] 数据包ID: {}", datapackId);
        LOGGER.debug("[DatapackFileModifier] 数据包文件夹: {}", datapackFolder.getAbsolutePath());
        LOGGER.debug("[DatapackFileModifier] 是否为ZIP: {}", datapackFolder.isFile() && datapackFolder.getName().endsWith(".zip"));

        DatapackSettings settings = DatapackSettingsManager.getSetting(datapackId);
        if (settings == null) {
            LOGGER.error("[DatapackFileModifier] 未找到数据包配置: {}", datapackId);
            return;
        }

        Map<String, String> customValues = settings.getCustomValues();
        LOGGER.debug("[DatapackFileModifier] 数据包 {} 有 {} 个自定义配置", datapackId, customValues.size());

        if (customValues.isEmpty()) {
            LOGGER.warn("[DatapackFileModifier] 数据包 {} 没有自定义配置", datapackId);
            return;
        }

        // 检查是否为 ZIP 文件
        if (datapackFolder.isFile() && datapackFolder.getName().endsWith(".zip")) {
            LOGGER.debug("[DatapackFileModifier] 调用 applyDatapackConfigToZip");
            applyDatapackConfigToZip(datapackId, datapackFolder, settings, customValues);
            LOGGER.debug("[DatapackFileModifier] applyDatapackConfigToZip 返回");
        } else {
            LOGGER.debug("[DatapackFileModifier] 调用 applyDatapackConfigToFolder");
            applyDatapackConfigToFolder(datapackId, datapackFolder, settings, customValues);
            LOGGER.debug("[DatapackFileModifier] applyDatapackConfigToFolder 返回");
        }

        LOGGER.debug("[DatapackFileModifier] <<<<< 退出 applyDatapackConfig <<<<<");
    }

    /**
     * 应用配置到文件夹格式的数据包
     */
    private static void applyDatapackConfigToFolder(String datapackId, File datapackFolder,
                                                      DatapackSettings settings, Map<String, String> customValues) {
        int modifiedCount = 0;
        int skippedCount = 0;

        for (Map.Entry<String, String> entry : customValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 获取配置位置列表（支持多文件位置）
            List<DatapackSettings.ConfigLocation> locations = settings.getConfigLocations(key);
            
            if (locations.isEmpty()) {
                // 向后兼容：使用旧的filePath方式
                DatapackSettings.ConfigMetadata metadata = settings.getConfigMetadata(key);
                if (metadata != null && metadata.getFilePath() != null) {
                    String filePath = metadata.getFilePath();
                    File targetFile = new File(datapackFolder, filePath);
                    String jsonKey = settings.getFilePathMapping(key + ".json_key");
                    if (jsonKey == null) jsonKey = key;
                    
                    if (modifyFile(targetFile, filePath, jsonKey, value, metadata)) {
                        modifiedCount++;
                    } else {
                        skippedCount++;
                    }
                } else {
                    LOGGER.debug("[DatapackFileModifier] 配置 {} 没有文件路径，跳过", key);
                    skippedCount++;
                }
                continue;
            }

            // 批量修改所有位置
            int locationModifiedCount = 0;
            int locationSkippedCount = 0;
            
            for (DatapackSettings.ConfigLocation location : locations) {
                String filePath = location.getFilePath();
                String jsonKey = location.getKey();
                
                // 构建完整文件路径
                File targetFile = new File(datapackFolder, filePath);
                if (!targetFile.exists()) {
                    LOGGER.warn("[DatapackFileModifier] 目标文件不存在: {}", targetFile.getAbsolutePath());
                    locationSkippedCount++;
                    continue;
                }

                // 获取配置元数据
                DatapackSettings.ConfigMetadata metadata = settings.getConfigMetadata(key);
                if (metadata == null) {
                    LOGGER.debug("[DatapackFileModifier] 配置 {} 没有元数据，跳过", key);
                    locationSkippedCount++;
                    continue;
                }

                // 根据文件类型进行修改
                try {
                    LOGGER.debug("[DatapackFileModifier] 批量修改: {} -> {} ({})", key, filePath, jsonKey);
                    
                    if (modifyFile(targetFile, filePath, jsonKey, value, metadata)) {
                        locationModifiedCount++;
                        LOGGER.debug("[DatapackFileModifier] 已修改文件 {}: {} = {}",
                            targetFile.getName(), key, value);
                    } else {
                        locationSkippedCount++;
                    }
                } catch (Exception e) {
                    LOGGER.error("[DatapackFileModifier] 修改文件失败: {}", targetFile.getAbsolutePath(), e);
                    locationSkippedCount++;
                }
            }
            
            if (locationModifiedCount > 0) {
                modifiedCount++;
                LOGGER.info("[DatapackFileModifier] 配置 {} 批量修改完成: 成功 {} 个位置, 跳过 {} 个位置",
                    key, locationModifiedCount, locationSkippedCount);
            } else {
                skippedCount++;
            }
        }

        if (modifiedCount > 0) {
            LOGGER.info("[DatapackFileModifier] 数据包 {} 配置应用完成: 修改 {} 个配置, 跳过 {} 个",
                datapackId, modifiedCount, skippedCount);
        }
    }

    /**
     * 应用配置到 ZIP 格式的数据包
     */
    private static void applyDatapackConfigToZip(String datapackId, File zipFile,
                                                  DatapackSettings settings, Map<String, String> customValues) {
        LOGGER.debug("[DatapackFileModifier] ========== ZIP处理开始 ==========");
        LOGGER.debug("[DatapackFileModifier] 数据包ID: {}", datapackId);
        LOGGER.debug("[DatapackFileModifier] ZIP文件路径: {}", zipFile.getAbsolutePath());
        LOGGER.debug("[DatapackFileModifier] 配置项数量: {}", customValues.size());

        if (customValues.isEmpty()) {
            LOGGER.debug("[DatapackFileModifier] 警告: 没有自定义配置值！");
            return;
        }

        // 打印所有配置项
        for (Map.Entry<String, String> entry : customValues.entrySet()) {
            LOGGER.debug("[DatapackFileModifier] 配置项: {} = {}", entry.getKey(), entry.getValue());
        }

        // 优化：创建配置路径映射，支持多文件位置批量修改
        Map<String, java.util.List<ConfigChange>> changesByFile = new java.util.HashMap<>();
        
        for (Map.Entry<String, String> configEntry : customValues.entrySet()) {
            String configKey = configEntry.getKey();
            String configValue = configEntry.getValue();
            
            DatapackSettings.ConfigMetadata metadata = settings.getConfigMetadata(configKey);
            if (metadata == null) {
                LOGGER.debug("[DatapackFileModifier] 配置 {} 没有元数据，跳过", configKey);
                continue;
            }
            
            // 获取配置位置列表（支持多文件位置）
            List<DatapackSettings.ConfigLocation> locations = settings.getConfigLocations(configKey);
            
            if (locations.isEmpty()) {
                // 向后兼容：使用旧的filePath方式
                String targetPath = metadata.getFilePath();
                if (targetPath == null || targetPath.isEmpty()) {
                    LOGGER.debug("[DatapackFileModifier] 配置 {} 没有文件路径，跳过", configKey);
                    continue;
                }
                
                String jsonKey = settings.getFilePathMapping(configKey + ".json_key");
                if (jsonKey == null) jsonKey = configKey;
                
                changesByFile.computeIfAbsent(targetPath, k -> new java.util.ArrayList<>())
                            .add(new ConfigChange(configKey, configValue, metadata, jsonKey));
            } else {
                // 批量处理所有位置
                for (DatapackSettings.ConfigLocation location : locations) {
                    String targetPath = location.getFilePath();
                    String jsonKey = location.getKey();
                    
                    changesByFile.computeIfAbsent(targetPath, k -> new java.util.ArrayList<>())
                                .add(new ConfigChange(configKey, configValue, metadata, jsonKey));
                    
                    LOGGER.debug("[DatapackFileModifier] 添加配置变更: {} -> {} ({})", configKey, targetPath, jsonKey);
                }
            }
        }
        
        if (changesByFile.isEmpty()) {
            LOGGER.info("[DatapackFileModifier] 没有需要修改的配置");
            return;
        }
        
        LOGGER.info("[DatapackFileModifier] 需要修改的文件数: {}", changesByFile.size());

        int modifiedCount = 0;
        int skippedCount = 0;

        // 用于存储需要修改的文件：filePath -> 新内容
        Map<String, byte[]> filesToUpdate = new java.util.HashMap<>();

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            int totalEntries = 0;
            int matchedFiles = 0;

            // 单次遍历：读取所有需要修改的文件内容
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                totalEntries++;

                // 检查这个文件是否在需要修改的列表中
                java.util.List<ConfigChange> changes = changesByFile.get(entryName);
                if (changes == null) {
                    continue; // 这个文件不需要修改，继续下一个
                }
                
                matchedFiles++;
                LOGGER.info("[DatapackFileModifier] 找到匹配文件: {} (需要应用 {} 个配置变更)", 
                           entryName, changes.size());

                try {
                    // 读取文件内容
                    byte[] content = zip.getInputStream(entry).readAllBytes();
                    LOGGER.debug("[DatapackFileModifier] 文件 {} 原始内容长度: {} 字节", entryName, content.length);

                    // 写入临时文件进行修改
                    File tempFile = File.createTempFile("datapack-mod-", ".tmp");
                    Files.write(tempFile.toPath(), content);

                        // 应用所有配置变更到同一个文件
                        boolean fileModified = false;
                        for (ConfigChange change : changes) {
                            LOGGER.info("[DatapackFileModifier] 应用配置: {} = {} (JSON路径: {})", change.key, change.value, change.jsonKey);
                            
                            boolean success = modifyFile(tempFile, entryName, change.jsonKey, change.value, change.metadata);
                            if (success) {
                                fileModified = true;
                            } else {
                                LOGGER.warn("[DatapackFileModifier] 配置 {} 应用失败", change.key);
                            }
                        }

                    if (fileModified) {
                        modifiedCount++;
                        // 读取修改后的内容
                        byte[] newContent = Files.readAllBytes(tempFile.toPath());
                        filesToUpdate.put(entryName, newContent);
                        LOGGER.info("[DatapackFileModifier] 文件修改成功: {} (新大小: {} 字节)", 
                                   entryName, newContent.length);
                    } else {
                        skippedCount++;
                        LOGGER.warn("[DatapackFileModifier] 文件未被修改: {}", entryName);
                    }

                    // 删除临时文件
                    if (!tempFile.delete()) {
                        LOGGER.warn("[DatapackFileModifier] 临时文件删除失败: {}", tempFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOGGER.error("[DatapackFileModifier] 处理文件 {} 失败", entryName, e);
                    skippedCount++;
                }
            }

            LOGGER.info("[DatapackFileModifier] ZIP 文件扫描完成，共 {} 个条目，匹配 {} 个文件", 
                       totalEntries, matchedFiles);
            LOGGER.info("[DatapackFileModifier] 成功修改: {}, 跳过: {}", modifiedCount, skippedCount);

        } catch (IOException e) {
            LOGGER.error("[DatapackFileModifier] 读取 ZIP 数据包失败: {}", zipFile.getAbsolutePath(), e);
            return;
        }

        // 如果有文件需要修改，重新创建 ZIP 文件
        if (!filesToUpdate.isEmpty()) {
            LOGGER.info("[DatapackFileModifier] ========== 开始更新 ZIP 文件 ==========");

            // 创建临时 ZIP 文件
            File tempZipFile = null;
            try {
                tempZipFile = File.createTempFile("datapack-", ".zip");
                LOGGER.info("[DatapackFileModifier] 创建临时 ZIP 文件: {}", tempZipFile.getAbsolutePath());

                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(tempZipFile));
                     java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile)) {

                    java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                    int copiedCount = 0;
                    int updatedCount = 0;

                    while (entries.hasMoreElements()) {
                        java.util.zip.ZipEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        // 检查是否需要更新这个文件
                        if (filesToUpdate.containsKey(entryName)) {
                            // 写入修改后的内容
                            java.util.zip.ZipEntry newEntry = new java.util.zip.ZipEntry(entryName);
                            zos.putNextEntry(newEntry);
                            zos.write(filesToUpdate.get(entryName));
                            zos.closeEntry();
                            LOGGER.debug("[DatapackFileModifier] 已更新文件: {}", entryName);
                            updatedCount++;
                        } else {
                            // 复制原始文件
                            java.util.zip.ZipEntry newEntry = new java.util.zip.ZipEntry(entryName);
                            zos.putNextEntry(newEntry);

                            // 复制文件内容
                            try (java.io.InputStream is = zip.getInputStream(entry)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    zos.write(buffer, 0, len);
                                }
                            }
                            zos.closeEntry();
                            copiedCount++;
                        }
                    }

                    LOGGER.info("[DatapackFileModifier] ZIP 文件构建完成: 复制 {} 个文件, 更新 {} 个文件", copiedCount, updatedCount);
                }

                // 备份原始 ZIP 文件
                File backupFile = new File(zipFile.getAbsolutePath() + ".bak");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                java.nio.file.Files.copy(zipFile.toPath(), backupFile.toPath());
                LOGGER.info("[DatapackFileModifier] 已备份原文件: {}", backupFile.getAbsolutePath());

                // 删除原始 ZIP 文件
                if (!zipFile.delete()) {
                    LOGGER.error("[DatapackFileModifier] 删除原始 ZIP 文件失败: {}", zipFile.getAbsolutePath());
                    return;
                }
                LOGGER.info("[DatapackFileModifier] 已删除原始 ZIP 文件");

                // 重命名临时文件
                if (!tempZipFile.renameTo(zipFile)) {
                    LOGGER.error("[DatapackFileModifier] 重命名临时 ZIP 文件失败");
                    // 尝试恢复备份
                    if (backupFile.exists()) {
                        java.nio.file.Files.copy(backupFile.toPath(), zipFile.toPath());
                    }
                    return;
                }
                LOGGER.info("[DatapackFileModifier] 已替换 ZIP 文件: {}", zipFile.getAbsolutePath());

                // 删除备份
                backupFile.delete();
                LOGGER.info("[DatapackFileModifier] 已删除备份文件");

                LOGGER.info("[DatapackFileModifier] ========== ZIP 数据包配置应用完成 ==========");
                LOGGER.info("[DatapackFileModifier] 数据包: {}, 修改: {}, 跳过: {}", datapackId, modifiedCount, skippedCount);

            } catch (IOException e) {
                LOGGER.error("[DatapackFileModifier] 创建新 ZIP 文件失败", e);
            } finally {
                // 清理临时文件
                if (tempZipFile != null && tempZipFile.exists()) {
                    tempZipFile.delete();
                }
            }
        } else {
            LOGGER.warn("[DatapackFileModifier] ========== 没有文件需要修改 ==========");
            LOGGER.warn("[DatapackFileModifier] 可能的原因:");
            LOGGER.warn("[DatapackFileModifier] 1. ZIP 内的文件路径与 datapack-config.json 中定义的 filePath 不匹配");
            LOGGER.warn("[DatapackFileModifier] 2. 修改操作失败（请查看上方日志）");
        }
    }

    /**
     * 修改文件内容
     * @param targetFile 目标文件（datapack-config.json 中 filePath 指定的文件）
     * @param entryName ZIP 中的原始文件路径（用于判断文件类型）
     * @param key 配置键
     * @param value 用户修改后的值
     * @param metadata 配置元数据（包含类型、默认值等信息）
     * @return 是否成功修改
     */
    private static boolean modifyFile(File targetFile, String entryName, String key, String value,
                                     DatapackSettings.ConfigMetadata metadata) {
        LOGGER.debug("[DatapackFileModifier] ===== modifyFile 被调用 =====");
        LOGGER.debug("[DatapackFileModifier] 目标文件: {}", targetFile.getAbsolutePath());
        LOGGER.debug("[DatapackFileModifier] 文件存在: {}", targetFile.exists());
        LOGGER.debug("[DatapackFileModifier] 文件大小: {} 字节", targetFile.length());
        LOGGER.debug("[DatapackFileModifier] ZIP entry 路径: {}", entryName);
        LOGGER.debug("[DatapackFileModifier] 配置 key: {}", key);
        LOGGER.debug("[DatapackFileModifier] 配置 value: {}", value);

        String entryNameLower = entryName.toLowerCase();
        LOGGER.debug("[DatapackFileModifier] Entry 名称（小写）: {}", entryNameLower);

        // JSON 文件
        if (entryNameLower.endsWith(".json")) {
            LOGGER.debug("[DatapackFileModifier] 调用 modifyJsonFile");
            return modifyJsonFile(targetFile, key, value, metadata);
        }
        // 函数文件 (.mcfunction)
        else if (entryNameLower.endsWith(".mcfunction")) {
            LOGGER.debug("[DatapackFileModifier] 调用 modifyFunctionFile");
            return modifyFunctionFile(targetFile, key, value);
        }
        // 其他文本文件
        else {
            LOGGER.debug("[DatapackFileModifier] 调用 modifyTextFile");
            return modifyTextFile(targetFile, key, value);
        }
    }

    /**
     * 修改 JSON 文件
     * @param jsonFile JSON 文件（datapack-config.json 中 filePath 指定的文件）
     * @param key 配置键
     * @param value 用户修改后的值
     * @param metadata 配置元数据（包含 jsonPath 信息）
     * @return 是否成功修改
     */
    private static boolean modifyJsonFile(File jsonFile, String key, String value,
                                         DatapackSettings.ConfigMetadata metadata) {
        try {
            LOGGER.debug("[DatapackFileModifier] ===== 开始修改 JSON 文件 =====");
            LOGGER.debug("[DatapackFileModifier] 文件路径: {}", jsonFile.getAbsolutePath());
            LOGGER.debug("[DatapackFileModifier] 配置 key: {}", key);
            LOGGER.debug("[DatapackFileModifier] 配置 value: {}", value);

            // 读取 JSON 文件
            String jsonContent = Files.readString(jsonFile.toPath());
            LOGGER.debug("[DatapackFileModifier] 原始 JSON 内容: {}", jsonContent);

            JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();

            // 确定 JSON 路径：直接使用传入的 key（已经包含了嵌套路径）
            String jsonPath = key;
            LOGGER.debug("[DatapackFileModifier] JSON 路径: {}", jsonPath);

            // 如果 key 中包含 "/"，表示嵌套路径（如 "settings/spawnRate"）
            if (jsonPath.contains("/")) {
                String[] path = jsonPath.split("/");
                LOGGER.debug("[DatapackFileModifier] 嵌套路径分段: {}", (Object) path);

                // 使用 JsonElement 导航，支持对象属性与数组索引的混合路径
                // 例如 "argument/spline/points/1/location" 中 "1" 表示数组索引
                JsonElement currentElement = jsonObject;

                // 遍历路径直到最后一个元素
                for (int i = 0; i < path.length - 1; i++) {
                    String segment = path[i];
                    LOGGER.debug("[DatapackFileModifier] 处理路径层级 {}/{}: '{}'", i, path.length - 1, segment);

                    // 检测是否为数字（数组索引），如 "0"、"1"、"2" 等
                    if (segment.matches("\\d+")) {
                        int index = Integer.parseInt(segment);
                        LOGGER.debug("[DatapackFileModifier] 检测到数组索引: [{}]", index);
                        currentElement = currentElement.getAsJsonArray().get(index);
                    } else {
                        // 对象属性路径
                        JsonObject obj = currentElement.getAsJsonObject();
                        if (!obj.has(segment)) {
                            LOGGER.debug("[DatapackFileModifier] 路径 '{}' 不存在，创建新对象", segment);
                            obj.add(segment, new JsonObject());
                        }
                        currentElement = obj.get(segment);
                    }
                }

                // 设置最终值
                String finalKey = path[path.length - 1];
                LOGGER.debug("[DatapackFileModifier] 设置最终值: '{}' = '{}'", finalKey, value);
                
                // 检查最终键是否为数组索引
                if (finalKey.matches("\\d+")) {
                    int index = Integer.parseInt(finalKey);
                    JsonArray arr = currentElement.getAsJsonArray();
                    arr.set(index, createJsonValue(value));
                } else {
                    setJsonValue(currentElement.getAsJsonObject(), finalKey, value);
                }
            } else {
                // 直接设置值
                LOGGER.debug("[DatapackFileModifier] 直接设置值（非嵌套）: {} = {}", jsonPath, value);
                setJsonValue(jsonObject, jsonPath, value);
            }

            // 写回文件
            String newJsonContent = GSON.toJson(jsonObject);
            LOGGER.debug("[DatapackFileModifier] 修改后的 JSON: {}", newJsonContent);

            Files.writeString(jsonFile.toPath(), newJsonContent);
            LOGGER.debug("[DatapackFileModifier] JSON 文件已成功更新: {}", jsonFile.getAbsolutePath());
            LOGGER.debug("[DatapackFileModifier] ===== JSON 修改完成 =====");

            return true;
        } catch (com.google.gson.JsonSyntaxException e) {
            LOGGER.error("[DatapackFileModifier] JSON 语法错误: {}", jsonFile.getAbsolutePath(), e);
            return false;
        } catch (Exception e) {
            LOGGER.error("[DatapackFileModifier] 修改 JSON 文件失败: {}", jsonFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 设置 JSON 值（自动判断类型）
     * @param jsonObject JSON 对象
     * @param key 键
     * @param value 字符串值
     */
    private static void setJsonValue(JsonObject jsonObject, String key, String value) {
        // 尝试解析为布尔值
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            jsonObject.addProperty(key, Boolean.parseBoolean(value));
        }
        // 尝试解析为浮点数（需要在整数之前检查，以支持 .25、1.、1.25 等格式）
        else if (isDouble(value)) {
            jsonObject.addProperty(key, Double.parseDouble(value));
        }
        // 尝试解析为整数
        else if (value.matches("-?\\d+")) {
            jsonObject.addProperty(key, Integer.parseInt(value));
        }
        // 作为字符串
        else {
            jsonObject.addProperty(key, value);
        }
    }

    /**
     * 创建 JSON 原始值（自动判断类型）
     * 用于设置数组元素的值
     * @param value 字符串值
     * @return JsonPrimitive 对象
     */
    private static JsonPrimitive createJsonValue(String value) {
        // 尝试解析为布尔值
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        }
        // 尝试解析为浮点数
        else if (isDouble(value)) {
            return new JsonPrimitive(Double.parseDouble(value));
        }
        // 尝试解析为整数
        else if (value.matches("-?\\d+")) {
            return new JsonPrimitive(Integer.parseInt(value));
        }
        // 作为字符串
        else {
            return new JsonPrimitive(value);
        }
    }

    /**
     * 检查是否为浮点数
     * 支持：.25、0.25、1.、-0.5、-2.0 等格式
     */
    private static boolean isDouble(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            // 确保包含小数点
            return value.contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 修改函数文件
     * @param functionFile 函数文件（datapack-config.json 中 filePath 指定的文件）
     * @param key 配置键
     * @param value 用户修改后的值
     * @return 是否成功修改
     */
    private static boolean modifyFunctionFile(File functionFile, String key, String value) {
        try {
            // 读取文件内容
            List<String> lines = Files.readAllLines(functionFile.toPath());
            boolean modified = false;

            // 查找包含 key 的行并替换
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                // 匹配模式：set 配置键 值
                if (line.startsWith("set " + key + " ")) {
                    lines.set(i, "set " + key + " " + value);
                    modified = true;
                }
                // 匹配模式：# 配置键: 值（注释形式）
                else if (line.matches("#\\s*" + key + "\\s*:\\s*.*")) {
                    lines.set(i, "# " + key + ": " + value);
                    modified = true;
                }
            }

            if (modified) {
                Files.write(functionFile.toPath(), lines);
                return true;
            }

            return false;
        } catch (IOException e) {
            LOGGER.error("[DatapackFileModifier] 修改函数文件失败: {}", functionFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 修改普通文本文件
     * @param textFile 文本文件（datapack-config.json 中 filePath 指定的文件）
     * @param key 配置键
     * @param value 用户修改后的值
     * @return 是否成功修改
     */
    private static boolean modifyTextFile(File textFile, String key, String value) {
        try {
            // 读取文件内容
            List<String> lines = Files.readAllLines(textFile.toPath());
            boolean modified = false;

            // 查找包含 key 的行并替换
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                // 匹配模式：key=value
                if (line.startsWith(key + "=")) {
                    lines.set(i, key + "=" + value);
                    modified = true;
                }
                // 匹配模式：key: value
                else if (line.matches(key + "\\s*:\\s*.*")) {
                    lines.set(i, key + ": " + value);
                    modified = true;
                }
                // 匹配模式：# key: value（注释形式）
                else if (line.matches("#\\s*" + key + "\\s*:\\s*.*")) {
                    lines.set(i, "# " + key + ": " + value);
                    modified = true;
                }
            }

            if (modified) {
                Files.write(textFile.toPath(), lines);
                return true;
            }

            return false;
        } catch (IOException e) {
            LOGGER.error("[DatapackFileModifier] 修改文本文件失败: {}", textFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 获取数据包文件夹路径（支持文件夹和 ZIP 文件）
     * @param datapackId 数据包ID
     * @return 数据包文件夹或 ZIP 文件，如果不存在返回null
     */
    public static File getDatapackFolder(String datapackId) {
        // 从 packId 中提取数据包名称
        String packName = datapackId.replaceFirst("datapack_config_mod:", "");

        Path datapackFolderPath = DatapackConfig.getDatapackFolderPath();
        LOGGER.debug("[DatapackFileModifier] 数据包文件夹路径: {}", datapackFolderPath.toFile().getAbsolutePath());
        LOGGER.debug("[DatapackFileModifier] 查找数据包名称: {}", packName);

        // 先检查如果 packName 已经包含 .zip 后缀，直接使用
        File zipFile = new File(datapackFolderPath.toFile(), packName);
        if (zipFile.exists() && zipFile.isFile() && packName.endsWith(".zip")) {
            LOGGER.info("[DatapackFileModifier] 找到 ZIP 数据包: {}", zipFile.getAbsolutePath());
            return zipFile;
        }

        // 检查 ZIP 文件（添加 .zip 后缀）
        File zipFileWithExt = new File(datapackFolderPath.toFile(), packName + ".zip");
        if (zipFileWithExt.exists() && zipFileWithExt.isFile()) {
            LOGGER.info("[DatapackFileModifier] 找到 ZIP 数据包: {}", zipFileWithExt.getAbsolutePath());
            return zipFileWithExt;
        }

        // 再检查文件夹
        File folderFile = new File(datapackFolderPath.toFile(), packName);
        if (folderFile.exists() && folderFile.isDirectory()) {
            LOGGER.info("[DatapackFileModifier] 找到文件夹数据包: {}", folderFile.getAbsolutePath());
            return folderFile;
        }

        LOGGER.warn("[DatapackFileModifier] 数据包不存在（既不是文件夹也不是 ZIP 文件）: {}", packName);
        LOGGER.warn("[DatapackFileModifier] 已尝试的路径: {} 和 {}", zipFile.getAbsolutePath(), zipFileWithExt.getAbsolutePath());
        return null;
    }

    /**
     * 配置变更辅助类（用于批量处理同一文件的多个配置变更）
     */
    private static class ConfigChange {
        final String key;
        final String value;
        final DatapackSettings.ConfigMetadata metadata;
        final String jsonKey;  // 存储解析后的JSON路径（如 "argument1/argument/xz_scale"）

        ConfigChange(String key, String value, DatapackSettings.ConfigMetadata metadata, String jsonKey) {
            this.key = key;
            this.value = value;
            this.metadata = metadata;
            this.jsonKey = jsonKey;
        }
    }
}
