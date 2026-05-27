package com.aichen.datapackconfigmod.parser;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ZIP文件检查器，用于调试ZIP文件内部结构
 */
public class ZipFileInspector {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 检查ZIP文件内部结构
     * @param zipFile ZIP文件
     */
    public static void inspectZipFile(File zipFile) {
        if (!zipFile.exists() || !zipFile.isFile() || !zipFile.getName().endsWith(".zip")) {
            LOGGER.error("[DatapackConfigMod] 无效的ZIP文件: {}", zipFile.getAbsolutePath());
            return;
        }
        
        try (ZipFile zip = new ZipFile(zipFile)) {
            LOGGER.info("[DatapackConfigMod] 检查ZIP文件结构: {}", zipFile.getName());
            LOGGER.info("[DatapackConfigMod] ZIP文件大小: {} 字节", zipFile.length());
            
            Enumeration<? extends ZipEntry> entries = zip.entries();
            boolean hasEntries = false;
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                hasEntries = true;
                LOGGER.info("[DatapackConfigMod] ZIP条目: {} (大小: {} 字节, 压缩后: {} 字节)", 
                    entry.getName(), entry.getSize(), entry.getCompressedSize());
            }
            
            if (!hasEntries) {
                LOGGER.warn("[DatapackConfigMod] ZIP文件为空: {}", zipFile.getName());
            }
            
        } catch (IOException e) {
            LOGGER.error("[DatapackConfigMod] 检查ZIP文件失败: {}", zipFile.getName(), e);
        }
    }
    
    /**
     * 检查ZIP文件是否包含配置文件
     * @param zipFile ZIP文件
     * @return 是否包含配置文件
     */
    public static boolean hasConfigFile(File zipFile) {
        if (!zipFile.exists() || !zipFile.isFile() || !zipFile.getName().endsWith(".zip")) {
            return false;
        }
        
        String[] configFileNames = {"datapack-config.json", "datapack_config.json"};
        
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                for (String configFileName : configFileNames) {
                    if (entry.getName().equals(configFileName) || 
                        entry.getName().endsWith("/" + configFileName)) {
                        LOGGER.debug("[DatapackConfigMod] 找到配置文件: {} 在 {}", configFileName, zipFile.getName());
                        return true;
                    }
                }
            }
            
        } catch (IOException e) {
            LOGGER.error("[DatapackConfigMod] 检查ZIP文件配置失败: {}", zipFile.getName(), e);
        }
        
        return false;
    }
}