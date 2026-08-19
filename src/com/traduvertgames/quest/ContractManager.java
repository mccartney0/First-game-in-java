package com.traduvertgames.quest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.traduvertgames.entities.RegionalNpcs;
import com.traduvertgames.main.Game;
import com.traduvertgames.state.PilotUpgrades;
import com.traduvertgames.world.DungeonManager;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RegionalProgressionManager;
import com.traduvertgames.world.RpgWorldManager;

/** Contratos opcionais do hub, com bônus e objetivo explícito por região. */
public final class ContractManager {

    public enum Kind {
        EVENT,
        DUNGEON,
        NPC
    }

    public static final class Contract {
        private final String id;
        private final String title;
        private final String description;
        private final Kind kind;
        private final DynamicEventManager.Type eventType;
        private final String sideQuestId;
        private final int rewardBonus;
        private final int reputationBonus;

        private Contract(String id, String title, String description, Kind kind,
                DynamicEventManager.Type eventType, String sideQuestId,
                int rewardBonus, int reputationBonus) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.kind = kind;
            this.eventType = eventType;
            this.sideQuestId = sideQuestId;
            this.rewardBonus = rewardBonus;
            this.reputationBonus = reputationBonus;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Kind getKind() {
            return kind;
        }

        public int getRewardBonus() {
            return rewardBonus;
        }
    }

    private static final List<Contract> offered = new ArrayList<Contract>();
    private static final Map<String, Boolean> completed = new HashMap<String, Boolean>();
    private static RpgWorldManager.RegionType currentRegion;
    private static String activeContractId;
    private static int activeRewardBonus;

    private ContractManager() {
    }

    public static void reset() {
        offered.clear();
        completed.clear();
        currentRegion = null;
        activeContractId = null;
        activeRewardBonus = 0;
    }

    public static void open(RpgWorldManager.RegionType region) {
        currentRegion = region;
        offered.clear();
        if (region == null) {
            return;
        }
        int seed = region.ordinal() + RegionalProgressionManager.getReputation(region)
                + RegionalProgressionManager.getThreat(region);
        DynamicEventManager.Type event = DynamicEventManager.Type.values()[Math.floorMod(seed,
                DynamicEventManager.Type.values().length)];
        String prefix = region.name().toLowerCase();
        offered.add(new Contract("event_" + prefix + "_" + event.name().toLowerCase(),
                event.getTitle() + " prioritária",
                event.getDescription() + " Bônus de contrato: +40 créditos.", Kind.EVENT,
                event, null, 40, 5));
        if (!DungeonManager.isRegionCompleted(region)) {
            offered.add(new Contract("dungeon_" + prefix,
                    "Incursão: " + region.getDisplayName(),
                    "Derrote o chefe regional e reduza a ameaça. Bônus: +80 créditos.",
                    Kind.DUNGEON, null, null, 80, 10));
        } else {
            offered.add(new Contract("event_" + prefix + "_rescue",
                    "Resgate de veteranos",
                    "Complete um resgate regional para reforçar a reputação. Bônus: +55 créditos.",
                    Kind.EVENT, DynamicEventManager.Type.RESCUE, null, 55, 8));
        }
        String sideQuestId = RegionalNpcs.getQuestIdForRegion(region);
        if (sideQuestId != null && SideQuestManager.isRegistered(sideQuestId)
                && !SideQuestManager.isCompleted(sideQuestId)) {
            offered.add(new Contract("npc_" + prefix,
                    RegionalNpcs.getQuestTitleForRegion(region),
                    "Missão do NPC regional. Conclua o objetivo para receber bônus de contrato.",
                    Kind.NPC, null, sideQuestId, 60, 8));
        } else {
            offered.add(new Contract("event_" + prefix + "_convoy",
                    "Comboio da região",
                    "Escolte suprimentos e reduza a ameaça local. Bônus: +60 créditos.",
                    Kind.EVENT, DynamicEventManager.Type.SUPPLY_CONVOY, null, 60, 8));
        }
    }

    public static List<Contract> getOffered() {
        return new ArrayList<Contract>(offered);
    }

    public static String getActiveContractId() {
        return activeContractId;
    }

    public static int getActiveRewardBonus() {
        return activeRewardBonus;
    }

    public static boolean isCompleted(Contract contract) {
        return contract != null && Boolean.TRUE.equals(completed.get(contract.getId()));
    }

    public static boolean accept(int index) {
        if (index < 0 || index >= offered.size() || Game.player == null) {
            return false;
        }
        Contract contract = offered.get(index);
        if (isCompleted(contract)) {
            return false;
        }
        activeContractId = contract.id;
        activeRewardBonus = contract.rewardBonus;
        boolean started;
        switch (contract.kind) {
        case EVENT:
            started = DynamicEventManager.startEventForCurrentRegion(contract.eventType);
            break;
		case DUNGEON:
			started = DungeonManager.requestEnter(currentRegion);
			break;
        case NPC:
            RegionalNpcs.activateQuestForRegion(currentRegion);
            started = true;
            break;
        default:
            started = false;
            break;
        }
        if (!started) {
            clearActive();
        }
        return started;
    }

    public static void onEventCompleted(RpgWorldManager.RegionType region, DynamicEventManager.Type type) {
        Contract contract = activeContract(region, Kind.EVENT);
        if (contract == null || contract.eventType != type) {
            return;
        }
        complete(contract);
    }

    public static void onDungeonCompleted(RpgWorldManager.RegionType region) {
        Contract contract = activeContract(region, Kind.DUNGEON);
        if (contract != null) {
            complete(contract);
        }
    }

    public static void onSideQuestCompleted(String sideQuestId) {
        if (sideQuestId == null || activeContractId == null) {
            return;
        }
        Contract contract = findActive();
        if (contract != null && contract.kind == Kind.NPC && sideQuestId.equals(contract.sideQuestId)) {
            complete(contract);
        }
    }

    public static Map<String, Boolean> serializeCompleted() {
        return new HashMap<String, Boolean>(completed);
    }

    public static void deserializeCompleted(Map<String, Boolean> snapshot) {
        completed.clear();
        if (snapshot != null) {
            completed.putAll(snapshot);
        }
        activeContractId = null;
        activeRewardBonus = 0;
    }

    private static Contract activeContract(RpgWorldManager.RegionType region, Kind kind) {
        if (activeContractId == null || currentRegion != region) {
            return null;
        }
        Contract contract = findActive();
        return contract != null && contract.kind == kind ? contract : null;
    }

    private static Contract findActive() {
        for (Contract contract : offered) {
            if (contract.id.equals(activeContractId)) {
                return contract;
            }
        }
        return null;
    }

    private static void complete(Contract contract) {
        completed.put(contract.id, true);
        PilotUpgrades.addCredits(contract.rewardBonus);
        RegionalProgressionManager.addReputation(currentRegion, contract.reputationBonus);
        clearActive();
        com.traduvertgames.main.SaveManager.saveCurrentGame();
    }

    private static void clearActive() {
        activeContractId = null;
        activeRewardBonus = 0;
    }
}
