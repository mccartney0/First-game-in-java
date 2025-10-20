package com.traduvertgames.quest;

import java.util.HashSet;
import java.util.Set;

import com.traduvertgames.entities.QuestBeacon;

public final class RitualObjective extends BaseObjective {
    private final Set<QuestBeacon> beacons = new HashSet<QuestBeacon>();
    private int activated = 0;

    public RitualObjective() {
        super("Reativar obeliscos", "Canalize energia em cada farol arcano para purificar o setor.");
    }

    @Override
    public void onLevelStart() {
        beacons.clear();
        activated = 0;
    }

    @Override
    public void onBeaconSpawned(QuestBeacon beacon) {
        beacons.add(beacon);
    }

    @Override
    public void onBeaconActivated(QuestBeacon beacon) {
        if (beacons.remove(beacon)) {
            activated++;
        }
    }

    @Override
    public String getProgressText() {
        int total = activated + beacons.size();
        if (total == 0) {
            return "Obeliscos indisponíveis";
        }
        return String.format("%d de %d ativados", activated, total);
    }

    @Override
    public boolean isComplete() {
        return beacons.isEmpty() && activated > 0;
    }
}
