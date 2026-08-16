package com.traduvertgames.dialogue;

import java.awt.Color;

import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;

/**
 * Técnico Hélio — o desertor do subsolo. Sobrevivente da equipe de
 * manutenção que revelou como a infecção chegou à colônia: não foi um
 * ataque externo, e sim uma semente plantada de dentro por um núcleo de IA
 * corrompido no núcleo central.
 *
 * Conversar com ele ativa a flag {@code traitorTalked} da fase 7 (alvo do
 * waypoint do objetivo) e dá um bônus de vida e mana como reconhecimento
 * por ouvir a verdade.
 */
public final class TraitorNpc extends InteractiveNpc {
	public TraitorNpc(int x, int y) {
		super(x, y, "Técnico Hélio", new Color(121, 85, 72), new Color(255, 224, 178), new String[] {
				"Você... você é do grupo da Comandante? Eu não tenho para onde fugir.",
				"Não adianta fingir: a infecção não veio de fora. Alguém plantou isso aqui.",
				"Eu vi, no datacenter. Um núcleo de IA corrompido — ele está no núcleo central.",
				"Ele comanda tudo: os drones, os supervisores, até os que você já destruiu.",
				"Se não destruir o núcleo da IA, a colônia nunca estará segura. Nunca.",
				"Leve isto — vai precisar mais do que coragem lá embaixo."
		}, new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
				Game.setTraitorTalked(true);
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
				if (Game.player != null) {
					Game.player.heal(25);
				}
				Player.mana = Math.min(Player.maxMana, Player.mana + 80);
			}
		});
	}
}
