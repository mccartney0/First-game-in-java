package com.traduvertgames.quest;

import java.util.HashSet;
import java.util.Set;

import com.traduvertgames.entities.QuestItem;

public final class CollectArtifactsObjective extends BaseObjective {
    private final Set<QuestItem> trackedItems = new HashSet<QuestItem>();
    private int collected = 0;

    public CollectArtifactsObjective() {
        super("Recuperar relíquias", "Reúna todos os núcleos de mana perdidos na base.");
    }

    @Override
    public void onLevelStart() {
        trackedItems.clear();
        collected = 0;
    }

    @Override
    public void onQuestItemSpawned(QuestItem item) {
        trackedItems.add(item);
    }

    @Override
    public void onQuestItemCollected(QuestItem item) {
        if (trackedItems.remove(item)) {
            collected++;
        }
    }

    @Override
    public String getProgressText() {
        int required = trackedItems.size() + collected;
        if (required == 0) {
            return "Nenhum item detectado";
        }
        return String.format("%d de %d recuperados", collected, required);
    }

    @Override
    public boolean isComplete() {
        return trackedItems.isEmpty() && collected > 0;
    }
}
