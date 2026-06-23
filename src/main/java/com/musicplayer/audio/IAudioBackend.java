package com.musicplayer.audio;

import java.nio.file.Path;

/**
 * Interface comum para os backends de áudio.
 * Desktop usa javax.sound, Android usa OpenAL via LWJGL.
 */
public interface IAudioBackend {
    void play(Path file) throws Exception;
    void pause();
    void resume();
    void stop();
    void setVolume(float volume); // 0.0 a 1.0
    boolean isPlaying();
    float getProgress(); // 0.0 a 1.0
    long getDurationMs();
    long getPositionMs();
}
