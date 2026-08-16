package com.traduvertgames.quest;

import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.entities.QuestNPC;
import com.traduvertgames.main.Game;

public final class QuestManager {
    private static final RPGObjective NULL_OBJECTIVE = new NullObjective();
    private static RPGObjective currentObjective = NULL_OBJECTIVE;
    private static int currentLevel = 1;
    private static final List<Entity> pendingRemoval = new ArrayList<Entity>();

    private QuestManager() {
    }

    /** Título narrativo curto de cada fase da campanha. */
    private static final String[] PHASE_TITLES = {
            "", // índice 0 não usado
            "Setor Alpha",
            "Câmara do Warbringer",
            "Círculo do Ritual",
            "Núcleo da Colônia",
            "Datacenter Nexus",
            "Torre do Supervisor",
            "Subsolo da Colônia",
            "Núcleo Central",
            "Modo Sobrevivência"
    };

    /** Abertura da campanha, exibida no banner inicial da fase 1. */
    public static final String CAMPAIGN_OPENING =
            "O sistema da colônia foi infectado.\n" +
            "As máquinas se voltaram contra nós.\n" +
            "Você é a última linha de defesa.";

    /** Texto exibido ao concluir a campanha e entrar no modo sobrevivência. */
    public static final String SURVIVAL_INTRO =
            "Campanha concluída!\nA mente da colônia foi destruída, mas restos das máquinas\n" +
            "continuam se reagrupando. Resista às ondas infinitas.";

    public static String getPhaseTitle(int level) {
        if (level <= 0) {
            return "Arena de Treino";
        }
        if (level >= PHASE_TITLES.length) {
            return "Fase " + level;
        }
        return PHASE_TITLES[level];
    }

    public static boolean isSurvivalMode() {
        return currentLevel >= PHASE_TITLES.length - 1;
    }

    public static int getCurrentLevel() {
        return currentLevel;
    }

    public static void prepareForLevel(int level) {
        currentLevel = level;
        currentObjective = createObjectiveForLevel(level);
        currentObjective.onLevelStart();
        pendingRemoval.clear();
    }

    public static void onLevelLoaded() {
        currentObjective.onLevelLoaded();
    }

    private static RPGObjective createObjectiveForLevel(int level) {
        switch (level) {
        case 1:
            // Fase 1: conversar com a Comandante Ava e coletar os artefatos.
            return new ContactObjective();
        case 2:
            // Fase 2: falar com a Engenheira Nia antes de caçar o Warbringer.
            return new DialogueObjective(new BossHuntObjective(), "Engenheira Nia");
        case 3:
            // Fase 3: pesquisar com Ivo antes de ativar o ritual.
            return new DialogueObjective(new RitualObjective(), "Pesquisador Ivo");
        case 4:
            // Fase 4: missão de resgate com apoio do Armeiro.
            return new DialogueObjective(new RescueObjective(), "Armeiro Mercúrio");
        case 5:
            // Fase 5: recuperar os dados com ajuda do Ivo.
            return new DialogueObjective(new DataRecoveryObjective(), "Pesquisador Ivo");
        case 6:
            // Fase 6: falar com Ava antes de derrubar o OVERSEER, o chefe supervisor.
            return new DialogueObjective(
                    new BossHuntObjective("Derrubar o Supervisor", "Localize e destrua o Supervisor, o cérebro da operação.", "o Supervisor"),
                    "Comandante Ava");
        case 7:
            // Fase 7: falar com Ava, ouvir o desertor do subsolo, sabotar os
            // geradores e então destruir o Guardião do Subsolo.
            return new DialogueObjective(new SabotageObjective(), "Comandante Ava");
        case 8:
            // Fase final da campanha: o briefing final da Ava e a destruição
            // do OVERSEER PRIME, a mente que comanda todas as máquinas.
            return new InfiltratorObjective();
        case 9:
            // Modo sobrevivência pós-campanha: ondas infinitas.
            return new NullObjective();
        default:
            return NULL_OBJECTIVE;
        }
    }

    public static void registerQuestItem(QuestItem item) {
        currentObjective.onQuestItemSpawned(item);
    }

    public static void collectQuestItem(QuestItem item) {
        currentObjective.onQuestItemCollected(item);
        scheduleRemoval(item);
    }

    public static void registerBeacon(QuestBeacon beacon) {
        currentObjective.onBeaconSpawned(beacon);
    }

    public static void activateBeacon(QuestBeacon beacon) {
        currentObjective.onBeaconActivated(beacon);
    }

    public static void registerNpc(QuestNPC npc) {
        currentObjective.onNpcSpawned(npc);
    }

    public static void rescueNpc(QuestNPC npc) {
        currentObjective.onNpcRescued(npc);
        scheduleRemoval(npc);
    }

    public static void notifyEnemyKilled(Enemy enemy) {
        currentObjective.onEnemyKilled(enemy);
    }

    /** Notifica a missão ativa que o jogador iniciou uma conversa. */
    public static void notifyDialogueStarted(InteractiveNpc npc) {
        currentObjective.onDialogueStarted(npc);
    }

    /** Notifica a missão ativa que o jogador concluiu a conversa. */
    public static void notifyDialogueFinished(InteractiveNpc npc) {
        currentObjective.onDialogueFinished(npc);
    }

    public static void notifyBossSpotted() {
        if (currentObjective instanceof BossHuntObjective) {
            ((BossHuntObjective) currentObjective).registerBossPresence();
        }
    }

    public static String getObjectiveTitle() {
        return currentObjective.getTitle();
    }

    public static String getObjectiveDescription() {
        return currentObjective.getDescription();
    }

    public static String getObjectiveProgress() {
        return currentObjective.getProgressText();
    }

    /** Título do personagem/alvo que o objetivo pede para localizar (usado no waypoint). */
    public static String getTargetHint() {
        return currentObjective.getTargetHint();
    }

    public static boolean isObjectiveComplete() {
        return currentObjective.isComplete();
    }

    /**
     * Estado lógico da missão da fase atual, para o save.
     * O formato é opaco e definido por cada objetivo ({@link RPGObjective#serializeState()}).
     */
    public static String serializeObjectiveState() {
        return currentObjective.serializeState();
    }

    /**
     * Aplica o estado salvo à missão da fase atual.
     * Deve ser chamado depois de {@link #prepareForLevel(int)} para a fase salva.
     */
    public static void deserializeObjectiveState(String state) {
        currentObjective.deserializeState(state);
    }

    public static void update() {
        currentObjective.update();
        processPendingRemovals();
    }

    private static void scheduleRemoval(Entity entity) {
        if (entity != null && !pendingRemoval.contains(entity)) {
            pendingRemoval.add(entity);
        }
    }

    private static void processPendingRemovals() {
        if (pendingRemoval.isEmpty()) {
            return;
        }
        Game.entities.removeAll(pendingRemoval);
        pendingRemoval.clear();
    }
}
