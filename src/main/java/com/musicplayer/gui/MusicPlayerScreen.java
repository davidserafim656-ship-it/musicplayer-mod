package com.musicplayer.gui;

import com.musicplayer.MusicPlayerClient;
import com.musicplayer.audio.AudioEngine;
import com.musicplayer.audio.PlatformHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.List;

public class MusicPlayerScreen extends Screen {

    private List<String> tracks;
    private int scrollOffset = 0;
    private static final int VISIBLE_TRACKS = 8;
    private static final int TRACK_HEIGHT = 20;

    public MusicPlayerScreen() {
        super(Text.literal("Music Player"));
    }

    @Override
    protected void init() {
        AudioEngine engine = MusicPlayerClient.audioEngine;
        tracks = engine.loadTracks();

        int btnY = this.height - 60;
        int btnW = 60;
        int startX = (this.width - 198) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("|<"), btn ->
                engine.previous()
        ).dimensions(startX, btnY, btnW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">/||"), btn -> {
            if (engine.isPlaying()) {
                engine.pause();
            } else if (engine.getCurrentIndex() >= 0) {
                engine.resume();
            } else if (!tracks.isEmpty()) {
                engine.play(0);
            }
        }).dimensions(startX + 69, btnY, btnW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">|"), btn ->
                engine.next()
        ).dimensions(startX + 138, btnY, btnW, 20).build());

        this.addDrawableChild(new SliderWidget(
                10, this.height - 30, this.width - 20, 20,
                Text.literal("Volume: " + (int)(engine.getVolume() * 100) + "%"),
                engine.getVolume()
        ) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Volume: " + (int)(value * 100) + "%"));
            }
            @Override protected void applyValue() {
                engine.setVolume((float) value);
            }
        });

        // Botões para cada música visível
        for (int i = 0; i < VISIBLE_TRACKS; i++) {
            final int index = i;
            int trackY = 50 + i * TRACK_HEIGHT;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(""), btn -> {
                int realIndex = scrollOffset + index;
                if (realIndex < tracks.size()) {
                    MusicPlayerClient.audioEngine.play(realIndex);
                }
            }).dimensions(5, trackY, this.width - 10, TRACK_HEIGHT - 2).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        AudioEngine engine = MusicPlayerClient.audioEngine;

        ctx.fill(0, 0, this.width, this.height, 0xCC000000);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("== Music Player =="), this.width / 2, 8, 0xFFFFFFFF);

        String status = engine.isLoading() ? "Carregando..." :
                engine.isPlaying() ? "> " + shorten(engine.getCurrentTrackName()) :
                !engine.getCurrentTrackName().isEmpty() ? "|| " + shorten(engine.getCurrentTrackName()) :
                "Nenhuma musica";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), this.width / 2, 20, 0xFFFFFF55);

        // Barra de progresso
        int bw = this.width - 20;
        ctx.fill(10, 35, 10 + bw, 40, 0xFF333333);
        ctx.fill(10, 35, 10 + (int)(engine.getProgress() * bw), 40, 0xFF5599FF);

        // Lista de músicas
        if (tracks.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Sem musicas!"), this.width / 2, 80, 0xFFFF5555);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(PlatformHelper.getMusicFolderDisplay()), this.width / 2, 95, 0xFFFFFF00);
        } else {
            for (int i = 0; i < VISIBLE_TRACKS; i++) {
                int ri = scrollOffset + i;
                if (ri >= tracks.size()) break;
                int ty = 50 + i * TRACK_HEIGHT;
                boolean sel = ri == engine.getCurrentIndex();
                ctx.fill(5, ty, this.width - 5, ty + TRACK_HEIGHT - 2, sel ? 0x882255AA : 0x44FFFFFF);
                ctx.drawTextWithShadow(textRenderer, Text.literal(shorten(tracks.get(ri))), 10, ty + 5, sel ? 0xFFFFFF55 : 0xFFCCCCCC);
            }
        }

        super.render(ctx, mx, my, delta);
    }

    private String shorten(String s) {
        int max = (this.width / 6) - 2;
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollOffset -= (int) v;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, tracks.size() - VISIBLE_TRACKS)));
        this.clearAndInit();
        return true;
    }

    @Override public boolean shouldPause() { return false; }
}
