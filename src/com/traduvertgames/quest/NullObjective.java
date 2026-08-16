package com.traduvertgames.quest;

/**
 * Missão do modo sobrevivência (pós-campanha): não tem conclusão — o jogador
 * apenas resiste às ondas infinitas e acumula pontuação.
 */
public final class NullObjective extends BaseObjective {
    public NullObjective() {
        super("Modo Sobrevivência", "Resista às ondas infinitas — sobreviva o máximo que puder.");
    }

    @Override
    public String getProgressText() {
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
