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
    private static final int TRACK_HEIGHT = 18;

    private int winX, winY, winW, winH;

    public MusicPlayerScreen() {
        super(Text.literal("♪ Music Player"));
    }

    @Override
    protected void init() {
        AudioEngine engine = MusicPlayerClient.audioEngine;
        tracks = engine.loadTracks();

        winW = 270;
        winH = 240;
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        int btnY = winY + winH - 58;
        int btnW = 52;
        int spacing = 6;
        int totalW = 3 * btnW + 2 * spacing;
        int startX = winX + (winW - totalW) / 2;

        // ⏮ Anterior
        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏮"), btn ->
                engine.previous()
        ).dimensions(startX, btnY, btnW, 20).build());

        // ⏯ Play/Pause
        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏯"), btn -> {
            if (engine.isPlaying()) {
                engine.pause();
            } else if (engine.getCurrentIndex() >= 0) {
                engine.resume();
            } else if (!tracks.isEmpty()) {
                engine.play(0);
            }
        }).dimensions(startX + btnW + spacing, btnY, btnW, 20).build());

        // ⏭ Próxima
        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏭"), btn ->
                engine.next()
        ).dimensions(startX + (btnW + spacing) * 2, btnY, btnW, 20).build());

        // Slider de volume
        this.addDrawableChild(new SliderWidget(
                winX + 10, winY + winH - 28, winW - 20, 16,
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
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        AudioEngine engine = MusicPlayerClient.audioEngine;

        // Fundo
        ctx.fill(winX, winY, winX + winW, winY + winH, 0xD01a1a2e);
        ctx.drawBorder(winX, winY, winW, winH, 0xFF5555ff);

        // Título + plataforma
        String platform = PlatformHelper.isAndroid() ? " [Android]" : " [PC]";
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("♪ Music Player" + platform),
                winX + winW / 2, winY + 6, 0xFF9999FF);

        // Separador
        ctx.fill(winX + 5, winY + 18, winX + winW - 5, winY + 19, 0xFF5555ff);

        // Nome da música atual
        String trackLabel;
        if (engine.isLoading()) {
            trackLabel = "⏳ Carregando...";
        } else if (!engine.getCurrentTrackName().isEmpty()) {
            String icon = engine.isPlaying() ? "▶ " : "⏸ ";
            String name = engine.getCurrentTrackName();
            if (name.length() > 32) name = name.substring(0, 29) + "...";
            trackLabel = icon + name;
        } else {
            trackLabel = "Nenhuma música tocando";
        }
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(trackLabel),
                winX + winW / 2, winY + 23, 0xFFFFFF55);

        // Tempo (posição / duração)
        if (engine.getDurationMs() > 0) {
            String time = AudioEngine.formatTime(engine.getPositionMs())
                    + " / " + AudioEngine.formatTime(engine.getDurationMs());
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(time),
                    winX + winW / 2, winY + 34, 0xFFAAAAAA);
        }

        // Barra de progresso
        int barX = winX + 10, barY = winY + 44;
        int barW = winW - 20;
        ctx.fill(barX, barY, barX + barW, barY + 5, 0xFF333355);
        int prog = (int)(engine.getProgress() * barW);
        ctx.fill(barX, barY, barX + prog, barY + 5, 0xFF5577FF);

        // Lista de músicas
        int listTop = winY + 54;
        int listH = VISIBLE_TRACKS * TRACK_HEIGHT;
        ctx.enableScissor(winX + 5, listTop, winX + winW - 5, listTop + listH);

        if (tracks.isEmpty()) {
            // Mostra o caminho correto para a plataforma atual
            String folderPath = PlatformHelper.getMusicFolderDisplay();
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Nenhuma música encontrada!"),
                    winX + winW / 2, listTop + 10, 0xFFFF5555);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Coloque .ogg ou .mp3 em:"),
                    winX + winW / 2, listTop + 24, 0xFFAAAAAA);
            // Quebra caminho longo em duas linhas
            if (folderPath.length() > 35) {
                int mid = folderPath.lastIndexOf('/', 35);
                if (mid < 0) mid = 35;
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(folderPath.substring(0, mid + 1)),
                        winX + winW / 2, listTop + 36, 0xFFFFFF55);
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(folderPath.substring(mid + 1)),
                        winX + winW / 2, listTop + 48, 0xFFFFFF55);
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(folderPath),
                        winX + winW / 2, listTop + 36, 0xFFFFFF55);
            }
        } else {
            for (int i = scrollOffset; i < Math.min(scrollOffset + VISIBLE_TRACKS, tracks.size()); i++) {
                int ty = listTop + (i - scrollOffset) * TRACK_HEIGHT;
                boolean selected = (i == engine.getCurrentIndex());
                boolean hovered = mx >= winX + 5 && mx <= winX + winW - 5
                        && my >= ty && my < ty + TRACK_HEIGHT;

                if (selected)      ctx.fill(winX+5, ty, winX+winW-5, ty+TRACK_HEIGHT-1, 0x882255AA);
                else if (hovered)  ctx.fill(winX+5, ty, winX+winW-5, ty+TRACK_HEIGHT-1, 0x44FFFFFF);

                String tn = tracks.get(i);
                if (tn.length() > 34) tn = tn.substring(0, 31) + "...";
                ctx.drawTextWithShadow(textRenderer, Text.literal(tn),
                        winX + 10, ty + 4, selected ? 0xFFFFFF55 : 0xFFCCCCCC);
            }
        }
        ctx.disableScissor();

        // Separador antes dos botões
        ctx.fill(winX+5, winY+winH-63, winX+winW-5, winY+winH-62, 0xFF5555ff);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int listTop = winY + 54;
        if (mx >= winX+5 && mx <= winX+winW-5
                && my >= listTop && my < listTop + VISIBLE_TRACKS * TRACK_HEIGHT) {
            int clicked = scrollOffset + (int)((my - listTop) / TRACK_HEIGHT);
            if (clicked >= 0 && clicked < tracks.size()) {
                MusicPlayerClient.audioEngine.play(clicked);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        scrollOffset -= (int) vAmt;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, tracks.size() - VISIBLE_TRACKS)));
        return true;
    }

    @Override public boolean shouldPause() { return false; }
}
