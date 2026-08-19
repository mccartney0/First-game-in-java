package com.traduvertgames.quest;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RpgWorldManager;

/**
 * Objetivo aberto da superfície RPG ou do modo sobrevivência. Não tem conclusão
 * automática: o jogador decide quando explorar, iniciar evento ou sair.
 */
public final class NullObjective extends BaseObjective {
    public NullObjective() {
        super(Game.isRegionalAdventureMode() ? "Aventura RPG regional" : "Modo Sobrevivência",
                Game.isRegionalAdventureMode()
                        ? "Explore as regiões, ajude os NPCs e escolha atividades no hub."
                        : "Resista às ondas infinitas — sobreviva o máximo que puder.");
    }

    @Override
    public String getProgressText() {
        if (Game.isRegionalAdventureMode()) {
            String region = RpgWorldManager.getCurrentRegionName();
            if (DynamicEventManager.isActive()) {
                return DynamicEventManager.getActiveTitle() + " — " + DynamicEventManager.getProgressLabel();
            }
            return region + " — H: abrir hub";
        }
        return "Ondas: " + WaveTracker.getCurrentWave();
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    /** Acesso simples ao contador de ondas do WaveManager para o texto de progresso. */
    private static final class WaveTracker {
        private WaveTracker() {
        }

        private static int getCurrentWave() {
            try {
                return com.traduvertgames.main.WaveManager.getCurrentWaveNumber();
            } catch (Throwable ex) {
                return 0;
            }
        }
    }
}
