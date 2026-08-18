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
import com.traduvertgames.world.World;

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
            "Vale dos Refugiados",
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
            // Fases procedurais do modo infinito: título com a profundidade do ciclo atual.
            int depth = Game.getStaticLevelPlus();
            if (depth < 1) {
                depth = 1;
            }
            return "Fase Procedural " + depth;
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

    /**
     * Variante pública do {@link #createObjectiveForLevel(int)} para os
     * testes validarem a missão associada a cada fase sem depender do mapa.
     */
    public static RPGObjective objectiveForLevel(int level) {
        return createObjectiveForLevel(level);
    }

    private static RPGObjective createObjectiveForLevel(int level) {
        switch (level) {
        case 1:
            // Fase 1: conversar com a Comandante Ava e coletar os artefatos.
            return new ContactObjective();
        case 2:
            // Fase 2: falar com a Engenheira Nia, defender o ponto de
            // estabilização e então caçar o Warbringer.
            return new DialogueObjective(
                    new SequenceObjective(new HoldObjective(), new BossHuntObjective()),
                    "Engenheira Nia");
        case 3:
            // Fase 3: conversar com Ivo no laboratório, resistir à invasão das
            // máquinas que tentam completar o ritual e então desativar os três
            // obeliscos do Círculo do Ritual.
            return new DialogueObjective(
                    new SequenceObjective(
                            new SurviveObjective("O laboratório sob cerco", "O Ivo ativa o scanner de obeliscos, mas as máquinas detectam a operação. Resista até o perímetro ser seguro.", 30),
                            new RitualObjective()),
                    "Pesquisador Ivo");
        case 4:
            // Fase 4: com o apoio do Armeiro, evacuar os sobreviventes presos
            // no Núcleo da Colônia e garantir a extração.
            return new DialogueObjective(new RescueObjective(), "Armeiro Mercúrio");
        case 5:
            // Fase 5: recuperar os núcleos de dados do Datacenter Nexus com
            // ajuda do Ivo e então derrubar o Warbringer que guarda o servidor.
            return new DialogueObjective(
                    new SequenceObjective(new DataRecoveryObjective(),
                            new BossHuntObjective("Derrubar o Warbringer", "O Warbringer protege o servidor central. Neutralize a máquina para finalizar a recuperação dos dados.", "o Warbringer")),
                    "Pesquisador Ivo");
        case 6:
            // Fase 6: falar com Ava, resistir às ondas do perímetro e então
            // derrubar o OVERSEER, o chefe supervisor.
            return new DialogueObjective(
                    new SequenceObjective(
                            new SurviveObjective("Resistir no perímetro", "A torre do Supervisor só abre após o perímetro ser liberado. Resista por 35 segundos e mantenha-se em movimento.", 35),
                            new BossHuntObjective("Derrubar o Supervisor", "Localize e destrua o Supervisor, o cérebro da operação.", "o Supervisor")),
                    "Comandante Ava");
        case 7:
            // Fase 7: falar com Ava, ouvir o desertor do subsolo, sabotar os
            // geradores com a estabilização do beacon do setor e então destruir
            // o Guardião do Subsolo.
            return new DialogueObjective(
                    new SequenceObjective(new SabotageObjective(),
                            new HoldObjective("Isolar o núcleo do Guardião", "Ative o beacon e mantenha a área segura até o canal atingir 100%. Depois, avance para a câmara do chefe.")),
                    "Comandante Ava");
        case 8:
            // Fase final da campanha: o briefing da Ava, a escolta do
            // informante até o núcleo e a destruição do OVERSEER PRIME.
            return new SequenceObjective(new InfiltratorObjective(), new EscortObjective("Escoltar o informante", "Proteja o informante até o ponto de fuga. Elimine os inimigos próximos para ele continuar avançando."));
        case 9:
            // Fase 9 (Vale dos Refugiados): com o apoio do Curandeiro Léo,
            // resgatar a líder dos refugiados presos no acampamento do vale
            // e ativar o beacon de evacuação para concluir a campanha.
            return new DialogueObjective(new RescueObjective(), "Curandeiro Léo");
        case 10:
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
        // Rodada 22: missões secundárias acompanham os kills da fase.
        SideQuestManager.onEnemyKilled(enemy);
    }

    /** Registro de um NPC de escolta (usado pelo {@link EscortObjective}). */
    public static void registerEscort(com.traduvertgames.entities.EscortNpc npc) {
        if (currentObjective instanceof EscortObjective) {
            ((EscortObjective) currentObjective).onEscortSpawned(npc);
        } else if (currentObjective instanceof SequenceObjective) {
            ((SequenceObjective) currentObjective).onEscortEvent(stage -> stage.onEscortSpawned(npc));
        }
    }

    /** O escoltado foi atingido até a morte: a escolta falha e a fase recomeça. */
    public static void escortFailed(com.traduvertgames.entities.EscortNpc npc) {
        if (currentObjective instanceof EscortObjective) {
            ((EscortObjective) currentObjective).onEscortFailed(npc);
        } else if (currentObjective instanceof SequenceObjective) {
            ((SequenceObjective) currentObjective).onEscortEvent(stage -> stage.onEscortFailed(npc));
        }
    }

    /** O escoltado chegou ao ponto de fuga: a etapa de escolta conclui. */
    public static void escortArrived(com.traduvertgames.entities.EscortNpc npc) {
        if (currentObjective instanceof EscortObjective) {
            ((EscortObjective) currentObjective).onEscortArrived(npc);
        } else if (currentObjective instanceof SequenceObjective) {
            ((SequenceObjective) currentObjective).onEscortEvent(stage -> stage.onEscortArrived(npc));
        }
    }

    /** Objetivo em andamento (expõe o ativo de sequências para a HUD). */
    public static RPGObjective getCurrentObjective() {
        return currentObjective;
    }

    /** Notifica a missão ativa que o jogador iniciou uma conversa. */
    public static void notifyDialogueStarted(InteractiveNpc npc) {
        currentObjective.onDialogueStarted(npc);
    }

    /** Notifica a missão ativa que o jogador concluiu a conversa. */
    public static void notifyDialogueFinished(InteractiveNpc npc) {
        currentObjective.onDialogueFinished(npc);
    }

    /**
     * Um chefe de fase foi detectado no mapa: registra a presença nos
     * objetivos de caça, atravessando wrappers (Dialogue/Sequence).
     */
    public static void notifyBossSpotted() {
        currentObjective.onBossSpotted();
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

    /** Retorna true quando a etapa jogável atual é uma caça a chefe. */
    public static boolean isBossHuntActive() {
        RPGObjective objective = currentObjective;
        while (true) {
            if (objective instanceof DialogueObjective) {
                DialogueObjective dialogue = (DialogueObjective) objective;
                if (!dialogue.hasTalkedToTarget()) {
                    return false;
                }
                objective = dialogue.getDelegate();
            } else if (objective instanceof SequenceObjective) {
                objective = ((SequenceObjective) objective).getActive();
            } else {
                return objective instanceof BossHuntObjective;
            }
        }
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
        // A sequência pode ter acabado de liberar a etapa do chefe neste frame.
        World.ensureActivePhaseBoss();
        // Rodada 22: missões de coleta acompanham o inventário a cada frame.
        SideQuestManager.refreshCollectibles();
        processPendingRemovals();
    }

    /**
     * Reinicia a fase atual do jogador (usado quando a escolta falha ou outro
     * objetivo exige reintentar a fase). Recarrega o mapa preservando o
     * progresso da campanha e reinicia a missão da fase.
     */
    public static void restartCurrentLevel() {
        World.restartGame("level" + currentLevel + ".png");
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
