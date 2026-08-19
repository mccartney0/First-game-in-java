package com.traduvertgames.quest;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.OpenWorldManager;
import com.traduvertgames.main.WaveManager;

/**
 * Objetivo aberto da superfície RPG ou do modo sobrevivência. Não tem conclusão
 * automática: o jogador decide quando explorar, iniciar evento ou sair.
 */
public final class NullObjective extends BaseObjective {
    public NullObjective() {
        super(Game.isOpenWorldMode() ? "Mundo Aberto gigante"
                : Game.isRegionalAdventureMode() ? "Aventura RPG regional" : "Modo Sobrevivência",
                Game.isOpenWorldMode()
                        ? "Explore setores, descubra POIs e aceite eventos sem uma rota linear."
                        : Game.isRegionalAdventureMode()
                                ? "Explore as regiões, ajude os NPCs e escolha atividades no hub."
                                : "Resista às ondas infinitas — sobreviva o máximo que puder.");
    }

    @Override
    public String getProgressText() {
        if (Game.isOpenWorldMode()) {
            String region = RpgWorldManager.getCurrentRegionName();
            if (DynamicEventManager.isActive()) {
                return DynamicEventManager.getActiveTitle() + " — " + DynamicEventManager.getProgressLabel();
            }
            return region + " — " + OpenWorldManager.getExplorationLabel() + " — H: hub, M: marcador";
        }
        if (Game.isRegionalAdventureMode()) {
            String region = RpgWorldManager.getCurrentRegionName();
            if (DynamicEventManager.isActive()) {
                return DynamicEventManager.getActiveTitle() + " — " + DynamicEventManager.getProgressLabel();
            }
            return region + " — H: abrir hub";
        }
        return WaveManager.getSurvivalSummary() + " — auto-fire, colete XP e sobreviva ao chefe";
    }

    @Override
    public boolean isComplete() {
        return false;
    }

}
