package com.traduvertgames.dialogue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

/**
 * NPC/objeto interativo com diálogo. Aproxime-se do personagem e pressione R
 * para conversar. Subclasses definem as falas, a cor e a ação ao concluir.
 *
 * O prompt de interação aparece apenas dentro do raio definido e a conversa
 * pausa inimigos e objetivos durante a duração (comportamento parecido ao
 * onboarding, mas restrito ao diálogo).
 */
public class InteractiveNpc extends Entity {

	/** Raio em pixels a partir do qual o prompt "R" aparece. */
	public static final int INTERACTION_RADIUS = 48;

	private final String name;
	private final String[] lines;
	private final Color bodyColor;
	private final Color headColor;
	private final InteractionListener listener;
	private boolean finished = false;
	private boolean wasInteracted = false;

	public InteractiveNpc(int x, int y, String name, Color bodyColor, Color headColor, String[] lines,
			InteractionListener listener) {
		super(x, y, 16, 16, null);
		this.name = name;
		this.bodyColor = bodyColor;
		this.headColor = headColor;
		this.lines = lines;
		this.listener = listener;
		setMask(2, 2, 12, 12);
	}

	/**
	 * Cria um NPC com falas padrão — útil para NPCs puramente narrativos sem
	 * recompensa.
	 */
	public InteractiveNpc(int x, int y, String name, Color bodyColor, String[] lines) {
		this(x, y, name, bodyColor, new Color(255, 224, 178), lines, new InteractionListener() {
			@Override
			public void onInteractionStart(InteractiveNpc npc) {
			}

			@Override
			public void onInteractionEnd(InteractiveNpc npc) {
			}
		});
	}

	public String getName() {
		return name;
	}

	public boolean hasFinished() {
		return finished;
	}

	/** Indica se o jogador está perto o suficiente para interagir. */
	public boolean isWithinReach() {
		if (Game.player == null) {
			return false;
		}
		return calculateDistance(getX(), getY(), Game.player.getX(), Game.player.getY())
				<= INTERACTION_RADIUS;
	}

	@Override
	public void update() {
		// NPC só interage durante o jogo normal (não em menus, loja, pausa etc.).
		if (!"NORMAL".equals(Game.gameState)) {
			return;
		}
		// A conversa pausa durante o diálogo — nada a atualizar além do estado.
		if (finished) {
			return;
		}
		// Registra o primeiro momento em que o jogador interage (usado para
		// desbloquear flags de diálogo no Journal).
		if (!wasInteracted && isWithinReach() && DialogueManager.isActive() && DialogueManager.getTarget() == this) {
			wasInteracted = true;
		}
	}

	/**
	 * @return true se esta conversa já foi concluída e está persistida no save
	 *         (v3) — usado pelo indicador visual "✓ conversado".
	 */
	private boolean isDialogueSaved() {
		return com.traduvertgames.main.SaveManager.hasNpcDialogue(name,
				com.traduvertgames.quest.QuestManager.getCurrentLevel());
	}

	@Override
	public void render(Graphics g) {
		if (finished) {
			return;
		}
		int screenX = this.getX() - Camera.x;
		int screenY = this.getY() - Camera.y;
		// Corpo (túnica)
		g.setColor(bodyColor);
		g.fillRoundRect(screenX + 3, screenY + 10, 10, 6, 4, 4);
		// Cabeça
		g.setColor(headColor);
		g.fillOval(screenX + 5, screenY + 5, 6, 6);
		// Cabelo/capuz escuro
		g.setColor(new Color(30, 30, 30));
		g.fillRect(screenX + 6, screenY + 2, 4, 4);
		// Indicador "✓ conversado": conversa já concluída nesta sessão/save
		// e jogador próximo o suficiente para ver o status.
		if (isDialogueSaved() && isWithinReach()) {
			String check = "✓";
			g.setFont(new Font("arial", Font.BOLD, 7 * Game.SCALE / 4 + 2));
			int w = g.getFontMetrics().stringWidth(check);
			g.setColor(new Color(0, 0, 0, 180));
			g.fillOval(screenX + 6 - w / 2 - 2, screenY - 10, w + 4, 9);
			g.setColor(new Color(110, 255, 130));
			g.drawString(check, screenX + 6 - w / 2, screenY - 2);
		}
	}

	/**
	 * Chamado pelo DialogueManager quando a conversa com este NPC termina.
	 * A marcação de diálogo concluído é persistida no save (v3) e refletida
	 * no indicador visual "✓ conversado" mesmo sem recarregar.
	 */
	public void finishInteraction() {
		if (finished) {
			return;
		}
		finished = true;
		// Save v3: registra a conversa concluída por NPC/fase.
		com.traduvertgames.main.SaveManager.markNpcDialogue(name,
				com.traduvertgames.quest.QuestManager.getCurrentLevel());
		if (listener != null) {
			listener.onInteractionEnd(this);
		}
	}

	/**
	 * Chamado pelo DialogueManager quando a conversa começa.
	 */
	public void startInteraction() {
		if (listener != null) {
			listener.onInteractionStart(this);
		}
	}

	/** Notificação de que o diálogo foi concluído e o overlay fechou. */
	public void onDialogueClosed() {
		finishInteraction();
	}

	/**
	 * Listener opcional para recompensas/flags ao conversar com o NPC.
	 * onInteractionStart roda ao abrir o diálogo; onInteractionEnd ao terminar.
	 */
	public interface InteractionListener {
		void onInteractionStart(InteractiveNpc npc);

		void onInteractionEnd(InteractiveNpc npc);
	}

	/**
	 * Desenha o prompt de interação "R — conversar" flutuando acima do NPC
	 * quando o jogador está perto e o diálogo está inativo.
	 */
		public static void renderPrompt(Graphics g, InteractiveNpc npc, int scale) {
		if (npc.hasFinished() || !npc.isWithinReach()) {
			return;
		}
		// Badge ancorado no topo do NPC: tecla destacada em amarelo ("R") e
		// nome do personagem ao lado — legível contra qualquer fundo.
		String key = "R";
		String label = npc.getName();
		Font badgeFont = new Font("arial", Font.BOLD, 9 * scale / 4 + 2);
		Font nameFont = new Font("arial", Font.BOLD, 8 * scale / 4 + 2);
		int keyW = g.getFontMetrics(badgeFont).stringWidth(key);
		int nameW = g.getFontMetrics(nameFont).stringWidth(label);
		int totalW = keyW + 12 + nameW + 10;
		int totalH = 13 * scale / 4 + 2;
		int screenX = npc.getX() - Camera.x;
		int screenY = npc.getY() - Camera.y;
		int px = screenX + 8 - totalW / 2;
		int py = screenY - totalH - 4;
		// Sombra do badge para destacar sobre chão e paredes
		g.setColor(new Color(0, 0, 0, 220));
		g.fillRoundRect(px - 2, py - 2, totalW + 4, totalH + 4, 6 * scale / 4, 6 * scale / 4);
		g.setColor(new Color(255, 235, 59, 255));
		g.setFont(badgeFont);
		g.drawString(key, px + 4, py + totalH - 3);
		g.setColor(new Color(230, 240, 250, 250));
		g.setFont(nameFont);
		g.drawString("\u2014 " + label, px + keyW + 8, py + totalH - 3);
	}
}
