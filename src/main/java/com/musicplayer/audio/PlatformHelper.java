package com.musicplayer.audio;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class PlatformHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer");
    private static Boolean isAndroid = null;
    private static Path musicFolder = null;

    public static boolean isAndroid() {
        if (isAndroid == null) {
            try {
                Class.forName("android.os.Build");
                isAndroid = true;
                LOGGER.info("[MusicPlayer] Plataforma: Android");
            } catch (ClassNotFoundException e) {
                isAndroid = false;
                LOGGER.info("[MusicPlayer] Plataforma: Desktop");
            }
        }
        return isAndroid;
    }

    public static Path getMusicFolder() {
        if (musicFolder != null && Files.exists(musicFolder)) return musicFolder;
        Path folder = MinecraftClient.getInstance().runDirectory.toPath().resolve("music_player");
        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
        } catch (Exception e) {
            LOGGER.error("[MusicPlayer] Erro ao criar pasta: {}", folder, e);
        }
        musicFolder = folder;
        LOGGER.info("[MusicPlayer] Pasta de músicas: {}", folder.toAbsolutePath());
        return folder;
    }

    public static String getMusicFolderDisplay() {
        return getMusicFolder().toString();
    }
}
