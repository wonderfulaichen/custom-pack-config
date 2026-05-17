package com.aichen.datapackconfigmod.manager;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据包设置管理器
 * 管理所有数据包的配置设置
 */
public class DatapackSettingsManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 配置文件路径
    private static final String SETTINGS_FILE = "datapack-settings.json";
    
    // 数据包配置映射（数据包ID -> 配置）- 改为线程安全的ConcurrentHashMap
    private static Map<String, DatapackSettings> settingsMap = new ConcurrentHashMap<>();
    
    // 延迟保存机制
    private static volatile boolean dirty = false;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long SAVE_DELAY_MS = 500; // 500毫秒延迟保存
    
    /**
     * 加载所有数据包设置
     */
    public static void loadSettings() {
        Path settingsFile = getSettingsFilePath();
        
        // 如果文件不存在，创建空文件
        if (!Files.exists(settingsFile)) {
            saveSettings();
            return;
        }
        
        try {
            FileReader reader = new FileReader(settingsFile.toFile());
            Type type = new TypeToken<Map<String, DatapackSettings>>() {}.getType();
            Map<String, DatapackSettings> loadedSettings = GSON.fromJson(reader, type);
            reader.close();
            
            if (loadedSettings != null) {
                // 过滤掉文件夹版本的数据包设置（只保留.zip文件）
                Map<String, DatapackSettings> filteredSettings = new HashMap<>();
                boolean hasFolderDatapacks = false;
                
                for (Map.Entry<String, DatapackSettings> entry : loadedSettings.entrySet()) {
                    String datapackId = entry.getKey();
                    // 只保留以.zip结尾的数据包ID
                    if (datapackId.toLowerCase().endsWith(".zip")) {
                        filteredSettings.put(datapackId, entry.getValue());
                    } else {
                        LOGGER.info("[DatapackConfigMod] 过滤掉文件夹版本数据包: {}", datapackId);
                        hasFolderDatapacks = true;
                    }
                }
                
                settingsMap = filteredSettings;
                
                // 如果过滤掉了文件夹版本的数据包，重新保存配置文件
                if (hasFolderDatapacks) {
                    LOGGER.info("[DatapackConfigMod] 检测到文件夹版本数据包，重新保存过滤后的设置");
                    saveSettings();
                }
                
                if (DataPackConfigMod.LOGGER.isInfoEnabled()) {
                    LOGGER.info("[DatapackConfigMod] 已加载 {} 个数据包设置（过滤后）", settingsMap.size());
                    // 调试：打印每个数据包的启用状态
                    for (Map.Entry<String, DatapackSettings> entry : settingsMap.entrySet()) {
                        LOGGER.debug("[DatapackConfigMod] 数据包 {} 启用状态: {}", 
                            entry.getKey(), entry.getValue().isEnabled());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("[DatapackConfigMod] 加载数据包设置失败", e);
            settingsMap = new HashMap<>();
        }
    }
    
    /**
     * 立即保存所有数据包设置
     * 修复：确保立即刷新到磁盘
     * 优化：移除重复的JSON序列化
     */
    public static void saveSettings() {
        saveSettingsImmediately();
    }
    
    /**
     * 立即保存所有数据包设置（内部方法）
     */
    private static void saveSettingsImmediately() {
        Path settingsFile = getSettingsFilePath();

        try {
            // 确保父目录存在
            Files.createDirectories(settingsFile.getParent());

            // 直接序列化并写入，避免重复序列化
            FileWriter writer = new FileWriter(settingsFile.toFile());
            GSON.toJson(settingsMap, writer);
            writer.flush();  // 修复：强制刷新缓冲区
            writer.close();

            if (DataPackConfigMod.LOGGER.isTraceEnabled()) {
                LOGGER.trace("[DatapackConfigMod] 已保存 {} 个数据包设置", settingsMap.size());
            }
        } catch (IOException e) {
            LOGGER.error("[DatapackConfigMod] 保存数据包设置失败", e);
        }
    }
    
    /**
     * 延迟保存数据包设置
     */
    private static void scheduleDelayedSave() {
        dirty = true;
        
        // 取消之前的保存任务
        scheduler.schedule(() -> {
            if (dirty) {
                saveSettingsImmediately();
                dirty = false;
            }
        }, SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 添加或更新数据包设置
     * @param settings 数据包设置
     */
    public static void addOrUpdateSetting(DatapackSettings settings) {
        settingsMap.put(settings.getDatapackId(), settings);
        scheduleDelayedSave();
    }
    
    /**
     * 获取数据包设置
     * @param datapackId 数据包ID
     * @return 数据包设置，如果不存在返回null
     */
    public static DatapackSettings getSetting(String datapackId) {
        return settingsMap.get(datapackId);
    }
    
    /**
     * 获取所有数据包设置
     * @return 所有数据包设置列表
     */
    public static Map<String, DatapackSettings> getAllSettings() {
        return new HashMap<>(settingsMap);
    }
    
    /**
     * 获取启用的数据包ID列表
     * @return 启用的数据包ID列表
     */
    public static List<String> getEnabledDatapackIds() {
        List<String> enabledIds = new ArrayList<>();
        
        for (DatapackSettings setting : settingsMap.values()) {
            if (setting.isEnabled()) {
                enabledIds.add(setting.getDatapackId());
            }
        }
        
        return enabledIds;
    }
    
    /**
     * 设置数据包启用状态
     * @param datapackId 数据包ID
     * @param enabled 是否启用
     */
    public static void setDatapackEnabled(String datapackId, boolean enabled) {
        DatapackSettings settings = settingsMap.get(datapackId);
        
        if (settings != null) {
            settings.setEnabled(enabled);
            scheduleDelayedSave();
            
            if (DataPackConfigMod.LOGGER.isInfoEnabled()) {
                LOGGER.info("[DatapackConfigMod] 数据包 {} 状态设置为: {}", datapackId, enabled);
            }
        }
    }
    
    /**
     * 设置数据包自定义值
     * @param datapackId 数据包ID
     * @param key 键
     * @param value 值
     */
    public static void setCustomValue(String datapackId, String key, String value) {
        DatapackSettings settings = settingsMap.get(datapackId);
        
        if (settings != null) {
            settings.addCustomValue(key, value);
            scheduleDelayedSave();
        }
    }
    
    /**
     * 移除数据包设置
     * @param datapackId 数据包ID
     */
    public static void removeSetting(String datapackId) {
        settingsMap.remove(datapackId);
        scheduleDelayedSave();
    }
    
    /**
     * 检查是否存在设置
     * @param datapackId 数据包ID
     * @return 是否存在
     */
    public static boolean hasSetting(String datapackId) {
        return settingsMap.containsKey(datapackId);
    }
    
    /**
     * 获取配置文件路径
     * @return 配置文件路径
     */
    private static Path getSettingsFilePath() {
        return Paths.get("config", "datapack-config-mod", SETTINGS_FILE);
    }
    
    /**
     * 清空所有设置
     */
    public static void clearSettings() {
        settingsMap.clear();
        scheduleDelayedSave();
    }
    
    /**
     * 强制立即保存（用于应用退出时）
     */
    public static void forceSave() {
        saveSettingsImmediately();
        dirty = false;
    }
}
