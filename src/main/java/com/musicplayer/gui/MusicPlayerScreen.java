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
    private static final int VISIBLE_TRACKS = 7;
    private static final int TRACK_HEIGHT = 22;
    private static final int HEADER_H = 48;
    private static final int FOOTER_H = 70;
    public MusicPlayerScreen() { super(Text.literal("Music Player")); }
    @Override
    protected void init() {
        tracks = MusicPlayerClient.audioEngine.loadTracks();
        AudioEngine engine = MusicPlayerClient.audioEngine;
        int cy = this.height - FOOTER_H + 8;
        int bw = 50; int cx = this.width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("|<"), b -> engine.previous()).dimensions(cx-bw-54,cy,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">/||"), b -> { if(engine.isPlaying()) engine.pause(); else if(engine.getCurrentIndex()>=0) engine.resume(); else if(!tracks.isEmpty()) engine.play(0); }).dimensions(cx-bw/2,cy,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">|"), b -> engine.next()).dimensions(cx+54,cy,bw,20).build());
        addDrawableChild(new SliderWidget(10,this.height-30,this.width-20,18,Text.literal("Volume: "+(int)(engine.getVolume()*100)+"%"),engine.getVolume()){
            @Override protected void updateMessage(){setMessage(Text.literal("Volume: "+(int)(value*100)+"%"));}
            @Override protected void applyValue(){engine.setVolume((float)value);}
        });
    }
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        AudioEngine engine = MusicPlayerClient.audioEngine;
        ctx.fill(0,0,this.width,this.height,0xEE111122);
        ctx.fill(0,0,this.width,HEADER_H,0xFF1a1a3a);
        ctx.fill(0,HEADER_H-1,this.width,HEADER_H,0xFF4444AA);
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("Music Player"),this.width/2,6,0xFF9999FF);
        String np = engine.isLoading()?"Carregando...":engine.isPlaying()?"▶ "+shorten(engine.getCurrentTrackName(),40):!engine.getCurrentTrackName().isEmpty()?"|| "+shorten(engine.getCurrentTrackName(),40):"Nenhuma musica";
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal(np),this.width/2,20,0xFFFFFF55);
        int bx=10,by=35,bw=this.width-20;
        ctx.fill(bx,by,bx+bw,by+5,0xFF222244);
        ctx.fill(bx,by,bx+(int)(engine.getProgress()*bw),by+5,0xFF5577FF);
        if(engine.getDurationMs()>0) ctx.drawCenteredTextWithShadow(textRenderer,Text.literal(AudioEngine.formatTime(engine.getPositionMs())+" / "+AudioEngine.formatTime(engine.getDurationMs())),this.width/2,42,0xFF888888);
        int listTop=HEADER_H+4;
        ctx.fill(0,listTop-4,this.width,this.height-FOOTER_H+4,0xFF0d0d1a);
        if(tracks.isEmpty()){
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("Sem musicas!"),this.width/2,listTop+20,0xFFFF5555);
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal(PlatformHelper.getMusicFolderDisplay()),this.width/2,listTop+40,0xFFFFFF44);
        } else {
            for(int i=0;i<VISIBLE_TRACKS;i++){
                int ri=scrollOffset+i; if(ri>=tracks.size()) break;
                int ty=listTop+i*TRACK_HEIGHT;
                boolean sel=ri==engine.getCurrentIndex();
                boolean hov=mx>=5&&mx<=this.width-5&&my>=ty&&my<ty+TRACK_HEIGHT;
                if(sel) ctx.fill(5,ty,this.width-5,ty+TRACK_HEIGHT-1,0xAA224488);
                else if(hov) ctx.fill(5,ty,this.width-5,ty+TRACK_HEIGHT-1,0x55334466);
                if(sel) ctx.fill(5,ty,8,ty+TRACK_HEIGHT-1,0xFF5577FF);
                ctx.drawTextWithShadow(textRenderer,Text.literal((ri+1)+". "+shorten(tracks.get(ri),50)),14,ty+6,sel?0xFFFFFF55:hov?0xFFDDDDFF:0xFFBBBBCC);
            }
        }
        ctx.fill(0,this.height-FOOTER_H,this.width,this.height-FOOTER_H+1,0xFF4444AA);
        ctx.fill(0,this.height-FOOTER_H,this.width,this.height,0xFF111128);
        super.render(ctx,mx,my,delta);
    }
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int listTop=HEADER_H+4;
        if(mx>=5&&mx<=this.width-5&&my>=listTop&&my<this.height-FOOTER_H){
            int i=(int)((my-listTop)/TRACK_HEIGHT);
            int ri=scrollOffset+i;
            if(ri>=0&&ri<tracks.size()){MusicPlayerClient.audioEngine.play(ri);return true;}
        }
        return super.mouseClicked(mx,my,btn);
    }
    @Override public boolean mouseScrolled(double mx,double my,double h,double v){scrollOffset-=(int)v;scrollOffset=Math.max(0,Math.min(scrollOffset,Math.max(0,tracks.size()-VISIBLE_TRACKS)));return true;}
    private String shorten(String s,int max){return s.length()>max?s.substring(0,max-3)+"...":s;}
    @Override public boolean shouldPause(){return false;}
}
