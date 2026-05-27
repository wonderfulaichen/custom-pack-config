package com.aichen.datapackconfigmod.util;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存管理器
 * 参考东方灵梦模组的缓存机制，减少重复计算和资源加载
 */
public class CacheManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 配置缓存 - 使用LRU缓存策略，最大缓存1000个条目
    private static final Map<String, Object> configCache = new LinkedHashMap<String, Object>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > 1000; // LRU淘汰策略
        }
    };
    
    // 数据包设置缓存
    private static final Map<String, Object> datapackSettingsCache = new ConcurrentHashMap<>();
    
    // ZIP文件系统缓存，避免重复打开ZIP文件
    private static final Map<String, Object> zipFileSystemCache = new ConcurrentHashMap<>();
    
    // 解析结果缓存，缓存解析后的JSON对象
    private static final Map<String, Object> parsedConfigCache = new ConcurrentHashMap<>();
    
    // 缓存有效期（毫秒）
    private static final long CACHE_EXPIRY_MS = 30000; // 30秒
    
    // 缓存时间戳
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // 缓存锁，用于同步操作
    private static final ReentrantLock cacheLock = new ReentrantLock();
    
    /**
     * 获取配置缓存（线程安全）
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    public static Object getConfigCache(String key) {
        cacheLock.lock();
        try {
            if (isCacheExpired(key)) {
                configCache.remove(key);
                cacheTimestamps.remove(key);
                return null;
            }
            return configCache.get(key);
        } finally {
            cacheLock.unlock();
        }
    }
    
    /**
     * 设置配置缓存（线程安全）
     * @param key 缓存键
     * @param value 缓存值
     */
    public static void setConfigCache(String key, Object value) {
        cacheLock.lock();
        try {
            configCache.put(key, value);
            cacheTimestamps.put(key, System.currentTimeMillis());
            LOGGER.debug("[DatapackConfigMod] 缓存已更新: {}", key);
        } finally {
            cacheLock.unlock();
        }
    }
    
    /**
     * 获取数据包设置缓存
     * @param datapackId 数据包ID
     * @param settingKey 设置键
     * @return 缓存值
     */
    public static Object getDatapackSettingCache(String datapackId, String settingKey) {
        String cacheKey = datapackId + "::" + settingKey;
        return getConfigCache(cacheKey);
    }
    
    /**
     * 设置数据包设置缓存
     * @param datapackId 数据包ID
     * @param settingKey 设置键
     * @param value 缓存值
     */
    public static void setDatapackSettingCache(String datapackId, String settingKey, Object value) {
        String cacheKey = datapackId + "::" + settingKey;
        setConfigCache(cacheKey, value);
    }
    
    /**
     * 检查缓存是否过期
     * @param key 缓存键
     * @return 是否过期
     */
    private static boolean isCacheExpired(String key) {
        Long timestamp = cacheTimestamps.get(key);
        if (timestamp == null) {
            return true;
        }
        return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
    }
    
    /**
     * 获取ZIP文件系统缓存
     * @param filePath ZIP文件路径
     * @return ZIP文件系统对象，如果不存在则返回null
     */
    public static Object getZipFileSystemCache(String filePath) {
        String cacheKey = "zip::" + filePath;
        return getConfigCache(cacheKey);
    }
    
    /**
     * 设置ZIP文件系统缓存
     * @param filePath ZIP文件路径
     * @param fileSystem ZIP文件系统对象
     */
    public static void setZipFileSystemCache(String filePath, Object fileSystem) {
        String cacheKey = "zip::" + filePath;
        setConfigCache(cacheKey, fileSystem);
    }
    
    /**
     * 获取解析结果缓存
     * @param configPath 配置文件路径
     * @return 解析后的JSON对象，如果不存在则返回null
     */
    public static Object getParsedConfigCache(String configPath) {
        String cacheKey = "parsed::" + configPath;
        return getConfigCache(cacheKey);
    }
    
    /**
     * 设置解析结果缓存
     * @param configPath 配置文件路径
     * @param parsedObject 解析后的JSON对象
     */
    public static void setParsedConfigCache(String configPath, Object parsedObject) {
        String cacheKey = "parsed::" + configPath;
        setConfigCache(cacheKey, parsedObject);
    }
    
    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        cacheLock.lock();
        try {
            configCache.clear();
            datapackSettingsCache.clear();
            zipFileSystemCache.clear();
            parsedConfigCache.clear();
            cacheTimestamps.clear();
            LOGGER.info("[DatapackConfigMod] 所有缓存已清除");
        } finally {
            cacheLock.unlock();
        }
    }
    
    /**
     * 清除指定数据包的缓存
     * @param datapackId 数据包ID
     */
    public static void clearDatapackCache(String datapackId) {
        // 清除所有以datapackId开头的缓存
        configCache.keySet().removeIf(key -> key.startsWith(datapackId + "::"));
        cacheTimestamps.keySet().removeIf(key -> key.startsWith(datapackId + "::"));
        LOGGER.debug("[DatapackConfigMod] 数据包缓存已清除: {}", datapackId);
    }
}