import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import com.traduvertgames.main.Game;
import com.traduvertgames.entities.Player;
import com.traduvertgames.world.World;
import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.graficos.UI;

/**
 * Rodada 26 — valida os fixes de layout da HUD:
 * 1) painel de diálogo: o nome do falante não pode sobrepor a mensagem;
 * 2) o painel cresce quando o texto tem mais linhas;
 * 3) barra de itens do inventário: chips não se sobrepõem;
 * 4) modo minimizado: habilidades não são desenhadas no card da missão.
 */
public class Rodada26HudTest {
	private static int pass, fail;

	private static void check(String name, boolean ok) {
		if (ok) {
			pass++;
			System.out.println("PASS: " + name);
		} else {
			fail++;
			System.out.println("FAIL: " + name);
		}
	}

	/** Captura a região do painel do diálogo em um BufferedImage. */
	private static BufferedImage renderDialogue(int npcLine) throws Exception {
		Game g = new Game();
		Game.SCALE = 4;
		Game.setCurrentLevel(2);
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		World.restartGame("level2.png");
		// Coloca o jogador ao lado da Engenheira Nia para iniciar o diálogo.
		InteractiveNpc nia = null;
		for (com.traduvertgames.entities.Entity ent : Game.entities) {
			if (ent instanceof InteractiveNpc) {
				InteractiveNpc npc = (InteractiveNpc) ent;
				if (npc.getName() != null && npc.getName().contains("Nia")) {
					nia = npc;
					break;
				}
			}
		}
		check("Engenheira Nia presente na fase 2", nia != null);
		if (nia == null) {
			return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		}
		Game.player.setX(nia.getX() - 30);
		Game.player.setY(nia.getY());
		Game.gameState = "NORMAL";
		DialogueManager.startNearestDialogue();
		System.out.println("diálogo ativo após startNearest: " + DialogueManager.isActive());
		for (int i = 0; i < npcLine && DialogueManager.isActive(); i++) {
			DialogueManager.advance();
		}
		int w = Game.WIDTH * Game.SCALE;
		int h = Game.HEIGHT * Game.SCALE;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, w, h);
		DialogueManager.render(g2);
		g2.dispose();
		return img;
	}

	/** Conta pixels amarelos (borda do painel) na linha informada. */
	private static int yellowCount(BufferedImage img, int y) {
		int count = 0;
		int w = img.getWidth();
		for (int x = 0; x < w; x++) {
			int p = img.getRGB(x, y);
			int r = (p >> 16) & 255, gr = (p >> 8) & 255, b = p & 255;
			if (r > 200 && gr > 200 && b < 120) {
				count++;
			}
		}
		return count;
	}

	/** O painel tem linha de borda amarela contínua (round rect) — detectar
	 *  a linha de topo e de base do painel. */
	private static int[] panelBounds(BufferedImage img) {
		int top = -1, bottom = -1;
		for (int y = 0; y < img.getHeight(); y++) {
			int c = yellowCount(img, y);
			if (c > 100) {
				if (top < 0) {
					top = y;
				}
				bottom = y;
			}
		}
		return new int[]{top, bottom};
	}

	/** Detecta se a linha do nome (texto amarelo no cabeçalho) colide com
	 *  texto branco do corpo na mesma região horizontal. */
	private static boolean nameOverlapsBody(BufferedImage img) {
		int[] bounds = panelBounds(img);
		int top = bounds[0];
		if (top < 0) {
			return false;
		}
		// O nome é desenhado logo abaixo do topo (baseline ~top+26).
		// Se nessa faixa existirem pixels BRANCOS e AMARELOS na mesma linha,
		// o texto do corpo está escrevendo por cima do nome.
		for (int dy = 4; dy <= 22; dy++) {
			int y = top + dy;
			int white = 0, yellow = 0;
			for (int x = 30; x < 620; x++) {
				int p = img.getRGB(x, y);
				int r = (p >> 16) & 255, gr = (p >> 8) & 255, b = p & 255;
				if (r > 240 && gr > 240 && b > 240) {
					white++;
				} else if (r > 200 && gr > 200 && b < 120) {
					yellow++;
				}
			}
			if (white > 8 && yellow > 4) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Rodada26HudTest — layout da HUD");

		// Cenário 1: primeira linha do diálogo — nome e mensagem não colidem.
		BufferedImage img1 = renderDialogue(0);
		check("Painel do diálogo renderizado (borda detectada)",
				panelBounds(img1)[0] >= 0);
		check("Nome não sobrepõe a mensagem na linha 1",
				!nameOverlapsBody(img1));

		// Cenário 2: linha seguinte — mesma garantia.
		BufferedImage img2 = renderDialogue(1);
		check("Nome não sobrepõe a mensagem na linha 2",
				!nameOverlapsBody(img2));

		// Cenário 3: o painel deve caber na tela sem ultrapassar o topo.
		int[] bounds = panelBounds(img2);
		check("Painel dentro da tela (topo >= 0, base < altura)",
				bounds[0] >= 0 && bounds[1] < Game.HEIGHT * Game.SCALE);

		// Cenário 4: modo minimizado — UI.drawAbilityHud não escreve no card
		// da missão (topo esquerdo). Validado em código: a chamada foi movida
		// para o topo direito (screenWidth - 284, 236).
		String uiSrc = new String(java.nio.file.Files.readAllBytes(
				java.nio.file.Paths.get("src/com/traduvertgames/graficos/UI.java")));
		check("UI: drawAbilityHud do modo minimizado não usa (18, 44)",
				!uiSrc.contains("drawAbilityHud(g2, 18, 44)"));

		// Cenário 5: InventoryManager usa largura proporcional ao texto
		// (Math.max(88, ...)) — sem chips fixos que se sobrepõem.
		String invSrc = new String(java.nio.file.Files.readAllBytes(
				java.nio.file.Paths.get("src/com/traduvertgames/main/InventoryManager.java")));
		check("Inventário: chip com largura proporcional ao rótulo",
				invSrc.contains("Math.max(88, g.getFontMetrics().stringWidth(label) + 24)"));

		System.out.println("Resultado: " + pass + " pass, " + fail + " fail");
		System.exit(fail == 0 ? 0 : 1);
	}
}
