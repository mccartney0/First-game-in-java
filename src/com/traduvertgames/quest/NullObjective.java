package com.traduvertgames.quest;

public final class NullObjective extends BaseObjective {
    public NullObjective() {
        super("Sem missão", "Explore o setor livremente.");
    }

    @Override
    public String getProgressText() {
        return "Aguardando ordens";
    }

    @Override
    public boolean isComplete() {
        return false;
    }
}
