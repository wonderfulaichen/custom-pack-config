package com.aichen.datapackconfigmod.config;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.client.DatapackConfigGuiBuilder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloth Config配置界面
 * 使用Cloth Config创建用户友好的配置界面
 */
public class DatapackConfig {
    
    // 配置值
    public static String datapackFolderPath = "config/datapack-config-mod";
    public static boolean logDatapackLoading = true;
    
    /**
     * 创建配置界面
     * @param builder Cloth Config的ConfigBuilder
     * @return 配置界面
     */
    @OnlyIn(Dist.CLIENT)
    public static void setupConfigScreen(ConfigBuilder builder) {
        DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 开始构建配置界面");

        // 先加载数据包设置（修复：提前扫描数据包，无需进入创建世界）
        try {
            loadDatapackSettings();
        } catch (Exception e) {
            DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 加载数据包设置失败，继续使用基本配置", e);
        }

        builder.setTitle(Component.translatable("config.datapackconfigmod.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 基本设置分类
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.datapackconfigmod.category.general"));



        // 数据包文件夹路径
        general.addEntry(entryBuilder.startStrField(
                Component.translatable("config.datapackconfigmod.datapack_folder_path"),
                datapackFolderPath)
                .setDefaultValue("config/datapack-config-mod")
                .setTooltip(Component.translatable("config.datapackconfigmod.datapack_folder_path.tooltip"))
                .setSaveConsumer(newValue -> {
                    datapackFolderPath = newValue;
                    // 路径改变后重新加载设置
                    loadDatapackSettings();
                })
                .build());

        // 日志设置（移动到基本设置分类下）
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.datapackconfigmod.log_datapack_loading"),
                logDatapackLoading)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.datapackconfigmod.log_datapack_loading.tooltip"))
                .setSaveConsumer(newValue -> logDatapackLoading = newValue)
                .build());

        // 构建数据包配置界面（新设计：每个数据包独立分类）
        try {
            DatapackConfigGuiBuilder.buildDatapackConfigCategories(builder, entryBuilder);
            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 配置界面构建完成");
        } catch (Exception e) {
            DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 构建数据包配置界面失败", e);
            // 添加错误提示
            ConfigCategory errorCategory = builder.getOrCreateCategory(Component.literal("错误"));
            errorCategory.addEntry(entryBuilder.startTextDescription(
                Component.literal("§c加载数据包配置时出错，请查看游戏日志")
            ).build());
        }
    }
    
    /**
     * 加载数据包设置（新增方法）
     */
    private static void loadDatapackSettings() {
        // 确保数据包文件夹存在
        com.aichen.datapackconfigmod.manager.DatapackManager.ensureDatapackFolderExists();
        
        // 加载已保存的设置
        com.aichen.datapackconfigmod.manager.DatapackSettingsManager.loadSettings();
        
        // 扫描数据包文件夹，解析配置
        scanDatapacksForConfig();
    }
    
    /**
     * 扫描数据包并解析配置（新增方法）
     */
    private static void scanDatapacksForConfig() {
        try {
            Path datapackFolder = getDatapackFolderPath();
            File folder = datapackFolder.toFile();

            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 扫描数据包文件夹: {}", folder.getAbsolutePath());

            if (!folder.exists()) {
                DataPackConfigMod.LOGGER.warn("[DatapackConfigMod] 数据包文件夹不存在: {}", folder.getAbsolutePath());
                return;
            }

            if (!folder.isDirectory()) {
                DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 数据包路径不是目录: {}", folder.getAbsolutePath());
                return;
            }

            File[] files = folder.listFiles();
            if (files == null) {
                DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 无法列出文件");
                return;
            }

            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 找到 {} 个文件", files.length);

            // 修改：只处理.zip文件，不再进行去重
            int validDatapacks = 0;
            for (File file : files) {
                DataPackConfigMod.LOGGER.debug("[DatapackConfigMod] 检查文件: {}", file.getName());
                if (isValidDatapack(file)) {
                    String packName = file.getName();
                    String packId = DataPackConfigMod.MODID + ":" + packName;

                    DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 找到有效数据包: {}, ID: {}", file.getName(), packId);
                    validDatapacks++;

                    // 解析并存储配置
                    parseAndStoreDatapackConfig(file, packId);
                }
            }

            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 扫描完成，找到 {} 个.zip数据包", validDatapacks);
        } catch (Exception e) {
            DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 扫描数据包配置失败", e);
        }
    }
    
    /**
     * 检查是否是有效的数据包（新增方法）
     * 修改：只支持.zip文件，忽略文件夹
     */
    private static boolean isValidDatapack(File file) {
        // 修改：只检查是否是.zip文件，忽略文件夹
        return file.isFile() && file.getName().endsWith(".zip");
    }
    
    /**
     * 解析并存储数据包配置（修复：合并元数据，不跳过已存在的配置）
     */
    private static void parseAndStoreDatapackConfig(File datapackFile, String packId) {
        try {
            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 开始解析数据包配置: {} (文件: {})", packId, datapackFile.getAbsolutePath());

            var existingSettings = com.aichen.datapackconfigmod.manager.DatapackSettingsManager.getSetting(packId);

            // 检查配置文件是否存在
            File configFile = new File(datapackFile, "datapack_config.json");
            DataPackConfigMod.LOGGER.debug("[DatapackConfigMod] 检查配置文件: {}", configFile.getAbsolutePath());
            DataPackConfigMod.LOGGER.debug("[DatapackConfigMod] 配置文件存在: {}", configFile.exists());

            var newSettings = com.aichen.datapackconfigmod.parser.DatapackConfigParser.parseConfig(datapackFile, packId);

            if (newSettings != null) {
                DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 成功解析数据包配置: {} ({} 个自定义值, {} 个元数据)",
                    newSettings.getDisplayName(),
                    newSettings.getCustomValues().size(),
                    newSettings.getAllConfigMetadata().size());

                if (existingSettings != null) {
                    // 如果已存在设置，保留用户的启用状态，合并新元数据和自定义值
                    DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 合并现有设置: {}", packId);
                    // 保留用户的启用状态（不需要重复赋值）

                    // 合并自定义值（保留用户修改的值）
                    for (var entry : newSettings.getCustomValues().entrySet()) {
                        String key = entry.getKey();
                        String newValue = entry.getValue();
                        String oldValue = existingSettings.getCustomValue(key);

                        if (oldValue == null) {
                            // 新增的配置项
                            existingSettings.addCustomValue(key, newValue);
                        }
                        // 如果用户已修改过，保留用户值；否则使用新值
                    }

                    // 合并元数据（总是更新，确保类型信息是最新的）
                    for (var entry : newSettings.getAllConfigMetadata().entrySet()) {
                        existingSettings.addConfigMetadata(entry.getKey(), entry.getValue());
                    }

                    // 保存合并后的设置
                    com.aichen.datapackconfigmod.manager.DatapackSettingsManager.addOrUpdateSetting(existingSettings);
                    DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 配置已合并，保留用户修改");
                } else {
                    // 新数据包，直接使用解析的配置
                    com.aichen.datapackconfigmod.manager.DatapackSettingsManager.addOrUpdateSetting(newSettings);
                }
            } else {
                // 如果没有配置文件，创建默认设置
                DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 数据包没有配置文件，创建默认设置: {}", packId);
                var defaultSettings = new com.aichen.datapackconfigmod.config.model.DatapackSettings(
                    packId,
                    datapackFile.getName()
                );
                defaultSettings.setEnabled(true);
                com.aichen.datapackconfigmod.manager.DatapackSettingsManager.addOrUpdateSetting(defaultSettings);
            }
        } catch (Exception e) {
            DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 解析数据包配置失败: {}", packId, e);
        }
    }
    
    /**
     * 获取数据包文件夹的完整路径
     * @return 数据包文件夹路径
     */
    public static Path getDatapackFolderPath() {
        // 使用相对路径，确保跨平台兼容性
        return Paths.get(".").resolve(datapackFolderPath);
    }
    

    
    /**
     * 是否记录数据包加载日志
     * @return 是否记录日志
     */
    public static boolean shouldLogDatapackLoading() {
        return logDatapackLoading;
    }
}