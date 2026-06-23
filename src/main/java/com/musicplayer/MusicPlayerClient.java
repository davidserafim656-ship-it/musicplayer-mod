package com.musicplayer;

import com.musicplayer.audio.AudioEngine;
import com.musicplayer.gui.MusicPlayerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class MusicPlayerClient implements ClientModInitializer {

    public static KeyBinding openPlayerKey;
    public static AudioEngine audioEngine;

    @Override
    public void onInitializeClient() {
        // Registra a tecla M para abrir o player
        openPlayerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.musicplayer.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.musicplayer"
        ));

        // Inicializa o engine de áudio
        audioEngine = new AudioEngine();

        // Verifica o pressionamento da tecla a cada tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPlayerKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MusicPlayerScreen());
                }
            }
        });
    }
}
