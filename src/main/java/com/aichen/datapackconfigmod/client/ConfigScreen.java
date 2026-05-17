package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.DatapackConfig;
import com.aichen.datapackconfigmod.manager.DatapackSettingsManager;
import com.mojang.logging.LogUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

/**
 * 配置屏幕工厂
 * 用于创建Cloth Config配置界面
 */
public class ConfigScreen {

    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 创建配置屏幕
     * @param parent 父屏幕
     * @return 配置屏幕
     */
    @OnlyIn(Dist.CLIENT)
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(net.minecraft.network.chat.Component.translatable("config.datapackconfigmod.title"));
        
        // 启用左侧分类菜单模式（参考官方实现）
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);  // 分类默认关闭（根据用户要求）
        
        // 设置配置界面
        DatapackConfig.setupConfigScreen(builder);
        
        // 设置保存操作 - 在所有修改完成后一次性应用
        builder.setSavingRunnable(() -> {
            LOGGER.debug("[DatapackConfigMod] ===== 配置界面点击完成 =====");
            LOGGER.debug("[DatapackConfigMod] 准备应用配置到数据包文件");

            // 获取所有数据包设置并应用
            try {
                var allSettings = DatapackSettingsManager.getAllSettings();
                LOGGER.info("[DatapackConfigMod] 找到 {} 个数据包设置", allSettings.size());

                if (allSettings.isEmpty()) {
                    LOGGER.warn("[DatapackConfigMod] 警告: 没有找到任何数据包设置！");
                }

                for (String datapackId : allSettings.keySet()) {
                    LOGGER.info("[DatapackConfigMod] 应用配置到数据包: {}", datapackId);

                    // 打印该数据包的所有自定义值
                    var settings = allSettings.get(datapackId);
                    var customValues = settings.getCustomValues();
                    LOGGER.debug("[DatapackConfigMod] 数据包 {} 有 {} 个自定义值", datapackId, customValues.size());
                    for (var entry : customValues.entrySet()) {
                        LOGGER.debug("[DatapackConfigMod]   - {} = {}", entry.getKey(), entry.getValue());
                    }

                    DatapackConfigGuiBuilder.applyConfigToDatapack(datapackId);
                }
                LOGGER.info("[DatapackConfigMod] ===== 所有数据包配置应用完成 =====");
            } catch (Exception e) {
                DataPackConfigMod.LOGGER.error("[DatapackConfigMod] 应用配置到数据包失败", e);
            }
        });
        
        return builder.build();
    }
}