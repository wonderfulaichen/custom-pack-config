package com.aichen.custompackconfigmod;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class GlobalPackManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String FOLDER_NAME = "global_datapacks";

    public static final Path PACK_PATH = FMLPaths.GAMEDIR.get().resolve(FOLDER_NAME);

    public static void initFolder() {
        File folder = PACK_PATH.toFile();
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                LOGGER.info("Created global datapacks folder at: {}", PACK_PATH);
            }
        }
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            initFolder();

            File folder = PACK_PATH.toFile();
            File[] files = folder.listFiles();

            if (files == null) {
                LOGGER.warn("Could not list files in: {}", PACK_PATH);
                return;
            }

            int loaded = 0;
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith(".")) {
                    continue;
                }

                if (file.isDirectory() || name.endsWith(".zip")) {
                    String id = "global_" + name.replaceAll("[^a-z0-9_.-]", "_");
                    if (id.equals("global_")) {
                        LOGGER.warn("Skipping '{}': sanitized ID is empty", name);
                        continue;
                    }

                    Pack pack = Pack.readMetaAndCreate(
                            id,
                            Component.literal("Global: " + name),
                            true,
                            (path) -> new PathPackResources(id, file.toPath(), false),
                            PackType.SERVER_DATA,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                    );

                    if (pack != null) {
                        event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
                        loaded++;
                        LOGGER.info("Registered global datapack: {} (id: {})", name, id);
                    } else {
                        LOGGER.warn("Failed to load global datapack: {} (invalid pack format)", name);
                    }
                }
            }
            LOGGER.info("Global datapack scan complete: {} pack(s) loaded", loaded);
        }
    }
}