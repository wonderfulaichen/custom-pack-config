package com.aichen.datapackconfigmod.manager;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;

import java.io.File;

/**
 * 文件数据包资源提供者
 * 用于从文件或文件夹加载数据包
 */
public class FilePackSupplier implements Pack.ResourcesSupplier {
    
    private final File datapackFile;
    
    public FilePackSupplier(File datapackFile) {
        this.datapackFile = datapackFile;
    }
    
    @Override
    public PackResources openPrimary(String id) {
        try {
            // 1.20.4: SharedZipFileAccess 是 private 内部类，无法直接创建
            // 改用 PathPackResources (接受 Path 参数，public 构造器)
            return new PathPackResources(id, datapackFile.toPath(), false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create pack resources for " + id, e);
        }
    }
    
    @Override
    public PackResources openFull(String id, Pack.Info info) {
        return openPrimary(id);
    }
}
