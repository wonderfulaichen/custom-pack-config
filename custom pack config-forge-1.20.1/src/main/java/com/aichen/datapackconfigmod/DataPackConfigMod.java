package com.aichen.datapackconfigmod;

import com.mojang.logging.LogUtils;
import com.aichen.datapackconfigmod.config.DatapackConfig;
import com.aichen.datapackconfigmod.config.ForgeConfig;
import com.aichen.datapackconfigmod.event.ClientEvents;
import com.aichen.datapackconfigmod.event.ServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    public DataPackConfigMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 注册服务器事件处理器
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        
        // 注册配置事件
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.COMMON_SPEC, "datapack-config-mod.toml");
        
        // 确保数据包文件夹存在
        com.aichen.datapackconfigmod.manager.DatapackManager.ensureDatapackFolderExists();
        
        if (DatapackConfig.shouldLogDatapackLoading()) {
            LOGGER.info("[DatapackConfigMod] 数据包配置模组已加载");
        }
    }
    
    /**
     * 配置加载事件
     * @param event 事件
     */
    private void onConfigLoad(ModConfigEvent.Loading event) {
        ForgeConfig.syncConfig();
    }
    
    /**
     * 配置重载事件
     * @param event 事件
     */
    private void onConfigReload(ModConfigEvent.Reloading event) {
        ForgeConfig.syncConfig();
    }
}
