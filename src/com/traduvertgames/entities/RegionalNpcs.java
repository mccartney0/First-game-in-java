package com.traduvertgames.entities;

import java.awt.Color;

import com.traduvertgames.dialogue.BranchingNpc;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.quest.SideQuestManager.Reward;
import com.traduvertgames.quest.SideQuestManager.SideQuest;
import com.traduvertgames.world.RpgWorldManager;

/** NPCs e missões secundárias exclusivas das regiões do mundo RPG. */
public final class RegionalNpcs {

    private RegionalNpcs() {
    }

    private static final class QuestSpec {
        final String questId;
        final String name;
        final String title;
        final String description;
        final String offerText;
        final String rewardText;
        final SideQuestManager.Type type;
        final InventoryManager.ItemType itemType;
        final int target;
        final Reward reward;
        final Color bodyColor;

        QuestSpec(String questId, String name, String title, String description, String offerText,
                String rewardText, SideQuestManager.Type type, InventoryManager.ItemType itemType,
                int target, Reward reward, Color bodyColor) {
            this.questId = questId;
            this.name = name;
            this.title = title;
            this.description = description;
            this.offerText = offerText;
            this.rewardText = rewardText;
            this.type = type;
            this.itemType = itemType;
            this.target = target;
            this.reward = reward;
            this.bodyColor = bodyColor;
        }
    }

    /** Registra todas as definições antes de restaurar um save sem carregar a superfície. */
    public static void registerDefinitions() {
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            QuestSpec spec = specFor(region);
            SideQuestManager.register(new SideQuest(spec.questId, spec.type, spec.itemType, spec.target,
                    spec.reward, spec.title, spec.description));
        }
    }

    /** Nome persistente do NPC associado a uma região. */
    public static String getNameForRegion(RpgWorldManager.RegionType region) {
        return specFor(region).name;
    }

    /** Cria o NPC responsável pela missão secundária da região. */
    public static InteractiveNpc create(RpgWorldManager.RegionType region, int x, int y) {
        QuestSpec spec = specFor(region);
        return new RegionalQuestNpc(x, y, spec);
    }

    /** Ativa pelo hub a mesma missão que o NPC regional ofereceria. */
    public static void activateQuestForRegion(RpgWorldManager.RegionType region) {
        QuestSpec spec = specFor(region);
        SideQuestManager.register(new SideQuest(spec.questId, spec.type, spec.itemType, spec.target,
                spec.reward, spec.title, spec.description));
        SideQuestManager.activateIfNeeded(spec.questId);
    }

    /** Título legível da missão secundária disponível na região. */
    public static String getQuestTitleForRegion(RpgWorldManager.RegionType region) {
        return specFor(region).title;
    }

    /** Identificador persistente usado pelo save e pelos testes de regressão. */
    public static String getQuestIdForRegion(RpgWorldManager.RegionType region) {
        return specFor(region).questId;
    }

    /** Localiza a região de uma missão para callbacks de progressão. */
    public static RpgWorldManager.RegionType getRegionForQuestId(String questId) {
        if (questId == null) {
            return null;
        }
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            if (specFor(region).questId.equals(questId)) {
                return region;
            }
        }
        return null;
    }

    private static QuestSpec specFor(RpgWorldManager.RegionType region) {
        switch (region) {
        case REFUGE:
            return new QuestSpec(
                    "region_refuge_supply", "Mara, a Intendente", "Suprimentos para o Refúgio",
                    "Reúna kits médicos para manter o abrigo funcionando durante as incursões.",
                    "As reservas do Refúgio estão no fim. Você pode trazer kits médicos para os sobreviventes?",
                    "A recompensa é recuperação de vida, escudo e créditos.",
                    SideQuestManager.Type.COLLECT_N, InventoryManager.ItemType.MEDKIT, 3,
                    new Reward(45, 25, 0, 180), new Color(46, 125, 50));
        case RUINS:
            return new QuestSpec(
                    "region_ruins_scrap", "Davi, o Sucateiro", "Limpeza das Ruínas",
                    "Elimine as patrulhas que ocuparam o depósito industrial e recupere o setor.",
                    "As máquinas transformaram meu depósito em uma toca. Derrube oito delas e eu libero o estoque.",
                    "Você receberá vida, energia de arma e créditos pelo serviço.",
                    SideQuestManager.Type.KILL_N, null, 8,
                    new Reward(25, 15, 30, 220), new Color(121, 85, 72));
        case MARSH:
            return new QuestSpec(
                    "region_marsh_medicine", "Iara, a Boticária", "Remédios no Lodo",
                    "Colete nanomedkits perdidos no pântano para tratar os feridos da travessia.",
                    "O lodo engoliu nossos remédios. Traga dois nanomedkits antes que a febre avance.",
                    "A recompensa inclui cura, escudo e uma reserva de mana.",
                    SideQuestManager.Type.COLLECT_N, InventoryManager.ItemType.NANOMEDKIT, 2,
                    new Reward(70, 30, 35, 240), new Color(93, 64, 55));
        case TUNDRA:
            return new QuestSpec(
                    "region_tundra_beacon", "Kellan, o Vigia", "Sinal na Tundra",
                    "Reduza as elites que cercam o beacon de contenção para reativar o sinal.",
                    "O frio não é o pior inimigo daqui. Elimine doze guardas e o beacon poderá voltar a transmitir.",
                    "A recompensa é um reforço de escudo e créditos de contenção.",
                    SideQuestManager.Type.KILL_N, null, 12,
                    new Reward(30, 60, 0, 300), new Color(55, 71, 79));
        case SANCTUARY:
            return new QuestSpec(
                    "region_sanctuary_data", "Nó-7, o Arquivista", "Memória do Santuário",
                    "Recupere núcleos de dados para reconstruir a memória da inteligência aliada.",
                    "O Santuário ainda guarda fragmentos da nossa história. Traga dois núcleos de dados intactos.",
                    "O arquivo concede mana, energia e créditos de pesquisa.",
                    SideQuestManager.Type.COLLECT_N, InventoryManager.ItemType.DATA_CORE, 2,
                    new Reward(20, 20, 100, 360), new Color(81, 45, 168));
        case CORE:
        default:
            return new QuestSpec(
                    "region_core_purge", "A Sentinela-9", "Câmara do Núcleo",
                    "Destrua as unidades que protegem o Núcleo do Supervisor antes da incursão final.",
                    "A arena está selada por quinze guardas. Abra caminho e o núcleo ficará vulnerável.",
                    "A recompensa é uma grande recuperação e créditos de alto risco.",
                    SideQuestManager.Type.KILL_N, null, 15,
                    new Reward(55, 55, 40, 500), new Color(173, 20, 87));
        }
    }

    private static QuestSpec specForName(String name) {
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            QuestSpec candidate = specFor(region);
            if (candidate.name.equals(name)) {
                return candidate;
            }
        }
        return specFor(RpgWorldManager.RegionType.CORE);
    }

    private static final class RegionalQuestNpc extends BranchingNpc {
        private final QuestSpec spec;

        RegionalQuestNpc(int x, int y, QuestSpec spec) {
            super(x, y, spec.name, spec.bodyColor, new Color(255, 224, 178));
            this.spec = spec;
            setRepeatableDialogue(true);
        }

        @Override
        protected DialogueNode[] buildNodes() {
            QuestSpec initialSpec = spec != null ? spec : specForName(getName());
            return new DialogueNode[] {
                    new DialogueNode(initialSpec.offerText,
                            new String[] { "Aceitar missão", "Ainda não", "O que ganho?" },
                            new int[] { 1, 3, 2 },
                            new Runnable[] { this::activateQuest, null, null }),
                    new DialogueNode(
                            "Missão registrada. Volte quando o trabalho estiver concluído e eu confirmarei o resultado.",
                            new String[] { null, null, null }, new int[] { -1, -1, -1 },
                            new Runnable[] { null, null, null }),
                    new DialogueNode(initialSpec.rewardText,
                            new String[] { "Aceitar missão", "Depois", null }, new int[] { 1, 3, -1 },
                            new Runnable[] { this::activateQuest, null, null }),
                    new DialogueNode("Entendo. Estarei aqui enquanto a região ainda precisar de ajuda.",
                            new String[] { null, null, null }, new int[] { -1, -1, -1 },
                            new Runnable[] { null, null, null })
            };
        }

        private void activateQuest() {
            SideQuestManager.register(new SideQuest(spec.questId, spec.type, spec.itemType, spec.target,
                    spec.reward, spec.title, spec.description));
            SideQuestManager.activateIfNeeded(spec.questId);
        }

        @Override
        public void startInteraction() {
            resetBranch();
            super.startInteraction();
        }

        @Override
        protected String getNodeText() {
            DialogueNode node = getNode();
            if (node != null && spec.offerText.equals(node.text)) {
                if (SideQuestManager.isCompleted(spec.questId)) {
                    return "Você voltou depois de cumprir '" + spec.title
                            + "'. A região não esquecerá o que fez por nós.";
                }
                if (SideQuestManager.isActive(spec.questId)) {
                    return "Progresso de '" + spec.title + "': "
                            + SideQuestManager.getProgressLabel(spec.questId) + ". " + spec.description;
                }
            }
            return super.getNodeText();
        }
    }
}
