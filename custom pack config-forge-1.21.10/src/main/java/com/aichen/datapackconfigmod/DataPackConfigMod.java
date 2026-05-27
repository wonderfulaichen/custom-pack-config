package com.aichen.datapackconfigmod;

import com.mojang.logging.LogUtils;
import com.aichen.datapackconfigmod.config.DatapackConfig;
import com.aichen.datapackconfigmod.config.ForgeConfig;
import com.aichen.datapackconfigmod.event.ClientEvents;
import com.aichen.datapackconfigmod.event.ServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * 数据包配置模组主类
 * 为数据包提供全局配置和管理功能
 */
@Mod(DataPackConfigMod.MODID)
public class DataPackConfigMod
{
    // 模组ID常量
    public static final String MODID = "datapack_config_mod";
    
    // 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构造函数
     * @param context FML加载上下文
     */
    @SuppressWarnings("deprecation")
    public DataPackConfigMod(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context)
    {
        // MC 1.21.10: IEventBus 已被移除，使用 getModBusGroup()
        var modBus = context.getModBusGroup();

        // 注册服务器事件处理器
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        
        // MC 1.21.10: ModConfigEvent.getBus() 已移除
        // 配置加载由 ForgeConfig 的 @SubscribeEvent 自动处理

        // 注册配置 - FMLJavaModLoadingContext 继承 ModLoadingContext
        context.registerConfig(ModConfig.Type.COMMON, ForgeConfig.COMMON_SPEC, "datapack-config-mod.toml");
        
        // 确保数据包文件夹存在
        com.aichen.datapackconfigmod.manager.DatapackManager.ensureDatapackFolderExists();
        
        if (DatapackConfig.shouldLogDatapackLoading()) {
            LOGGER.info("[DatapackConfigMod] 数据包配置模组已加载");
        }
    }
}
