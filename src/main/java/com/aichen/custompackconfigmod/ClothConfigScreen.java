package com.aichen.custompackconfigmod;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.custom_pack_config.config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Log Dirt Block"), Config.logDirtBlock)
                .setDefaultValue(true)
                .setSaveConsumer(Config::setLogDirtBlock)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.literal("Magic Number"), Config.magicNumber)
                .setDefaultValue(42)
                .setSaveConsumer(Config::setMagicNumber)
                .build());

        builder.setSavingRunnable(Config::save);

        return builder.build();
    }
}