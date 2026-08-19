package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.Localization;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.VoiceManager;
import com.traduvertgames.world.RpgWorldManager;

/** Integração da troca de idioma em runtime com o save da sessão atual. */
class LanguageRuntimeIntegrationTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.setRegionalAdventureMode(true);
        Game.getInstance().loadRegionalAdventure(1);
        Localization.setLanguage(Localization.Language.PT_BR);
    }

    @AfterEach
    void tearDown() {
        Localization.setLanguage(Localization.Language.PT_BR);
        RpgWorldManager.disable();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void switchesTextAndVoicePathDynamicallyBetweenPortugueseAndEnglish() {
        assertEquals("Opções", Localization.tr("menu.options"));
        assertTrue(VoiceManager.resourceFor("Comandante Ava", 0)
                .contains("/pt-BR/comandante_ava/line_0.wav"));

        Localization.setLanguage(Localization.Language.EN_US);
        assertEquals("Options", Localization.tr("menu.options"));
        assertEquals("Commander Ava", Localization.localizeCharacter("Comandante Ava"));
        assertEquals("Pilot, the colony needs you. This is the operation plan.",
                Localization.tr("dialogue.comandante_ava.0"));
        assertTrue(VoiceManager.resourceFor("Comandante Ava", 0)
                .contains("/en-US/comandante_ava/line_0.wav"));

        Localization.setLanguage(Localization.Language.PT_BR);
        assertEquals("Opções", Localization.tr("menu.options"));
        assertEquals("Comandante Ava", Localization.localizeCharacter("Comandante Ava"));
    }

    @Test
    void selectedLanguageSurvivesSaveRoundTrip() throws Exception {
        Localization.setLanguage(Localization.Language.EN_US);
        SaveManager.activeSlot = 1;
        assertTrue(SaveManager.saveCurrentGame());

        Localization.setLanguage(Localization.Language.PT_BR);
        assertTrue(SaveManager.loadSlot(1));
        assertEquals(Localization.Language.EN_US, Localization.getLanguage());
        assertEquals("Options", Localization.tr("menu.options"));
    }
}
