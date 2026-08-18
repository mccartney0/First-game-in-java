package com.traduvertgames.dialogue;

import java.awt.Color;

import com.traduvertgames.main.Game;
import com.traduvertgames.state.PilotUpgrades;

/**
 * Comandante Ava — comandante da operação. Conversar com ela revela o plano
 * da missão e, ao concluir, desbloqueia o objetivo completo da fase.
 *
 * Rodada 31: na fase 9 (fim da campanha), a Ava aparece como variante de
 * despedida — agradece o resgate dos refugiados e concede a bênção final
 * (50 créditos da conta, vida e mana no máximo).
 */
public class CommanderNpc extends InteractiveNpc {

	/** Cores oficiais da Comandante Ava (teal da paleta do jogo). */
	public static final Color BODY_COLOR = new Color(0, 137, 124);
	public static final Color HEAD_COLOR = new Color(255, 224, 178);

	private final String missionLine;

	/** Falas da despedida ao fim da campanha (fase 9). */
	private static final String[] FAREWELL_LINES = {
			"Você fez mais do que a missão exigia, piloto.",
			"Os refugiados estão a salvo. O vale tem futuro novamente.",
			"Cada sobrevivente que você trouxe daqui escreve um capítulo da nossa reconstrução.",
			"Obrigada por lutar por todos nós — a colônia nunca vai esquecer."
	};

	public CommanderNpc(int x, int y) {
		this(x, y, "A missão exige foco, piloto. Execute o plano com calma.", new String[] {
				"O sistema da colônia foi infectado. As máquinas se voltaram contra nós.",
				"Aqui está o plano: avance pelo setor e garanta o objetivo principal. Não se apresse.",
				"A energia da sua arma está limitada. Recolha células e recarregue quando puder.",
				"Boa sorte, piloto. A colônia conta com você."
		});
	}

	public CommanderNpc(int x, int y, String missionLine, String[] lines) {
		this(x, y, missionLine, lines, false);
	}

	/**
	 * Construtor principal com a flag de despedida (fase 9 — fim da campanha).
	 */
	public CommanderNpc(int x, int y, String missionLine, String[] lines, boolean farewell) {
		super(x, y, "Comandante Ava", BODY_COLOR, HEAD_COLOR, lines, farewell
				? new FarewellListener()
				: new DefaultListener());
		this.missionLine = missionLine;
		this.isFarewell = farewell;
	}

	/** Conveniência: cria a variante de despedida da fase 9. */
	public static CommanderNpc farewell(int x, int y) {
		return new CommanderNpc(x, y, "Você fez mais do que a missão exigia, piloto.",
				FAREWELL_LINES, true);
	}

	private boolean isFarewell = false;

	/** @return true se este é o diálogo de despedida do fim da campanha. */
	public boolean isFarewell() {
		return isFarewell;
	}

	/** Linha extra sobre o objetivo da fase (adicionada no início das falas). */
	public String getMissionLine() {
		return missionLine;
	}

	/** Recompensa padrão ao concluir a conversa: +80 de mana. */
	private static final class DefaultListener implements InteractionListener {
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
	}

	/**
	 * Bênção final da campanha (variante de despedida): 50 créditos na conta
	 * do piloto (metagame, persistidos) e vida/mana restauradas ao máximo.
	 */
	private static final class FarewellListener implements InteractionListener {
		@Override
		public void onInteractionStart(InteractiveNpc npc) {
		}

		@Override
		public void onInteractionEnd(InteractiveNpc npc) {
			PilotUpgrades.addCredits(50);
			if (Game.player != null) {
				Game.player.setLife(Game.player.getMaxLife());
				Game.player.setMana(Game.player.getMaxMana());
			}
			com.traduvertgames.main.FloatingText.show("RECURSITOS DA COLÔNIA: +50 CRÉDITOS",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 40,
					new Color(255, 214, 10), 240);
		}
	}
}
