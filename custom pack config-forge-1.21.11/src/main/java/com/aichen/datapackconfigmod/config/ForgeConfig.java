package com.aichen.datapackconfigmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge 配置规范
 * 用于创建和管理 Forge 配置文件
 */
public class ForgeConfig {
    
    public static class Common {
        // 配置项
        public final ForgeConfigSpec.ConfigValue<String> datapackFolderPath;
        public final ForgeConfigSpec.BooleanValue logDatapackLoading;
        
        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("数据包配置设置")
                   .push("datapack");
            
            // 数据包文件夹路径
            datapackFolderPath = builder
                    .comment("全局数据包文件夹路径")
                    .define("datapackFolderPath", "config/datapack-config-mod");
            
            // 日志记录
            logDatapackLoading = builder
                    .comment("是否记录数据包加载日志")
                    .define("logDatapackLoading", true);
            
            builder.pop();
        }
    }
    
    // 配置实例
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    
    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
    
    /**
     * 从 Forge 配置同步到 DatapackConfig
     */
    public static void syncConfig() {
        if (COMMON.datapackFolderPath != null) {
            DatapackConfig.datapackFolderPath = COMMON.datapackFolderPath.get();
        }
        if (COMMON.logDatapackLoading != null) {
            DatapackConfig.logDatapackLoading = COMMON.logDatapackLoading.get();
        }
    }
}
