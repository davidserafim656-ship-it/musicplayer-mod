package com.musicplayer.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AudioEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer");
    private float volume = 0.8f;
    private String currentTrackName = "";
    private List<Path> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean playing = false;
    private boolean loading = false;
    private SoundInstance currentSound = null;

    public List<String> loadTracks() {
        playlist.clear();
        List<String> names = new ArrayList<>();
        Path folder = PlatformHelper.getMusicFolder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.{ogg,OGG}")) {
            for (Path entry : stream) { playlist.add(entry); names.add(entry.getFileName().toString()); }
        } catch (IOException e) { LOGGER.error("[MusicPlayer] Erro ao carregar", e); }
        playlist.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void play(int index) {
        if (index < 0 || index >= playlist.size()) return;
        stop();
        currentIndex = index;
        Path track = playlist.get(index);
        currentTrackName = track.getFileName().toString();
        loading = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            try {
                String uri = track.toUri().toString();
                Sound sound = new Sound(uri, () -> volume, () -> 1.0f, 1, Sound.RegistrationType.FILE, false, false, 1);
                WeightedSoundSet set = new WeightedSoundSet(Identifier.of("musicplayer", "track"), null);
                set.add(sound);
                SoundInstance si = new PositionedSoundInstance(
                    Identifier.of("musicplayer", "track"),
                    SoundCategory.MUSIC, volume, 1.0f,
                    net.minecraft.util.math.random.Random.create(),
                    false, 0, SoundInstance.AttenuationType.NONE, 0, 0, 0, true
                );
                mc.getSoundManager().play(si);
                currentSound = si;
                playing = true;
                loading = false;
                LOGGER.info("[MusicPlayer] Tocando: {}", currentTrackName);
            } catch (Exception e) {
                LOGGER.error("[MusicPlayer] Erro: {}", e.getMessage());
                loading = false;
            }
        });
    }

    public void pause() { if(currentSound!=null){MinecraftClient.getInstance().getSoundManager().stopAll();playing=false;} }
    public void resume() { if(currentSound!=null){if(currentIndex>=0){play(currentIndex);}} }
    public void stop() { if(currentSound!=null){MinecraftClient.getInstance().getSoundManager().stop(currentSound);currentSound=null;} playing=false; currentTrackName=""; }
    public void next() { if(!playlist.isEmpty()) play((currentIndex+1)%playlist.size()); }
    public void previous() { if(!playlist.isEmpty()) play((currentIndex-1+playlist.size())%playlist.size()); }
    public void setVolume(float v) { volume=Math.max(0f,Math.min(1f,v)); }
    public boolean isPlaying() { return playing; }
    public boolean isLoading() { return loading; }
    public float getProgress() { return 0f; }
    public float getVolume() { return volume; }
    public String getCurrentTrackName() { return currentTrackName; }
    public int getCurrentIndex() { return currentIndex; }
    public long getDurationMs() { return 0; }
    public long getPositionMs() { return 0; }
    public static String formatTime(long ms) { long s=ms/1000; return String.format("%d:%02d",s/60,s%60); }
}
