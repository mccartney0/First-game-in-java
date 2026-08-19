package com.traduvertgames.main;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/** Reprodutor de fala por personagem e linha, separado dos efeitos curtos. */
public final class VoiceManager {

    private static final Map<String, String> OVERRIDES = new HashMap<String, String>();
    private static final Map<String, Clip> CLIPS = new HashMap<String, Clip>();
    private static Clip currentClip;

    private VoiceManager() {
    }

    public static void playDialogueLine(String speaker, int lineIndex) {
        if (!OptionsConfig.isSoundEnabled()) {
            return;
        }
        String key = key(speaker, lineIndex);
        String resource = OVERRIDES.get(key);
        if (resource == null) {
            resource = defaultResource(speaker, lineIndex);
        }
        playResource(key, resource);
    }

    /** Registra um arquivo específico sem alterar os diálogos ou o código de gameplay. */
    public static synchronized void registerLine(String speaker, int lineIndex, String resourcePath) {
        if (speaker == null || resourcePath == null || resourcePath.isEmpty()) {
            return;
        }
        String key = key(speaker, lineIndex);
        OVERRIDES.put(key, resourcePath);
        closeClip(key);
    }

    public static synchronized void stop() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip = null;
        }
    }

    public static synchronized void unload() {
        stop();
        for (Clip clip : CLIPS.values()) {
            if (clip != null) {
                clip.close();
            }
        }
        CLIPS.clear();
    }

    public static String resourceFor(String speaker, int lineIndex) {
        String key = key(speaker, lineIndex);
        return OVERRIDES.containsKey(key) ? OVERRIDES.get(key) : defaultResource(speaker, lineIndex);
    }

    private static synchronized void playResource(String key, String resource) {
        Clip clip = CLIPS.get(key);
        if (clip == null) {
            clip = load(resource);
            if (clip == null) {
                return;
            }
            CLIPS.put(key, clip);
        }
        if (currentClip != null && currentClip != clip) {
            currentClip.stop();
        }
        clip.setFramePosition(0);
        applyVolume(clip);
        clip.start();
        currentClip = clip;
    }

    private static Clip load(String resource) {
        try (InputStream input = VoiceManager.class.getResourceAsStream(resource)) {
            if (input == null) {
                return null;
            }
            AudioInputStream audio = AudioSystem.getAudioInputStream(input);
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            return clip;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void applyVolume(Clip clip) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), OptionsConfig.getSoundVolume())));
        }
    }

    private static void closeClip(String key) {
        Clip clip = CLIPS.remove(key);
        if (clip != null) {
            clip.close();
        }
    }

    private static String key(String speaker, int lineIndex) {
        return slug(speaker) + ":" + Math.max(0, lineIndex);
    }

    private static String defaultResource(String speaker, int lineIndex) {
        String language = Localization.getLanguage().getCode();
        return "/audio/voices/" + language + "/" + slug(speaker) + "/line_" + Math.max(0, lineIndex) + ".wav";
    }

    private static String slug(String speaker) {
        if (speaker == null || speaker.trim().isEmpty()) {
            return "default";
        }
        String normalized = java.text.Normalizer.normalize(speaker.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9]+", "_");
        return normalized.replaceAll("^_+|_+$", "");
    }
}
