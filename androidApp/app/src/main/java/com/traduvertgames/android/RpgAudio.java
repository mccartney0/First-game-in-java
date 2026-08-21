package com.traduvertgames.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * Camada de sonificação de baixa latência do RPG. Os efeitos são curtos,
 * carregados uma vez e nunca participam do loop de renderização.
 */
final class RpgAudio {
    private final SoundPool soundPool;
    private final int stepGrass;
    private final int magicCast;
    private final int arcaneImpact;
    private final int dialogueOpen;
    private final int uiConfirm;
    private final int achievement;
    private boolean released;

    RpgAudio(Context context) {
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

    void pause() {
        if (!released) soundPool.autoPause();
    }

    void resume() {
        if (!released) soundPool.autoResume();
    }

    void release() {
        if (!released) {
            released = true;
            soundPool.release();
        }
    }

    private void play(int soundId, float left, float right, float rate) {
        if (!released && soundId != 0) soundPool.play(soundId, left, right, 1, 0, rate);
    }
}
