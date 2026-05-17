package com.aichen.datapackconfigmod.manager;

import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
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
    public PackResources open(String id) {
        try {
            return new FilePackResources(id, datapackFile, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create pack resources for " + id, e);
        }
    }
}
