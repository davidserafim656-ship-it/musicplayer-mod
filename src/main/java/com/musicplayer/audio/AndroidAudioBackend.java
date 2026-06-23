package com.musicplayer.audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;

/**
 * Backend de áudio para Android (PojavLauncher, Zalith, Mojo).
 * Usa OpenAL via LWJGL3, que já está incluído no Minecraft.
 *
 * Suporta OGG (via decodificação manual) e MP3 (via JLayer).
 * O contexto OpenAL já é iniciado pelo próprio Minecraft, então
 * apenas criamos sources e buffers adicionais.
 */
public class AndroidAudioBackend implements IAudioBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer/Android");

    private int alSource = -1;
    private int alBuffer = -1;
    private float volume = 0.8f;
    private boolean playing = false;
    private long durationMs = 0;
    private long startTimeMs = 0;
    private long pausePositionMs = 0;

    @Override
    public void play(Path file) throws Exception {
        stop();
        String name = file.getFileName().toString().toLowerCase();

        // Decodifica o arquivo para PCM
        PcmData pcm;
        if (name.endsWith(".ogg")) {
            pcm = decodeOgg(file);
        } else if (name.endsWith(".mp3")) {
            pcm = decodeMp3(file);
        } else {
            throw new Exception("Formato não suportado: " + name);
        }

        // Cria buffer OpenAL
        alBuffer = AL10.alGenBuffers();
        int format = pcm.channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
        AL10.alBufferData(alBuffer, format, pcm.data, pcm.sampleRate);

        // Cria source OpenAL
        alSource = AL10.alGenSources();
        AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer);
        AL10.alSourcef(alSource, AL10.AL_GAIN, volume);
        AL10.alSourcePlay(alSource);

        durationMs = pcm.durationMs;
        startTimeMs = System.currentTimeMillis();
        pausePositionMs = 0;
        playing = true;

        LOGGER.info("[MusicPlayer/Android] Tocando via OpenAL: {}", file.getFileName());
    }

    private PcmData decodeOgg(Path path) throws Exception {
        // Usa javax.sound se disponível (alguns builds Android o têm)
        // Caso contrário, usa STB Vorbis via LWJGL
        try {
            return decodeOggViaSTB(path);
        } catch (Exception e) {
            LOGGER.warn("[MusicPlayer/Android] STB Vorbis falhou, tentando fallback: {}", e.getMessage());
            return decodeOggViaJavaxSound(path);
        }
    }

    private PcmData decodeOggViaSTB(Path path) throws Exception {
        // LWJGL STBVorbis - disponível no Minecraft bundled LWJGL
        org.lwjgl.stb.STBVorbisInfo info = org.lwjgl.stb.STBVorbisInfo.create();
        java.nio.IntBuffer error = java.nio.ByteBuffer.allocateDirect(4).asIntBuffer();
        byte[] fileBytes = java.nio.file.Files.readAllBytes(path);
        ByteBuffer fileBuffer = ByteBuffer.allocateDirect(fileBytes.length);
        fileBuffer.put(fileBytes).flip();

        long handle = org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null);
        if (handle == 0) throw new Exception("STBVorbis não conseguiu abrir o arquivo OGG");

        org.lwjgl.stb.STBVorbis.stb_vorbis_get_info(handle, info);
        int channels = info.channels();
        int sampleRate = info.sample_rate();
        int totalSamples = org.lwjgl.stb.STBVorbis.stb_vorbis_stream_length_in_samples(handle);

        ShortBuffer pcmBuffer = ByteBuffer
                .allocateDirect(totalSamples * channels * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();

        org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, pcmBuffer);
        org.lwjgl.stb.STBVorbis.stb_vorbis_close(handle);

        ByteBuffer byteData = ByteBuffer.allocateDirect(pcmBuffer.capacity() * 2);
        pcmBuffer.rewind();
        while (pcmBuffer.hasRemaining()) {
            short s = pcmBuffer.get();
            byteData.put((byte)(s & 0xFF));
            byteData.put((byte)((s >> 8) & 0xFF));
        }
        byteData.flip();

        long dur = (totalSamples * 1000L) / sampleRate;
        return new PcmData(byteData, channels, sampleRate, dur);
    }

    private PcmData decodeOggViaJavaxSound(Path path) throws Exception {
        javax.sound.sampled.AudioInputStream raw =
                javax.sound.sampled.AudioSystem.getAudioInputStream(path.toFile());
        javax.sound.sampled.AudioFormat base = raw.getFormat();
        javax.sound.sampled.AudioFormat pcmFmt = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                base.getSampleRate(), 16, base.getChannels(),
                base.getChannels() * 2, base.getSampleRate(), false
        );
        javax.sound.sampled.AudioInputStream decoded =
                javax.sound.sampled.AudioSystem.getAudioInputStream(pcmFmt, raw);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = decoded.read(buf)) != -1) baos.write(buf, 0, n);

        byte[] bytes = baos.toByteArray();
        ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length);
        bb.put(bytes).flip();

        int channels = base.getChannels();
        int sampleRate = (int) base.getSampleRate();
        long dur = (bytes.length * 1000L) / (sampleRate * channels * 2);
        return new PcmData(bb, channels, sampleRate, dur);
    }

    private PcmData decodeMp3(Path path) throws Exception {
        FileInputStream fis = new FileInputStream(path.toFile());
        javazoom.jl.decoder.Bitstream bitstream = new javazoom.jl.decoder.Bitstream(fis);
        javazoom.jl.decoder.Decoder decoder = new javazoom.jl.decoder.Decoder();

        javazoom.jl.decoder.Header header = bitstream.readFrame();
        if (header == null) throw new Exception("MP3 inválido");

        int sampleRate = header.getSampleRate();
        int channels = (header.mode() == javazoom.jl.decoder.Header.SINGLE_CHANNEL) ? 1 : 2;
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

        byte[] bytes = baos.toByteArray();
        ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length);
        bb.put(bytes).flip();

        long dur = (bytes.length * 1000L) / (sampleRate * channels * 2);
        return new PcmData(bb, channels, sampleRate, dur);
    }

    @Override
    public void pause() {
        if (alSource >= 0 && playing) {
            AL10.alSourcePause(alSource);
            pausePositionMs = getPositionMs();
            playing = false;
        }
    }

    @Override
    public void resume() {
        if (alSource >= 0 && !playing) {
            AL10.alSourcePlay(alSource);
            startTimeMs = System.currentTimeMillis() - pausePositionMs;
            playing = true;
        }
    }

    @Override
    public void stop() {
        if (alSource >= 0) {
            AL10.alSourceStop(alSource);
            AL10.alDeleteSources(alSource);
            alSource = -1;
        }
        if (alBuffer >= 0) {
            AL10.alDeleteBuffers(alBuffer);
            alBuffer = -1;
        }
        playing = false;
        durationMs = 0;
        pausePositionMs = 0;
    }

    @Override
    public void setVolume(float vol) {
        this.volume = Math.max(0f, Math.min(1f, vol));
        if (alSource >= 0) {
            AL10.alSourcef(alSource, AL10.AL_GAIN, volume);
        }
    }

    @Override
    public boolean isPlaying() {
        if (!playing || alSource < 0) return false;
        return AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    @Override
    public float getProgress() {
        if (durationMs <= 0) return 0f;
        return Math.min(1f, (float) getPositionMs() / durationMs);
    }

    @Override
    public long getDurationMs() { return durationMs; }

    @Override
    public long getPositionMs() {
        if (alSource < 0) return 0;
        // OpenAL fornece posição em segundos como float
        float secs = AL10.alGetSourcef(alSource, AL11.AL_SEC_OFFSET);
        return (long)(secs * 1000);
    }

    // Container para dados PCM decodificados
    private static class PcmData {
        final ByteBuffer data;
        final int channels;
        final int sampleRate;
        final long durationMs;

        PcmData(ByteBuffer data, int channels, int sampleRate, long durationMs) {
            this.data = data;
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.durationMs = durationMs;
        }
    }
}
