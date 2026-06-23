# 🎵 Music Player Mod — Minecraft Java 1.21.4 (Fabric)

Toque suas músicas dentro do Minecraft — funciona no **PC e no Android**!

---

## 📱 Launchers Android suportados

| Launcher | Suporte | Pasta das músicas |
|----------|---------|-------------------|
| PojavLauncher | ✅ | `/sdcard/games/PojavLauncher/.minecraft/music_player/` |
| Zalith Launcher | ✅ | `/sdcard/Android/data/net.kdt.pojavlaunch/files/.minecraft/music_player/` |
| Mojo Launcher | ✅ | `/sdcard/MojoLauncher/.minecraft/music_player/` |
| PC (Windows/Linux/Mac) | ✅ | `.minecraft/music_player/` |

> 💡 O mod detecta automaticamente a plataforma. A interface mostra o caminho correto quando não há músicas.

---

## 📋 Requisitos

- Minecraft Java Edition **1.21.4**
- Fabric Loader **0.16.10+**
- Fabric API **0.114.0+**
- Java **21** (PC) / **Java 17+** (Android via launcher)

---

## 🔨 Como compilar

```bash
# Windows
gradlew.bat build

# Linux / Mac / Termux (Android)
./gradlew build
```

O `.jar` fica em `build/libs/musicplayer-1.0.0.jar`

---

## 🎵 Como usar

### PC
1. Copie o `.jar` para `.minecraft/mods/`
2. Coloque músicas em `.minecraft/music_player/`
3. No jogo, pressione **M**

### Android (PojavLauncher)
1. Abra o PojavLauncher → instale Fabric 1.21.4
2. Coloque o `.jar` em `Mods` dentro do launcher
3. Coloque músicas na pasta mostrada na tela do mod
4. No jogo, pressione **M** (ou use teclado virtual)

---

## 🎧 Formatos suportados

| Formato | PC | Android |
|---------|-----|---------|
| `.ogg`  | ✅ javax.sound | ✅ STBVorbis (LWJGL) |
| `.mp3`  | ✅ JLayer | ✅ JLayer |

---

## 🏗️ Arquitetura

```
AudioEngine           ← gerencia playlist e estados
  ├── PlatformHelper  ← detecta Android/PC e retorna caminhos
  ├── IAudioBackend   ← interface comum
  ├── DesktopAudioBackend  ← javax.sound (PC)
  └── AndroidAudioBackend ← OpenAL/STBVorbis (Android)
```

## 📁 Estrutura do projeto

```
src/main/java/com/musicplayer/
├── MusicPlayerClient.java
├── audio/
│   ├── AudioEngine.java
│   ├── IAudioBackend.java
│   ├── DesktopAudioBackend.java
│   ├── AndroidAudioBackend.java
│   └── PlatformHelper.java
└── gui/
    └── MusicPlayerScreen.java
```
