package com.aichen.datapackconfigmod.event;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.aichen.datapackconfigmod.client.ConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Optional;

/**
 * 客户端事件处理器
 * 处理客户端相关的初始化事件
 */
@Mod.EventBusSubscriber(modid = DataPackConfigMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    
    /**
     * 客户端初始化事件
     * @param event 事件
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册配置屏幕
        event.enqueueWork(() -> {
            Optional<? extends ModContainer> container = net.minecraftforge.fml.ModList.get().getModContainerById(DataPackConfigMod.MODID);
            if (container.isPresent()) {
                container.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> ConfigScreen.createConfigScreen(screen))
                );
            }
        });
    }
}