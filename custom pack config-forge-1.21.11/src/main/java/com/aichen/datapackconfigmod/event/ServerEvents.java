package com.aichen.datapackconfigmod.event;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.config.DatapackConfig;
import com.aichen.datapackconfigmod.manager.DatapackManager;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务器事件处理器
 * 处理服务器启动和停止事件
 */
@Mod.EventBusSubscriber(modid = DataPackConfigMod.MODID)
public class ServerEvents {
    
    /**
     * 服务器即将启动事件
     * 确保数据包文件夹存在
     * @param event 事件
     */
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // 确保数据包文件夹存在
        DatapackManager.ensureDatapackFolderExists();
        
        if (DatapackConfig.shouldLogDatapackLoading()) {
            DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 服务器即将启动，数据包文件夹准备就绪");
        }
    }
}