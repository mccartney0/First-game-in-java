package com.traduvertgames.world;

import java.awt.Color;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.RegionalNpcs;
import com.traduvertgames.graficos.MissionBanner;
import com.traduvertgames.main.Game;

/**
 * Progressão opcional de cada região. O jogador pode explorar atividades livres,
 * mas o hub oferece uma trilha explícita que conecta resgate, NPC, comboio e dungeon.
 */
public final class RegionalChainManager {

    public enum Stage {
        RESCUE,
        NPC,
        SUPPLY_CONVOY,
        DUNGEON,
        COMPLETE
    }

    private static final class ChainState {
        boolean rescueCompleted;
        boolean npcCompleted;
        boolean convoyCompleted;
        boolean dungeonCompleted;
    }

    private static final EnumMap<RpgWorldManager.RegionType, ChainState> states =
            new EnumMap<RpgWorldManager.RegionType, ChainState>(RpgWorldManager.RegionType.class);

    private RegionalChainManager() {
    }

    public static void reset() {
        states.clear();
    }

    public static Stage getStage(RpgWorldManager.RegionType region) {
        if (region == null) {
            return Stage.RESCUE;
        }
        ChainState state = stateFor(region);
        if (DungeonManager.isRegionCompleted(region)) {
            state.dungeonCompleted = true;
        }
        if (!state.rescueCompleted) {
            return Stage.RESCUE;
        }
        if (!state.npcCompleted) {
            return Stage.NPC;
        }
        if (!state.convoyCompleted) {
            return Stage.SUPPLY_CONVOY;
        }
        if (!state.dungeonCompleted) {
            return Stage.DUNGEON;
        }
        return Stage.COMPLETE;
    }

    public static boolean isComplete(RpgWorldManager.RegionType region) {
        return getStage(region) == Stage.COMPLETE;
    }

    public static String getProgressLabel(RpgWorldManager.RegionType region) {
        if (region == null) {
            return "Cadeia regional: 0/4";
        }
        ChainState state = stateFor(region);
        if (DungeonManager.isRegionCompleted(region)) {
            state.dungeonCompleted = true;
        }
        int completed = (state.rescueCompleted ? 1 : 0) + (state.npcCompleted ? 1 : 0)
                + (state.convoyCompleted ? 1 : 0) + (state.dungeonCompleted ? 1 : 0);
        return "Cadeia regional: " + completed + "/4";
    }

    public static String getNextActionTitle(RpgWorldManager.RegionType region) {
        switch (getStage(region)) {
        case RESCUE:
            return "Iniciar RESCUE";
        case NPC:
            return "Falar com " + RegionalNpcs.getNameForRegion(region);
        case SUPPLY_CONVOY:
            return "Escoltar SUPPLY_CONVOY";
        case DUNGEON:
            return "Entrar na dungeon regional";
        case COMPLETE:
        default:
            return "Cadeia concluída — explorar livremente";
        }
    }

    public static String getSummary(RpgWorldManager.RegionType region) {
        if (region == null) {
            return "Cadeia regional indisponível";
        }
        return getProgressLabel(region) + " | Próximo: " + getNextActionTitle(region);
    }

    /**
     * Aciona o próximo passo pelo hub. As atividades também continuam disponíveis
     * pela exploração livre; esta entrada garante uma trilha clara para o jogador.
     */
    public static boolean startNextStep(RpgWorldManager.RegionType region) {
        if (region == null || Game.player == null || !RpgWorldManager.isActive()
                || RpgWorldManager.isDungeonMode() || RpgWorldManager.getCurrentRegion() != region) {
            return false;
        }
        Stage stage = getStage(region);
        switch (stage) {
        case RESCUE:
            return DynamicEventManager.startEventForCurrentRegion(DynamicEventManager.Type.RESCUE);
        case NPC:
            RegionalNpcs.activateQuestForRegion(region);
            MissionBanner.show("CADEIA REGIONAL", "Fale com " + RegionalNpcs.getNameForRegion(region)
                    + " e conclua a missão de preparação do comboio.",
                    new Color(129, 199, 132), Color.WHITE, 180);
            return true;
        case SUPPLY_CONVOY:
            return DynamicEventManager.startEventForCurrentRegion(DynamicEventManager.Type.SUPPLY_CONVOY);
        case DUNGEON:
            if (DungeonManager.isRegionCompleted(region)) {
                stateFor(region).dungeonCompleted = true;
                return true;
            }
            boolean requested = DungeonManager.requestEnter(region);
            if (requested) {
                MissionBanner.show("CADEIA REGIONAL", "Comboio concluído: dungeon regional desbloqueada.",
                        new Color(220, 80, 255), Color.WHITE, 150);
            }
            return requested;
        case COMPLETE:
        default:
            MissionBanner.show("CADEIA CONCLUÍDA", "A região foi estabilizada. Explore novos contratos e dungeons.",
                    new Color(255, 214, 10), Color.WHITE, 150);
            return true;
        }
    }

    /** Callback de sucesso do evento RESCUE. */
    public static void onRescueCompleted(RpgWorldManager.RegionType region) {
        if (region == null) {
            return;
        }
        ChainState state = stateFor(region);
        if (!state.rescueCompleted) {
            state.rescueCompleted = true;
            MissionBanner.show("RESGATE CONCLUÍDO", "Um informante regional foi localizado: "
                    + RegionalNpcs.getNameForRegion(region) + ".", new Color(129, 199, 132), Color.WHITE, 180);
        }
    }

    /** Callback de sucesso da missão secundária do NPC regional. */
    public static void onNpcQuestCompleted(String questId) {
        RpgWorldManager.RegionType region = RegionalNpcs.getRegionForQuestId(questId);
        if (region == null) {
            return;
        }
        ChainState state = stateFor(region);
        if (!state.npcCompleted) {
            state.npcCompleted = true;
            MissionBanner.show("NPC REGIONAL CONCLUÍDO", "A missão abriu a rota do comboio de suprimentos.",
                    new Color(129, 199, 132), Color.WHITE, 180);
        }
    }

    /** Callback de sucesso do evento SUPPLY_CONVOY. */
    public static void onConvoyCompleted(RpgWorldManager.RegionType region) {
        if (region == null) {
            return;
        }
        ChainState state = stateFor(region);
        if (!state.convoyCompleted) {
            state.convoyCompleted = true;
            MissionBanner.show("COMBOIO CONCLUÍDO", "A rota segura revelou a entrada da dungeon regional.",
                    new Color(129, 199, 132), Color.WHITE, 180);
        }
    }

    /** Callback de sucesso do chefe da dungeon regional. */
    public static void onDungeonCompleted(RpgWorldManager.RegionType region) {
        if (region == null) {
            return;
        }
        ChainState state = stateFor(region);
        if (!state.dungeonCompleted) {
            state.dungeonCompleted = true;
            MissionBanner.show("CADEIA REGIONAL COMPLETA", "Resgate, NPC, comboio e dungeon concluídos.",
                    new Color(255, 214, 10), Color.WHITE, 210);
        }
    }

    public static Map<String, Object> serialize() {
        Map<String, Object> snapshot = new HashMap<String, Object>();
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            ChainState state = stateFor(region);
            Map<String, Boolean> row = new HashMap<String, Boolean>();
            row.put("rescueCompleted", state.rescueCompleted);
            row.put("npcCompleted", state.npcCompleted);
            row.put("convoyCompleted", state.convoyCompleted);
            row.put("dungeonCompleted", state.dungeonCompleted);
            snapshot.put(region.name(), row);
        }
        return snapshot;
    }

    public static void deserialize(Object raw) {
        reset();
        if (!(raw instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> snapshot = (Map<?, ?>) raw;
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            Object rawRow = snapshot.get(region.name());
            if (!(rawRow instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> row = (Map<?, ?>) rawRow;
            ChainState state = stateFor(region);
            state.rescueCompleted = Boolean.TRUE.equals(row.get("rescueCompleted"));
            state.npcCompleted = Boolean.TRUE.equals(row.get("npcCompleted"));
            state.convoyCompleted = Boolean.TRUE.equals(row.get("convoyCompleted"));
            state.dungeonCompleted = Boolean.TRUE.equals(row.get("dungeonCompleted"));
        }
    }

    private static ChainState stateFor(RpgWorldManager.RegionType region) {
        ChainState state = states.get(region);
        if (state == null) {
            state = new ChainState();
            states.put(region, state);
        }
        return state;
    }
}
