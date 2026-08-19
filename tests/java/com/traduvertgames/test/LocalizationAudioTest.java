package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Localization;
import com.traduvertgames.main.VoiceManager;

class LocalizationAudioTest {

    @AfterEach
    void restorePortuguese() {
        Localization.setLanguage(Localization.Language.PT_BR);
    }

    @Test
    void languageCatalogSwitchesAndFallsBack() {
        Localization.setLanguage(Localization.Language.PT_BR);
        assertEquals("Opções", Localization.tr("menu.options"));
        Localization.setLanguage(Localization.Language.EN_US);
        assertEquals("Options", Localization.tr("menu.options"));
        assertEquals("texto original", Localization.trOr("missing.key", "texto original"));
    }

    @Test
    void voicePathIsStableAndCanBeOverridden() {
        Localization.setLanguage(Localization.Language.PT_BR);
        assertTrue(VoiceManager.resourceFor("Comandante Ava", 0)
                .endsWith("/pt-BR/comandante_ava/line_0.wav"));
        VoiceManager.registerLine("Comandante Ava", 0, "/audio/custom/ava_line.wav");
        assertEquals("/audio/custom/ava_line.wav", VoiceManager.resourceFor("Comandante Ava", 0));
    }

    @Test
    void languageCodeRoundTrips() {
        Localization.deserialize("en-US");
        assertEquals("en-US", Localization.serialize());
        Localization.deserialize("invalid");
        assertEquals("pt-BR", Localization.serialize());
    }
}
