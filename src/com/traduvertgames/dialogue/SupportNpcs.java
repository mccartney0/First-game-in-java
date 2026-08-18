package com.traduvertgames.dialogue;

import java.awt.Color;

import com.traduvertgames.dialogue.InteractiveNpc.InteractionListener;
import com.traduvertgames.main.Game;

/**
 * NPCs de apoio:
 * - Engenheira Nia (verde-claro): recarrega energia da arma e dá mana ao conversar.
 * - Pesquisador Ivo (roxo): dá mana extra e informação técnica.
 * - Armeiro Mercúrio (laranja): recarrega arma completa e dá vida extra.
 * Todos são reutilizáveis por fase e funcionam por conversação (tecla R).
 */
public final class SupportNpcs {

	private SupportNpcs() {
	}

	/** Engenheira Nia — +150 energia de arma, +120 mana ao conversar. */
	public static InteractiveNpc engineer(int x, int y) {
		return new InteractiveNpc(x, y, "Engenheira Nia", new Color(76, 175, 80), new Color(255, 224, 178),
				new String[] {
						"Seus equipamentos estão desgastados. Deixe-me recarregar sua arma.",
						"Mantenha os módulos de escudo calibrados. E cuidado com os drones rebeldes.",
						"Pronto! Energia da arma restaurada."
				}, new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
				if (Game.player != null) {
					Game.player.addWeaponEnergy(150);
					Game.player.addMana(120);
				}
			}
		});
	}

	/** Pesquisador Ivo — +180 mana e dica técnica. */
	public static InteractiveNpc researcher(int x, int y) {
		return new InteractiveNpc(x, y, "Pesquisador Ivo", new Color(126, 87, 194), new Color(255, 224, 178),
				new String[] {
						"A infecção se espalhou pela rede da colônia. Cada fase tem um núcleo corrompido.",
						"Meus sensores indicam mais máquinas hostis adiante. Reforce seu escudo.",
						"Leve isto: uma injeção de mana de emergência."
				}, new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
				if (Game.player != null) {
					Game.player.addMana(180);
				}
			}
		});
	}

	/**
	 * Curandeiro Léo (verde-lima) — trata os ferimentos do piloto, restaurando
	 * 60% da vida máxima, e deixa um kit de campo (+20 de escudo). Aparece no
	 * mapa do Vale dos Refugiados (fase 9) e em campos de socorro espalhados.
	 */
	public static InteractiveNpc healer(int x, int y) {
		return new InteractiveNpc(x, y, "Curandeiro Léo", new Color(205, 220, 57), new Color(255, 224, 178),
				new String[] {
						"Chegou atrasado, mas ainda há o que salvar aqui. Deixa comigo.",
						"Os refugiados se escondem no vale; as máquinas varrem a floresta em ciclos.",
						"Aquele beacon pode abrir o caminho da evacuação. Vá com cuidado."
				}, new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
				if (Game.player != null) {
					com.traduvertgames.entities.Player.life = Math.min(
							com.traduvertgames.entities.Player.life
									+ (int) (com.traduvertgames.entities.Player.maxLife * 0.6),
							com.traduvertgames.entities.Player.maxLife);
					com.traduvertgames.entities.Player.shield = Math.min(
							com.traduvertgames.entities.Player.shield + 20,
							com.traduvertgames.entities.Player.maxShield);
					com.traduvertgames.entities.FloatingText.show("CURA +60% VIDA", (int) Game.player.getX(),
							(int) Game.player.getY() - 20, new Color(110, 255, 130), 60);
				}
			}
		});
	}

	/** Armeiro Mercúrio — arma cheia e +25 vida. */
	public static InteractiveNpc armorer(int x, int y) {
		return new InteractiveNpc(x, y, "Armeiro Mercúrio", new Color(255, 152, 0), new Color(255, 224, 178),
				new String[] {
						"Cada arma tem um papel. A sua precisa estar pronta para a próxima fase.",
						"Não desperdice munição: recarregue entre os combates.",
						"Pronto, piloto. Arma recarregada e kits de reparo aplicados."
				}, new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
				if (Game.player != null) {
					Game.player.addWeaponEnergy(200);
					PlayerLifeAdd.apply(25);
				}
			}
		});
	}

	/**
	 * Helper interno: soma vida sem estourar o máximo, seguindo a mesma
	 * disciplina dos métodos addMana/addShield do Player.
	 */
	private static final class PlayerLifeAdd {
		private PlayerLifeAdd() {
		}

		static void apply(int amount) {
			if (Game.player != null) {
				com.traduvertgames.entities.Player.life = Math.min(
						com.traduvertgames.entities.Player.life + amount,
						com.traduvertgames.entities.Player.maxLife);
			}
		}
	}
}
