package com.aichen.datapackconfigmod.manager;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.DatapackConfig;
import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.aichen.datapackconfigmod.parser.DatapackConfigParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Info;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.util.InclusiveRange;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据包管理器
 * 负责管理全局数据包的加载和卸载
 */
@Mod.EventBusSubscriber(modid = DataPackConfigMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DatapackManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 当添加包查找器时触发
     * 在此处注册全局数据包
     * @param event 事件
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        
        // 只处理数据包类型
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        
        // 先加载最新的配置（修复：确保加载用户修改后的配置）
        DatapackSettingsManager.loadSettings();
        
        // 获取数据包文件夹
        Path datapackFolder = DatapackConfig.getDatapackFolderPath();
        File folder = datapackFolder.toFile();
        
        // 检查文件夹是否存在
        if (!folder.exists()) {
            if (DatapackConfig.shouldLogDatapackLoading()) {
                LOGGER.info("[DatapackConfigMod] 数据包文件夹不存在，将创建: {}", folder.getAbsolutePath());
            }
            try {
                Files.createDirectories(datapackFolder);
            } catch (IOException e) {
                LOGGER.error("[DatapackConfigMod] 创建数据包文件夹失败: {}", e.getMessage());
                return;
            }
        }
        
        // 检查是否是目录
        if (!folder.isDirectory()) {
            LOGGER.error("[DatapackConfigMod] 数据包路径不是一个目录: {}", folder.getAbsolutePath());
            return;
        }
        
        // 扫描文件夹中的数据包
        List<File> datapackFiles = scanDatapacks(folder);
        
        if (datapackFiles.isEmpty()) {
            if (DatapackConfig.shouldLogDatapackLoading()) {
                LOGGER.info("[DatapackConfigMod] 未在 {} 找到数据包", folder.getAbsolutePath());
            }
            return;
        }
        
        // 加载数据包
        loadDatapacks(datapackFiles, event);
    }
    
    /**
     * 扫描数据包文件夹，查找有效的数据包
     * 修改：只扫描.zip文件，忽略文件夹
     * @param folder 数据包文件夹
     * @return 数据包文件列表
     */
    private static List<File> scanDatapacks(File folder) {
        List<File> datapacks = new ArrayList<>();
        
        File[] files = folder.listFiles();
        if (files == null) {
            return datapacks;
        }
        
        // 修改：只收集.zip文件，不再进行去重处理
        for (File file : files) {
            if (isValidDatapack(file)) {
                // 只添加.zip文件
                datapacks.add(file);
                
                if (DatapackConfig.shouldLogDatapackLoading()) {
                    LOGGER.debug("[DatapackConfigMod] 找到数据包: {}", file.getName());
                }
            }
        }
        
        if (DatapackConfig.shouldLogDatapackLoading()) {
            LOGGER.info("[DatapackConfigMod] 扫描完成，找到 {} 个.zip数据包", datapacks.size());
        }
        
        return datapacks;
    }
    
    /**
     * 检查文件是否是有效的数据包
     * 修改：只支持.zip文件格式的数据包，忽略文件夹
     * @param file 要检查的文件
     * @return 是否是有效数据包
     */
    private static boolean isValidDatapack(File file) {
        // 修改：只检查是否是.zip文件，忽略文件夹
        if (file.isFile() && file.getName().endsWith(".zip")) {
            return true;
        }
        
        // 文件夹返回false，不加载
        return false;
    }
    
    /**
     * 加载数据包
     * @param datapackFiles 数据包文件列表
     * @param event AddPackFindersEvent 事件
     */
    private static void loadDatapacks(List<File> datapackFiles, AddPackFindersEvent event) {
        for (File datapackFile : datapackFiles) {
            try {
                // 使用原始数据包名称，避免ID不一致问题
                String packName = datapackFile.getName();
                String packId = DataPackConfigMod.MODID + ":" + packName;
                
                // 解析数据包配置（新增）
                parseAndStoreDatapackConfig(datapackFile, packId);
                
                // 检查数据包是否启用（新增）
                if (!isDatapackEnabled(packId)) {
                    if (DatapackConfig.shouldLogDatapackLoading()) {
                        LOGGER.info("[DatapackConfigMod] 数据包已禁用，跳过加载: {}", packId);
                    }
                    continue;
                }
                
                // 创建资源包提供者
                Pack.ResourcesSupplier supplier = new FilePackSupplier(datapackFile);
                
                // 创建 Pack 实例（fixed=true 表示自动加载到右边）
                Pack pack = Pack.readMetaAndCreate(
                    packId,
                    Component.literal(packName),
                    true,  // true = 自动加载（固定位置）
                    supplier,
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.FEATURE
                );
                
                // 修复：如果 Pack.readMetaAndCreate 返回 null（对于文件夹形式的数据包可能发生），手动创建 Pack
                if (pack == null) {
                    if (datapackFile.isDirectory()) {
                        // 对于文件夹形式的数据包，手动读取 pack.mcmeta 并创建 Pack
                        LOGGER.debug("[DatapackConfigMod] Pack.readMetaAndCreate 返回 null，尝试手动创建 Pack");
                        pack = createPackFromDirectory(packId, packName, datapackFile, supplier);
                    }
                }
                
                // 修复：创建 final 变量供 lambda 使用
                final Pack finalPack = pack;
                
                if (finalPack != null) {
                    event.addRepositorySource((consumer) -> consumer.accept(finalPack));
                    
                    if (DatapackConfig.shouldLogDatapackLoading()) {
                        LOGGER.info("[DatapackConfigMod] 已加载全局数据包: {} (ID: {})", datapackFile.getName(), packId);
                    }
                } else {
                    if (DatapackConfig.shouldLogDatapackLoading()) {
                        LOGGER.warn("[DatapackConfigMod] 无法加载数据包: {} (ID: {})", datapackFile.getName(), packId);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[DatapackConfigMod] 加载数据包失败: {}", datapackFile.getName(), e);
            }
        }
    }
    
    /**
     * 从文件夹手动创建 Pack 实例（修复：处理 Pack.readMetaAndCreate 返回 null 的情况）
     */
    private static Pack createPackFromDirectory(String packId, String packName, File datapackFile, Pack.ResourcesSupplier supplier) {
        try {
            // 读取 pack.mcmeta 文件
            File packMetaFile = new File(datapackFile, "pack.mcmeta");
            if (!packMetaFile.exists()) {
                LOGGER.warn("[DatapackConfigMod] 数据包缺少 pack.mcmeta 文件: {}", datapackFile.getName());
                return null;
            }
            
            // 解析 pack.mcmeta
            String metaContent = Files.readString(packMetaFile.toPath());
            
            // 手动创建 PackMetadataSection
            // 使用默认值：pack_format = 15, description = packName
            String description = packName;
            int packFormat = 15;
            
            try {
                // 尝试从 JSON 中提取实际的 pack_format 和 description
                com.google.gson.JsonObject metaJson = com.google.gson.JsonParser.parseString(metaContent).getAsJsonObject();
                if (metaJson.has("pack")) {
                    com.google.gson.JsonObject packObj = metaJson.getAsJsonObject("pack");
                    if (packObj.has("pack_format")) {
                        packFormat = packObj.get("pack_format").getAsInt();
                    }
                    if (packObj.has("description")) {
                        description = packObj.get("description").getAsString();
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[DatapackConfigMod] 解析 pack.mcmeta 失败，使用默认值: {}", e.getMessage());
            }
            
            // 创建 Pack.Info (1.20.4 需要 PackCompatibility.forVersion(InclusiveRange, int))
            Info info = new Info(
                Component.literal(description),
                PackCompatibility.forVersion(new net.minecraft.util.InclusiveRange<>(packFormat, packFormat), packFormat),
                net.minecraft.world.flag.FeatureFlagSet.of(),  // 默认空的 FeatureFlagSet
                java.util.List.of()  // 默认空列表
            );
            
            // 手动创建 Pack 实例 (1.20.4 不再需要 PackType 参数)
            Pack pack = Pack.create(
                packId,
                Component.literal(packName),
                true,  // fixed = true
                supplier,
                info,
                Pack.Position.TOP,
                false,  // hidden = false
                PackSource.FEATURE
            );
            
            if (pack != null) {
                LOGGER.debug("[DatapackConfigMod] 手动创建 Pack 成功: {}", packId);
            }
            
            return pack;
            
        } catch (Exception e) {
            LOGGER.error("[DatapackConfigMod] 手动创建 Pack 失败: {}", packId, e);
            return null;
        }
    }
    
    /**
     * 解析并存储数据包配置（修复：允许重新解析已存在的设置，但保留用户自定义值）
     * @param datapackFile 数据包文件
     * @param packId 数据包ID
     */
    private static void parseAndStoreDatapackConfig(File datapackFile, String packId) {
        try {
            // 解析数据包配置
            var newSettings = DatapackConfigParser.parseConfig(datapackFile, packId);
            
            // 如果解析到配置，保存到管理器
            if (newSettings != null) {
                // 检查是否已存在设置
                var existingSettings = DatapackSettingsManager.getSetting(packId);
                
                if (existingSettings != null) {
                    // 合并现有设置：保留用户的启用状态和自定义值
                    newSettings.setEnabled(existingSettings.isEnabled());
                    
                    // 如果已有自定义值，合并它们
                    if (!existingSettings.getCustomValues().isEmpty()) {
                        for (var entry : existingSettings.getCustomValues().entrySet()) {
                            String key = entry.getKey();
                            // 只保留新配置中存在的配置项（通过检查元数据是否存在）
                            if (newSettings.getConfigMetadata(key) != null) {
                                newSettings.addCustomValue(key, entry.getValue());
                            }
                        }
                        LOGGER.info("[DatapackManager] 合并了 {} 个现有自定义值", existingSettings.getCustomValues().size());
                    } else {
                        // 如果没有自定义值，从配置文件读取实际值
                        LOGGER.info("[DatapackManager] 现有设置没有自定义值，从配置文件读取实际值");
                        readActualConfigValues(datapackFile, newSettings);
                    }
                } else {
                    // 首次加载：从配置文件读取实际值
                    readActualConfigValues(datapackFile, newSettings);
                }
                
                DatapackSettingsManager.addOrUpdateSetting(newSettings);
                
                if (DatapackConfig.shouldLogDatapackLoading()) {
                    LOGGER.info("[DatapackConfigMod] 已解析数据包配置: {} ({} 个自定义值)", 
                        packId, newSettings.getCustomValues().size());
                }
            } else {
                // 如果没有配置文件，创建默认设置（但保留现有设置）
                var existingSettings = DatapackSettingsManager.getSetting(packId);
                if (existingSettings == null) {
                    var defaultSettings = new com.aichen.datapackconfigmod.config.model.DatapackSettings(
                        packId, 
                        datapackFile.getName()
                    );
                    defaultSettings.setEnabled(true);
                    DatapackSettingsManager.addOrUpdateSetting(defaultSettings);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[DatapackConfigMod] 解析数据包配置失败: {}", packId, e);
        }
    }
    
    /**
     * 从配置文件中读取实际值并更新 customValues
     * 优先使用配置文件中的值，如果读取失败则使用默认值
     */
    private static void readActualConfigValues(File datapackFile, DatapackSettings settings) {
        LOGGER.info("[DatapackManager] 正在从配置文件读取实际值: {}", settings.getDatapackId());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String key : settings.getAllConfigMetadata().keySet()) {
            var metadata = settings.getConfigMetadata(key);
            String filePath = metadata.getFilePath();
            String jsonKey = settings.getFilePathMapping(key + ".json_key");
            if (jsonKey == null) jsonKey = key;
            
            if (filePath != null) {
                try {
                    // 读取配置文件中的实际值
                    LOGGER.debug("[DatapackManager] 尝试读取配置: key={}, filePath={}, jsonKey={}", key, filePath, jsonKey);
                    String actualValue = readValueFromConfigFile(datapackFile, filePath, jsonKey);
                    if (actualValue != null) {
                        settings.addCustomValue(key, actualValue);
                        LOGGER.info("[DatapackManager] ✓ 成功读取: {} = {} (文件: {}, key: {})", 
                            key, actualValue, filePath, jsonKey);
                        successCount++;
                    } else {
                        // 读取失败，使用默认值
                        String defaultValue = metadata.getDefaultValue();
                        if (defaultValue != null) {
                            settings.addCustomValue(key, defaultValue);
                            LOGGER.warn("[DatapackManager] ✗ 读取失败，使用默认值: {} = {} (文件: {}, key: {})", 
                                key, defaultValue, filePath, jsonKey);
                            failCount++;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("[DatapackManager] ✗ 读取异常，使用默认值: {} = {} (文件: {}, key: {})", 
                        key, metadata.getDefaultValue(), filePath, jsonKey, e);
                    // 出错时使用默认值
                    if (metadata.getDefaultValue() != null) {
                        settings.addCustomValue(key, metadata.getDefaultValue());
                        failCount++;
                    }
                }
            } else {
                LOGGER.warn("[DatapackManager] ✗ 配置项没有 filePath: key={}", key);
                failCount++;
            }
        }
        
        LOGGER.info("[DatapackManager] 读取完成: 成功={}, 失败={}, 总计={}", successCount, failCount, settings.getAllConfigMetadata().size());
    }
    
    /**
     * 从配置文件中读取指定键的值（支持文件夹和 ZIP 格式）
     * @param datapackFile 数据包文件或文件夹
     * @param filePath 配置文件路径
     * @param jsonKey JSON键路径（支持嵌套，如 "theme/color"）
     * @return 配置值，如果读取失败返回null
     */
    private static String readValueFromConfigFile(File datapackFile, String filePath, String jsonKey) {
        try {
            LOGGER.debug("[DatapackManager] 开始读取配置文件值: datapack={}, filePath={}, jsonKey={}", 
                datapackFile.getName(), filePath, jsonKey);
            
            String jsonContent = null;
            
            // 检查是否为 ZIP 文件
            if (datapackFile.isFile() && datapackFile.getName().endsWith(".zip")) {
                LOGGER.debug("[DatapackManager] 检测到 ZIP 文件: {}", datapackFile.getName());
                // 从 ZIP 文件中读取
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(datapackFile)) {
                    String zipPath = filePath.replace("\\", "/");
                    LOGGER.debug("[DatapackManager] 查找 ZIP entry: {}", zipPath);
                    java.util.zip.ZipEntry entry = zip.getEntry(zipPath);
                    if (entry == null) {
                        LOGGER.warn("[DatapackManager] ✗ ZIP 中找不到配置文件: {}", filePath);
                        return null;
                    }
                    
                    LOGGER.debug("[DatapackManager] ✓ 找到 ZIP entry: {} (大小: {} bytes)", entry.getName(), entry.getSize());
                    try (java.io.InputStream is = zip.getInputStream(entry)) {
                        jsonContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        LOGGER.debug("[DatapackManager] ✓ 成功读取 ZIP 内容: {} bytes", jsonContent.length());
                    }
                }
            } else {
                // 文件夹格式
                File configFile = new File(datapackFile, filePath);
                if (!configFile.exists()) {
                    LOGGER.debug("[DatapackManager] ✗ 配置文件不存在: {}", configFile.getAbsolutePath());
                    return null;
                }
                
                jsonContent = java.nio.file.Files.readString(configFile.toPath());
                LOGGER.debug("[DatapackManager] ✓ 成功读取文件内容: {} bytes", jsonContent.length());
            }
            
            if (jsonContent == null) {
                LOGGER.warn("[DatapackManager] ✗ jsonContent 为 null");
                return null;
            }
            
            // 解析JSON
            LOGGER.debug("[DatapackManager] 解析 JSON 内容...");
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonContent).getAsJsonObject();
            
            // 解析嵌套键（如 "theme/color"）
            String[] keys = jsonKey.split("/");
            com.google.gson.JsonElement currentElement = jsonObject;
            
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                LOGGER.debug("[DatapackManager] 查找 JSON 键: {} (第 {} 层)", key, i + 1);
                if (currentElement.isJsonObject() && currentElement.getAsJsonObject().has(key)) {
                    currentElement = currentElement.getAsJsonObject().get(key);
                    LOGGER.debug("[DatapackManager] ✓ 找到键: {} = {}", key, currentElement);
                    
                    // 如果是最后一个键，返回值
                    if (i == keys.length - 1) {
                        String value = currentElement.getAsString();
                        LOGGER.debug("[DatapackManager] ✓ 成功获取值: {}", value);
                        return value;
                    }
                } else {
                    LOGGER.warn("[DatapackManager] ✗ JSON键不存在: {} (在 {} 中)", key, jsonKey);
                    return null;
                }
            }
            
            LOGGER.warn("[DatapackManager] ✗ 未找到最终值");
            return null;
        } catch (Exception e) {
            LOGGER.error("[DatapackManager] ✗ 读取配置文件值失败: 文件={}, 键={}", filePath, jsonKey, e);
            return null;
        }
    }
    
    /**
     * 检查数据包是否启用（新增）
     * @param packId 数据包ID
     * @return 是否启用
     */
    private static boolean isDatapackEnabled(String packId) {
        var settings = DatapackSettingsManager.getSetting(packId);
        return settings != null && settings.isEnabled();
    }
    
    /**
     * 获取数据包文件夹路径
     * @return 数据包文件夹路径
     */
    public static Path getDatapackFolder() {
        return DatapackConfig.getDatapackFolderPath();
    }
    
    /**
     * 确保数据包文件夹存在
     */
    public static void ensureDatapackFolderExists() {
        Path folder = getDatapackFolder();
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
                if (DatapackConfig.shouldLogDatapackLoading()) {
                    LOGGER.info("[DatapackConfigMod] 已创建数据包文件夹: {}", folder.toAbsolutePath());
                }
            } catch (IOException e) {
                LOGGER.error("[DatapackConfigMod] 创建数据包文件夹失败: {}", e.getMessage());
            }
        }
    }
}