package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主菜单按钮处理器
 * 在主菜单添加数据包配置按钮
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = DataPackConfigMod.MODID)
public class MainMenuButtonHandler {
    
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        // 只在主菜单屏幕处理
        if (!(screen instanceof TitleScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = screen.width;
        int screenHeight = screen.height;

        // 计算按钮位置（放在右上角）
        int buttonX = screenWidth - BUTTON_WIDTH - 5; // 距离右边缘5像素
        int buttonY = 5; // 距离顶部5像素
        
        // 创建数据包配置按钮（使用标准小方块按钮）
        Button datapackConfigButton = Button.builder(
            Component.translatable("menu.datapackconfigmod.config"),
            (button) -> {
                // 打开配置界面
                minecraft.setScreen(ConfigScreen.createConfigScreen(screen));
            }
        )
        .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();
        
        // 添加按钮到屏幕
        event.addListener(datapackConfigButton);
        
        DataPackConfigMod.LOGGER.info("[DatapackConfigMod] 主菜单数据包配置按钮已添加，位置: ({}, {})", buttonX, buttonY);
    }
}
