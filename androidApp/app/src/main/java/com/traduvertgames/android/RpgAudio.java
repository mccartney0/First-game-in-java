package com.traduvertgames.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

/**
 * Camada de sonificação de baixa latência do RPG. Os efeitos são curtos,
 * carregados uma vez e nunca participam do loop de renderização.
 */
final class RpgAudio {
    enum MusicTrack {
        CLEARING(R.raw.rpg_music_clearing),
        NORTH_WATERS(R.raw.rpg_music_northwaters),
        FORTRESS(R.raw.rpg_music_fortress),
        BOSS(R.raw.rpg_music_boss),
        COMBAT_LOW(R.raw.rpg_music_combat_low),
        COMBAT_MEDIUM(R.raw.rpg_music_combat_mid),
        COMBAT_CRITICAL(R.raw.rpg_music_combat_critical),
        AVA(R.raw.rpg_music_npc_ava),
        ORIN(R.raw.rpg_music_npc_orin),
        ILYRA(R.raw.rpg_music_npc_ilyra);

        final int resourceId;

        MusicTrack(int resourceId) {
            this.resourceId = resourceId;
        }
    }

    private final Context context;
    private final SoundPool soundPool;
    private final int stepGrass;
    private final int magicCast;
    private final int arcaneImpact;
    private final int dialogueOpen;
    private final int uiConfirm;
    private final int achievement;
    private MediaPlayer musicPlayer;
    private MediaPlayer victoryPlayer;
    private MusicTrack activeTrack;
    private MusicTrack pendingTrack;
    private float musicFade = 1f;
    private float musicDuck = 1f;
    private float victoryDuckTimer;
    private boolean musicEnabled = true;
    private boolean musicPaused;
    private boolean released;

    RpgAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(5)
                .build();
        stepGrass = soundPool.load(context, R.raw.rpg_step_grass, 1);
        magicCast = soundPool.load(context, R.raw.rpg_magic_cast, 1);
        arcaneImpact = soundPool.load(context, R.raw.rpg_arcane_impact, 1);
        dialogueOpen = soundPool.load(context, R.raw.rpg_dialogue_open, 1);
        uiConfirm = soundPool.load(context, R.raw.rpg_ui_confirm, 1);
        achievement = soundPool.load(context, R.raw.rpg_achievement, 1);
    }

    void playStep() {
        play(stepGrass, 0.26f, 0.24f, 0.92f);
    }

    void playMagicCast() {
        play(magicCast, 0.52f, 0.48f, 1f);
    }

    void playArcaneImpact(boolean boss) {
        play(arcaneImpact, boss ? 0.70f : 0.44f, boss ? 0.66f : 0.42f, boss ? 0.82f : 1f);
    }

    void playDialogue() {
        play(dialogueOpen, 0.36f, 0.33f, 1f);
    }

    void playUiConfirm() {
        play(uiConfirm, 0.28f, 0.26f, 1f);
    }

    void playAchievement() {
        play(achievement, 0.50f, 0.46f, 1f);
    }

    void playBossVictory() {
        if (released || !musicEnabled) return;
        victoryDuckTimer = 2.4f;
        releaseVictoryPlayer();
        victoryPlayer = MediaPlayer.create(context, R.raw.rpg_music_boss_victory);
        if (victoryPlayer == null) return;
        victoryPlayer.setVolume(0.46f, 0.46f);
        victoryPlayer.setOnCompletionListener(player -> {
            if (victoryPlayer == player) victoryPlayer = null;
            player.release();
        });
        if (!musicPaused) victoryPlayer.start();
    }

    void updateMusic(MusicTrack requestedTrack, float dt) {
        if (released || !musicEnabled || requestedTrack == null) return;
        victoryDuckTimer = Math.max(0f, victoryDuckTimer - dt);
        float targetDuck = victoryDuckTimer > 0f ? 0.38f : 1f;
        musicDuck += (targetDuck - musicDuck) * Math.min(1f, dt / 0.16f);
        if (musicPlayer == null) {
            startMusic(requestedTrack, 0f);
            activeTrack = requestedTrack;
            musicFade = 0f;
        } else if (requestedTrack != activeTrack && requestedTrack != pendingTrack) {
            pendingTrack = requestedTrack;
        }
        if (pendingTrack != null) {
            musicFade = Math.max(0f, musicFade - dt / 0.28f);
            applyMusicVolume();
            if (musicFade <= 0f) {
                startMusic(pendingTrack, 0f);
                activeTrack = pendingTrack;
                pendingTrack = null;
            }
        } else if (musicFade < 1f) {
            musicFade = Math.min(1f, musicFade + dt / 0.38f);
        }
        applyMusicVolume();
    }

    boolean toggleMusic() {
        if (released) return false;
        musicEnabled = !musicEnabled;
        if (musicPlayer != null) {
            if (musicEnabled && !musicPaused) musicPlayer.start();
            else if (musicPlayer.isPlaying()) musicPlayer.pause();
        }
        if (victoryPlayer != null) {
            if (musicEnabled && !musicPaused) victoryPlayer.start();
            else if (victoryPlayer.isPlaying()) victoryPlayer.pause();
        }
        return musicEnabled;
    }

    boolean isMusicEnabled() {
        return musicEnabled;
    }

    void pause() {
        if (!released) {
            soundPool.autoPause();
            musicPaused = true;
            if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
            if (victoryPlayer != null && victoryPlayer.isPlaying()) victoryPlayer.pause();
        }
    }

    void resume() {
        if (!released) {
            soundPool.autoResume();
            musicPaused = false;
            if (musicEnabled && musicPlayer != null) musicPlayer.start();
            if (musicEnabled && victoryPlayer != null) victoryPlayer.start();
        }
    }

    void release() {
        if (!released) {
            released = true;
            soundPool.release();
            releaseMusicPlayer();
            releaseVictoryPlayer();
        }
    }

    private void play(int soundId, float left, float right, float rate) {
        if (!released && soundId != 0) soundPool.play(soundId, left, right, 1, 0, rate);
    }

    private void startMusic(MusicTrack track, float initialVolume) {
        releaseMusicPlayer();
        musicPlayer = MediaPlayer.create(context, track.resourceId);
        if (musicPlayer == null) return;
        musicPlayer.setLooping(true);
        musicPlayer.setVolume(initialVolume, initialVolume);
        if (!musicPaused && musicEnabled) musicPlayer.start();
    }

    private void applyMusicVolume() {
        if (musicPlayer != null) {
            float volume = 0.25f * musicFade * musicDuck;
            musicPlayer.setVolume(volume, volume);
        }
    }

    private void releaseMusicPlayer() {
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    private void releaseVictoryPlayer() {
        if (victoryPlayer != null) {
            victoryPlayer.release();
            victoryPlayer = null;
        }
    }
}
