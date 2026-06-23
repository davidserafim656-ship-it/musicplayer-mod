package com.musicplayer.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Engine principal de áudio.
 * Detecta automaticamente a plataforma e usa o backend correto:
 * - Desktop (PC): DesktopAudioBackend (javax.sound)
 * - Android (Pojav/Zalith/Mojo): AndroidAudioBackend (OpenAL/LWJGL)
 */
public class AudioEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer");

    private final IAudioBackend backend;
    private float volume = 0.8f;
    private String currentTrackName = "";
    private List<Path> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isLoading = false;

    public AudioEngine() {
        if (PlatformHelper.isAndroid()) {
            LOGGER.info("[MusicPlayer] Usando AndroidAudioBackend (OpenAL)");
            backend = new AndroidAudioBackend();
        } else {
            LOGGER.info("[MusicPlayer] Usando DesktopAudioBackend (javax.sound)");
            backend = new DesktopAudioBackend();
        }
    }

    public List<String> loadTracks() {
        playlist.clear();
        List<String> names = new ArrayList<>();
        Path folder = PlatformHelper.getMusicFolder();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.{ogg,mp3,OGG,MP3}")) {
            for (Path entry : stream) {
                playlist.add(entry);
                names.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            LOGGER.error("[MusicPlayer] Erro ao carregar músicas de {}", folder, e);
        }

        // Ordena por nome de arquivo
        playlist.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void play(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        Path track = playlist.get(index);
        currentTrackName = track.getFileName().toString();
        isLoading = true;

        new Thread(() -> {
            try {
                backend.play(track);
                LOGGER.info("[MusicPlayer] Tocando: {}", currentTrackName);
            } catch (Exception e) {
                LOGGER.error("[MusicPlayer] Erro ao tocar {}: {}", currentTrackName, e.getMessage());
                currentTrackName = "Erro: " + currentTrackName;
            } finally {
                isLoading = false;
            }
        }, "MusicPlayer-Thread").start();
    }

    public void pause()  { backend.pause(); }
    public void resume() { backend.resume(); }

    public void stop() {
        backend.stop();
        currentTrackName = "";
        currentIndex = -1;
    }

    public void next() {
        if (playlist.isEmpty()) return;
        play((currentIndex + 1) % playlist.size());
    }

    public void previous() {
        if (playlist.isEmpty()) return;
        play((currentIndex - 1 + playlist.size()) % playlist.size());
    }

    public void setVolume(float vol) {
        this.volume = Math.max(0f, Math.min(1f, vol));
        backend.setVolume(this.volume);
    }

    public boolean isPlaying()          { return backend.isPlaying(); }
    public boolean isLoading()          { return isLoading; }
    public float   getProgress()        { return backend.getProgress(); }
    public float   getVolume()          { return volume; }
    public String  getCurrentTrackName(){ return currentTrackName; }
    public int     getCurrentIndex()    { return currentIndex; }
    public long    getDurationMs()      { return backend.getDurationMs(); }
    public long    getPositionMs()      { return backend.getPositionMs(); }

    /** Formata milissegundos como mm:ss */
    public static String formatTime(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
