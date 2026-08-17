package com.traduvertgames.dialogue;

import java.awt.Color;

/**
 * NPC com diálogo ramificado (rodada 22). Diferente do InteractiveNpc
 * clássico (falas lineares), este define uma árvore de nós: cada nó tem um
 * texto e até três escolhas numeradas que levam a outros nós. Ao chegar em
 * um nó sem escolhas (terminal), a conversa é concluída normalmente.
 *
 * Ações podem ser executadas ao selecionar uma escolha (recompensas,
 * início de missões secundárias, desconto na loja etc.).
 *
 * Uso: estender e sobrescrever buildNodes(); registrar no mapa via
 * StoryManager/placeStoryNpcs como InteractiveNpc comum.
 */
public abstract class BranchingNpc extends InteractiveNpc {

	/** Nó do diálogo ramificado. */
	public static final class DialogueNode {
		public final String text;
		/** Texto das escolhas (até 3; null = nó terminal). */
		public final String[] choiceTexts;
		/** Nós-alvo de cada escolha (mesmo índice de choiceTexts). */
		public final int[] choiceTargets;
		/** Ações executadas ao selecionar cada escolha (pode ser null). */
		public final Runnable[] choiceActions;

		public DialogueNode(String text, String[] choiceTexts, int[] choiceTargets,
				Runnable[] choiceActions) {
			this.text = text;
			this.choiceTexts = choiceTexts;
			this.choiceTargets = choiceTargets;
			this.choiceActions = choiceActions;
		}

		/** Nó simples: texto seguido diretamente para o nó-alvo (sem escolha). */
		public static DialogueNode chain(String text, int target) {
			return new DialogueNode(text, new String[] { "" }, new int[] { target }, new Runnable[] { null });
		}
	}

	private DialogueNode[] nodes = new DialogueNode[0];
	private int currentNode = 0;

	public BranchingNpc(int x, int y, String name, Color bodyColor, Color headColor) {
		super(x, y, name, bodyColor, headColor, new String[0], new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
				// Diálogo ramificado: hooks por nó (selectChoice), nada aqui.
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
				// Diálogo ramificado concluído: persistido pelo flag padrão.
			}
		});
		// As falas agora vêm da árvore de nós.
		nodes = buildNodes();
	}

	/** Constrói a árvore de nós do diálogo. Sobrescrever nas subclasses. */
	protected abstract DialogueNode[] buildNodes();

	/** Falas lineares (herdadas do InteractiveNpc): nunca usadas — o
	 * DialogueManager detecta BranchingNpc e delega à árvore de nós. */
	public String[] getDialogueLines() {
		// O DialogueManager detecta BranchingNpc e delega; nunca chega aqui.
		return new String[0];
	}

	/** Texto do nó atual do diálogo (para o DialogueManager). */
	protected String getNodeText() {
		DialogueNode node = getNode();
		return node != null ? node.text : "";
	}

	/** Nó atual do diálogo (para o DialogueManager). */
	public DialogueNode getNode() {
		if (currentNode < 0 || currentNode >= nodes.length) {
			return null;
		}
		return nodes[currentNode];
	}

	public void setCurrentNode(int nodeIndex) {
		this.currentNode = nodeIndex;
	}

	/** Reseta para o nó inicial (conversas repetidas com NPCs dinâmicos). */
	public void resetBranch() {
		this.currentNode = 0;
	}

	/**
	 * Executa a ação da escolha selecionada (0..2) no nó atual, quando a
	 * escolha existe.
	 */
	public void selectChoice(int choiceIndex) {
		DialogueNode node = getNode();
		if (node == null || node.choiceActions == null || choiceIndex < 0
				|| choiceIndex >= node.choiceActions.length) {
			return;
		}
		Runnable action = node.choiceActions[choiceIndex];
		if (action != null) {
			action.run();
		}
		// Avança para o nó-alvo da escolha (ou terminal).
		int target = node.choiceTargets != null && choiceIndex < node.choiceTargets.length
				? node.choiceTargets[choiceIndex] : -1;
		if (target >= 0 && target < nodes.length) {
			currentNode = target;
		} else {
			currentNode = -1;
		}
	}

	/** Indica se o nó atual é terminal (próximo Enter conclui a conversa). */
	public boolean isTerminal() {
		return currentNode < 0 || getNode() == null;
	}

	/** Indica se o nó atual oferece escolhas visíveis. */
	public boolean hasChoices() {
		DialogueNode node = getNode();
		if (node == null || node.choiceTexts == null) {
			return false;
		}
		for (String t : node.choiceTexts) {
			if (t != null && !t.isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
