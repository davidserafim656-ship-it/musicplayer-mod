package com.musicplayer.audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

public class AndroidAudioBackend implements IAudioBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer/OpenAL");
    private int alSource = -1;
    private int alBuffer = -1;
    private float volume = 0.8f;
    private boolean playing = false;
    private long durationMs = 0;

    @Override
    public void play(Path file) throws Exception {
        stop();
        String name = file.getFileName().toString().toLowerCase();
        byte[] pcm; int channels, sampleRate;
        if (name.endsWith(".ogg")) {
            byte[] fb = Files.readAllBytes(file);
            ByteBuffer fileBuffer = ByteBuffer.allocateDirect(fb.length);
            fileBuffer.put(fb).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                long handle = STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null);
                if (handle == 0) throw new Exception("Erro OGG: " + error.get(0));
                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(handle, info);
                channels = info.channels(); sampleRate = info.sample_rate();
                int total = STBVorbis.stb_vorbis_stream_length_in_samples(handle);
                ShortBuffer sb = ByteBuffer.allocateDirect(total*channels*2).order(ByteOrder.nativeOrder()).asShortBuffer();
                STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, sb);
                STBVorbis.stb_vorbis_close(handle);
                pcm = new byte[sb.capacity()*2]; sb.rewind();
                ByteBuffer tmp = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
                while(sb.hasRemaining()) tmp.putShort(sb.get());
            }
        } else {
            FileInputStream fis = new FileInputStream(file.toFile());
            javazoom.jl.decoder.Bitstream bs = new javazoom.jl.decoder.Bitstream(fis);
            javazoom.jl.decoder.Decoder dec = new javazoom.jl.decoder.Decoder();
            javazoom.jl.decoder.Header h = bs.readFrame();
            if (h == null) throw new Exception("MP3 invalido");
            sampleRate = h.frequency();
            channels = (h.mode() == javazoom.jl.decoder.Header.SINGLE_CHANNEL) ? 1 : 2;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            do {
                javazoom.jl.decoder.SampleBuffer out = (javazoom.jl.decoder.SampleBuffer) dec.decodeFrame(h, bs);
                short[] s = out.getBuffer();
                for (int i = 0; i < out.getBufferLength(); i++) { baos.write(s[i]&0xFF); baos.write((s[i]>>8)&0xFF); }
                bs.closeFrame();
            } while ((h = bs.readFrame()) != null);
            pcm = baos.toByteArray();
        }
        alBuffer = AL10.alGenBuffers();
        int fmt = channels==1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
        ByteBuffer bb = ByteBuffer.allocateDirect(pcm.length).order(ByteOrder.nativeOrder());
        bb.put(pcm).flip();
        AL10.alBufferData(alBuffer, fmt, bb.asShortBuffer(), sampleRate);
        alSource = AL10.alGenSources();
        AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer);
        AL10.alSourcef(alSource, AL10.AL_GAIN, volume);
        AL10.alSource3f(alSource, AL10.AL_POSITION, 0f, 0f, 0f);
        AL10.alSourcei(alSource, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSourcePlay(alSource);
        durationMs = (pcm.length * 1000L) / (sampleRate * channels * 2L);
        playing = true;
    }

    @Override public void pause() { if(alSource>=0&&playing){AL10.alSourcePause(alSource);playing=false;} }
    @Override public void resume() { if(alSource>=0&&!playing){AL10.alSourcePlay(alSource);playing=true;} }
    @Override public void stop() {
        if(alSource>=0){AL10.alSourceStop(alSource);AL10.alDeleteSources(alSource);alSource=-1;}
        if(alBuffer>=0){AL10.alDeleteBuffers(alBuffer);alBuffer=-1;}
        playing=false; durationMs=0;
    }
    @Override public void setVolume(float v) { volume=Math.max(0f,Math.min(1f,v)); if(alSource>=0)AL10.alSourcef(alSource,AL10.AL_GAIN,volume); }
    @Override public boolean isPlaying() { return playing&&alSource>=0&&AL10.alGetSourcei(alSource,AL10.AL_SOURCE_STATE)==AL10.AL_PLAYING; }
    @Override public float getProgress() { return durationMs<=0?0f:Math.min(1f,(float)getPositionMs()/durationMs); }
    @Override public long getDurationMs() { return durationMs; }
    @Override public long getPositionMs() { return alSource<0?0:(long)(AL10.alGetSourcef(alSource,AL11.AL_SEC_OFFSET)*1000); }
}
