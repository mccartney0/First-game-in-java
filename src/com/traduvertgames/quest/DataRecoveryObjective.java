package com.traduvertgames.quest;

import java.util.HashSet;
import java.util.Set;

import com.traduvertgames.entities.DataCore;
import com.traduvertgames.entities.EngineerNPC;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.entities.QuestNPC;
import com.traduvertgames.entities.ResearcherNPC;

public final class DataRecoveryObjective extends BaseObjective {

    private final Set<DataCore> activeCores = new HashSet<DataCore>();
    private int collected = 0;
    private boolean researcherSafe = false;
    private boolean engineerAssisted = false;

    public DataRecoveryObjective() {
        super("Protocolos perdidos", "Recupere os núcleos de dados e evacue a equipe de pesquisa.");
    }

    @Override
    public void onLevelStart() {
        activeCores.clear();
        collected = 0;
        researcherSafe = false;
        engineerAssisted = false;
    }

    @Override
    public void onQuestItemSpawned(QuestItem item) {
        if (item instanceof DataCore) {
            activeCores.add((DataCore) item);
        }
    }

    @Override
    public void onQuestItemCollected(QuestItem item) {
        if (item instanceof DataCore && activeCores.remove(item)) {
            collected++;
        }
    }

    @Override
    public void onNpcRescued(QuestNPC npc) {
        if (npc instanceof ResearcherNPC) {
            researcherSafe = true;
        } else if (npc instanceof EngineerNPC) {
            engineerAssisted = true;
        }
    }

    @Override
    public String getProgressText() {
        int total = collected + activeCores.size();
        String coreStatus = total == 0 ? "Nenhum núcleo detectado"
                : String.format("Núcleos recuperados: %d / %d", collected, total);
        String researcherStatus = researcherSafe ? "Pesquisadora em segurança" : "Pesquisadora aguardando resgate";
        String engineerStatus = engineerAssisted ? "Engenheiro reativou suporte" : "Localize o engenheiro para reforços";
        return coreStatus + "\n" + researcherStatus + "\n" + engineerStatus;
    }

    @Override
    public boolean isComplete() {
        int total = collected + activeCores.size();
        return total > 0 && activeCores.isEmpty() && researcherSafe;
    }
}
