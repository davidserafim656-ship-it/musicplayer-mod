package com.musicplayer.audio;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detecta se está rodando no Android (PojavLauncher, Zalith, Mojo)
 * ou no Desktop (Windows, Linux, Mac) e fornece os caminhos corretos.
 */
public class PlatformHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer");
    private static Boolean isAndroid = null;
    private static Path musicFolder = null;

    public static boolean isAndroid() {
        if (isAndroid == null) {
            try {
                Class.forName("android.os.Build");
                isAndroid = true;
                LOGGER.info("[MusicPlayer] Plataforma detectada: Android");
            } catch (ClassNotFoundException e) {
                isAndroid = false;
                LOGGER.info("[MusicPlayer] Plataforma detectada: Desktop");
            }
        }
        return isAndroid;
    }

    /**
     * Retorna o nome do launcher Android, se detectável.
     * PojavLauncher, Zalith e Mojo usam caminhos ligeiramente diferentes.
     */
    public static String getAndroidLauncher() {
        if (!isAndroid()) return "desktop";
        try {
            // Verifica variável de ambiente definida pelos launchers
            String pojav = System.getenv("POJAV_PLAUNCHER_DIR");
            if (pojav != null) return "pojav";

            String zalith = System.getenv("ZALITH_LAUNCHER");
            if (zalith != null) return "zalith";

            // Mojo Launcher
            String mojo = System.getenv("MOJO_LAUNCHER");
            if (mojo != null) return "mojo";
        } catch (Exception ignored) {}
        return "android_generic";
    }

    /**
     * Retorna a pasta de músicas correta para cada plataforma/launcher.
     *
     * Desktop:  .minecraft/music_player/
     * Pojav:    /storage/emulated/0/games/PojavLauncher/.minecraft/music_player/
     * Zalith:   /storage/emulated/0/Android/data/net.kdt.pojavlaunch.debug/files/.minecraft/music_player/
     * Mojo:     /storage/emulated/0/MojoLauncher/instances/<instance>/music_player/
     * Genérico: <runDir>/music_player/
     */
    public static Path getMusicFolder() {
        if (musicFolder != null && Files.exists(musicFolder)) return musicFolder;

        Path folder;

        if (!isAndroid()) {
            // Desktop: usa o diretório de execução do Minecraft
            folder = MinecraftClient.getInstance().runDirectory.toPath().resolve("music_player");
        } else {
            String launcher = getAndroidLauncher();
            Path base;

            switch (launcher) {
                case "pojav":
                    // PojavLauncher armazena em /sdcard/games/PojavLauncher/
                    base = Paths.get("/storage/emulated/0/games/PojavLauncher/.minecraft");
                    break;
                case "zalith":
                    // Zalith Launcher (fork do Pojav)
                    base = Paths.get("/storage/emulated/0/Android/data/net.kdt.pojavlaunch/files/.minecraft");
                    // Tenta variante debug também
                    if (!Files.exists(base)) {
                        base = Paths.get("/storage/emulated/0/Android/data/net.kdt.pojavlaunch.debug/files/.minecraft");
                    }
                    break;
                case "mojo":
                    // Mojo Launcher
                    base = Paths.get("/storage/emulated/0/MojoLauncher/.minecraft");
                    break;
                default:
                    // Genérico: usa o runDirectory do Minecraft (funciona na maioria dos launchers)
                    base = MinecraftClient.getInstance().runDirectory.toPath();
                    break;
            }

            folder = base.resolve("music_player");
        }

        // Cria a pasta se não existir
        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
                LOGGER.info("[MusicPlayer] Pasta criada: {}", folder.toAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("[MusicPlayer] Não foi possível criar pasta: {}", folder, e);
            // Fallback: pasta temporária
            folder = Paths.get(System.getProperty("java.io.tmpdir")).resolve("music_player");
            try { Files.createDirectories(folder); } catch (Exception ignored) {}
        }

        musicFolder = folder;
        LOGGER.info("[MusicPlayer] Pasta de músicas: {}", folder.toAbsolutePath());
        return folder;
    }

    /**
     * Retorna uma descrição amigável do caminho para mostrar na interface.
     */
    public static String getMusicFolderDisplay() {
        Path folder = getMusicFolder();
        if (isAndroid()) {
            // No Android mostra o caminho completo pois é útil
            return folder.toString();
        } else {
            // No Desktop simplifica para .minecraft/music_player/
            return ".minecraft/music_player/";
        }
    }
}
