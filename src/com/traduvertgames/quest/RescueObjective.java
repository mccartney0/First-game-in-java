package com.traduvertgames.quest;

import java.util.HashSet;
import java.util.Set;

import com.traduvertgames.entities.QuestNPC;

public final class RescueObjective extends BaseObjective {
    private final Set<QuestNPC> survivors = new HashSet<QuestNPC>();
    private int rescued = 0;

    public RescueObjective() {
        super("Evacuar sobreviventes", "Localize os pesquisadores e conduza-os para um ponto seguro.");
    }

    @Override
    public void onLevelStart() {
        survivors.clear();
        rescued = 0;
    }

    @Override
    public void onNpcSpawned(QuestNPC npc) {
        survivors.add(npc);
    }

    @Override
    public void onNpcRescued(QuestNPC npc) {
        if (survivors.remove(npc)) {
            rescued++;
        }
    }

    @Override
    public String getProgressText() {
        int total = rescued + survivors.size();
        if (total == 0) {
            return "Nenhum sinal de vida";
        }
        return String.format("%d de %d evacuados", rescued, total);
    }

    @Override
    public boolean isComplete() {
        return survivors.isEmpty() && rescued > 0;
    }
}
