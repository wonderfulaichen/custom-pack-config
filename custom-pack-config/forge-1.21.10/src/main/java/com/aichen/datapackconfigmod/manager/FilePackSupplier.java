package com.aichen.datapackconfigmod.manager;

import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.repository.Pack;

import java.io.File;

/**
 * 文件数据包资源提供者工厂
 *
 * 使用 FilePackResources.FileResourcesSupplier（MC 1.21.10 标准实现）
 * 该类是 FilePackResources 的 public 内部类，专门用于从 ZIP 文件创建 Pack.ResourcesSupplier
 */
public class FilePackSupplier {

    private final File datapackFile;

    public FilePackSupplier(File datapackFile) {
        this.datapackFile = datapackFile;
    }

    /**
     * 创建 ZIP 文件的 ResourcesSupplier
     */
    public Pack.ResourcesSupplier createSupplier() {
        return new FilePackResources.FileResourcesSupplier(datapackFile.toPath());
    }
}
