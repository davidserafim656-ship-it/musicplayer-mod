package com.musicplayer.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Path;

/**
 * Backend de áudio para Desktop (Windows, Linux, macOS).
 * Usa javax.sound.sampled + JLayer para MP3.
 */
public class DesktopAudioBackend implements IAudioBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer/Desktop");

    private Clip currentClip;
    private float volume = 0.8f;
    private boolean playing = false;
    private long durationMs = 0;

    @Override
    public void play(Path file) throws Exception {
        stop();
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".ogg")) {
            playOgg(file);
        } else if (name.endsWith(".mp3")) {
            playMp3(file);
        } else {
            throw new Exception("Formato não suportado: " + name);
        }
    }

    private void playOgg(Path path) throws Exception {
        AudioInputStream raw = AudioSystem.getAudioInputStream(path.toFile());
        AudioFormat base = raw.getFormat();
        AudioFormat pcm = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                base.getSampleRate(), 16,
                base.getChannels(),
                base.getChannels() * 2,
                base.getSampleRate(), false
        );
        AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, raw);
        currentClip = AudioSystem.getClip();
        currentClip.open(decoded);
        durationMs = (long) (currentClip.getMicrosecondLength() / 1000L);
        applyVolume();
        currentClip.start();
        playing = true;
    }

    private void playMp3(Path path) throws Exception {
        FileInputStream fis = new FileInputStream(path.toFile());
        javazoom.jl.decoder.Bitstream bitstream = new javazoom.jl.decoder.Bitstream(fis);
        javazoom.jl.decoder.Decoder decoder = new javazoom.jl.decoder.Decoder();

        javazoom.jl.decoder.Header header = bitstream.readFrame();
        if (header == null) throw new Exception("MP3 inválido ou vazio");

        int sampleRate = header.getSampleRate();
        int channels = (header.mode() == javazoom.jl.decoder.Header.SINGLE_CHANNEL) ? 1 : 2;
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        do {
            javazoom.jl.decoder.SampleBuffer output =
                    (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(header, bitstream);
            short[] samples = output.getBuffer();
            for (short s : samples) {
                baos.write(s & 0xFF);
                baos.write((s >> 8) & 0xFF);
            }
            bitstream.closeFrame();
        } while ((header = bitstream.readFrame()) != null);

        byte[] pcm = baos.toByteArray();
        AudioInputStream audio = new AudioInputStream(
                new ByteArrayInputStream(pcm), format,
                pcm.length / format.getFrameSize()
        );
        currentClip = AudioSystem.getClip();
        currentClip.open(audio);
        durationMs = (long) (currentClip.getMicrosecondLength() / 1000L);
        applyVolume();
        currentClip.start();
        playing = true;
    }

    @Override
    public void pause() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            playing = false;
        }
    }

    @Override
    public void resume() {
        if (currentClip != null && !currentClip.isRunning()) {
            currentClip.start();
            playing = true;
        }
    }

    @Override
    public void stop() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
            currentClip = null;
        }
        playing = false;
        durationMs = 0;
    }

    @Override
    public void setVolume(float vol) {
        this.volume = Math.max(0f, Math.min(1f, vol));
        applyVolume();
    }

    private void applyVolume() {
        if (currentClip == null) return;
        try {
            FloatControl gain = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log10(Math.max(volume, 0.0001f)) * 20.0);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isPlaying() {
        return playing && currentClip != null && currentClip.isRunning();
    }

    @Override
    public float getProgress() {
        if (currentClip == null || currentClip.getFrameLength() <= 0) return 0f;
        return (float) currentClip.getFramePosition() / currentClip.getFrameLength();
    }

    @Override
    public long getDurationMs() { return durationMs; }

    @Override
    public long getPositionMs() {
        if (currentClip == null) return 0;
        return currentClip.getMicrosecondPosition() / 1000L;
    }
}
