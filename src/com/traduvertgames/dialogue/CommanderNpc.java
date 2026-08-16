package com.traduvertgames.dialogue;

import java.awt.Color;

import com.traduvertgames.main.Game;

/**
 * Comandante Ava — comandante da operação. Conversar com ela revela o plano
 * da missão e, ao concluir, desbloqueia o objetivo completo da fase.
 */
public class CommanderNpc extends InteractiveNpc {

	private final String missionLine;

	public CommanderNpc(int x, int y) {
		this(x, y, "A missão exige foco, piloto. Execute o plano com calma.", new String[] {
				"O sistema da colônia foi infectado. As máquinas se voltaram contra nós.",
				"Aqui está o plano: avance pelo setor e garanta o objetivo principal. Não se apresse.",
				"A energia da sua arma está limitada. Recolha células e recarregue quando puder.",
				"Boa sorte, piloto. A colônia conta com você."
		});
	}

	public CommanderNpc(int x, int y, String missionLine, String[] lines) {
		super(x, y, "Comandante Ava", new Color(0, 137, 124), new Color(255, 224, 178), lines,
				new InteractionListener() {
					@Override
					public void onInteractionStart(InteractiveNpc npc) {
						// Sinal sonoro discreto de início de conversa.
					}

					@Override
					public void onInteractionEnd(InteractiveNpc npc) {
						// A missão recebe a conclusão via QuestManager.notifyDialogueFinished.
						// A recompensa fica a cargo da fase (bônus de vida/escudo).
						if (Game.player != null) {
							Game.player.addMana(80);
						}
					}
				});
		this.missionLine = missionLine;
	}

	/** Linha extra sobre o objetivo da fase (adicionada no início das falas). */
	public String getMissionLine() {
		return missionLine;
	}
}
