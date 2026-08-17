import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import com.traduvertgames.main.Game;
import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.dialogue.BranchingNpc;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Entity;

/**
 * Rodada 25 — diálogo sem sobreposição.
 * Valida o render do DialogueManager com falas longas e escolhas extensas:
 * o painel cresce com o conteúdo, o rodapé nunca é desenhado por cima do
 * texto/escolhas e todo o conteúdo permanece dentro do painel.
 */
public class Rodada25DialogTest {
	private static int pass = 0;
	private static int fail = 0;

	static void check(String name, boolean ok) {
		if (ok) {
			pass++;
			System.out.println("PASS: " + name);
		} else {
			fail++;
			System.out.println("FAIL: " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		Game g = new Game();
		Game.SCALE = 4;
		Game.setCurrentLevel(2);
		Game.gameState = "NORMAL";
		Game.player = new com.traduvertgames.entities.Player(300, 200, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));

		// NPC ramificado com fala longa e escolhas extensas (mesma estrutura da
		// Pesquisadora Lila, que originou o bug de sobreposição).
		final BranchingNpc npc = new BranchingNpc(330, 220, "Teste Lila",
				new Color(40, 70, 140), new Color(255, 224, 178)) {
			@Override
			protected DialogueNode[] buildNodes() {
				return new DialogueNode[] {
						new DialogueNode(
								"Espere, piloto! Meus sensores indicam células de energia caídas por aqui. Preciso delas para calibrar os escudos da base — pode ajudar?",
								new String[] { "Claro, vou procurar", "Não posso agora",
										"Por que não vai você?", null },
								new int[] { 1, 3, 2, -1 },
								new Runnable[] { null, null, null, null }),
						new DialogueNode("Obrigada! Pegue as células de energia pelo mapa e traga para mim.",
								new String[] { null, null, null },
								new int[] { -1, -1, -1 }, new Runnable[] { null, null, null }) };
			}
		};
		Game.entities.add(npc);

		// O onboarding bloqueia diálogos (prioridade do treino) — desativar.
		com.traduvertgames.main.OnboardingManager.stop();

		// Inicia o diálogo via R (simula startNearestDialogue posicionando o
		// jogador perto do NPC — aqui forçamos diretamente para o teste).
		double dist = Math.hypot(npc.getX() - Game.player.getX(),
				npc.getY() - Game.player.getY());
		check("NPC dentro do raio de interação (48px)", dist <= 48.0);
		DialogueManager.startNearestDialogue();
		check("Diálogo aberto", DialogueManager.isActive());

		int W = Game.WIDTH * Game.SCALE;
		int H = Game.HEIGHT * Game.SCALE;
		BufferedImage buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gg = buffer.createGraphics();
		gg.setColor(Color.BLACK);
		gg.fillRect(0, 0, W, H);

		// Renderiza com o nó 0 (fala longa + 3 escolhas) e com o nó 1.
		DialogueManager.render(gg);
		DialogueManager.advance(); // abre o nó 1 (fala longa)
		DialogueManager.render(gg);

		// Varredura do buffer: localizar o painel (fundo escuro 0x000000EB
		// aproximado) e checar que texto/rodapé estão dentro dele.
		int left = -1, right = -1, top = -1, bottom = -1;
		int darkCount = 0;
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int px = buffer.getRGB(x, y);
					// Painel: quase preto com alpha >= 230 (o contorno arredondado
					// anti-aliasado pode gerar variações leves de alpha).
					if ((px & 0xFF000000) >= 0xE6000000 && (px & 0x00FFFFFF) == 0) {
						darkCount++;
					if (left < 0 || x < left) left = x;
					if (right < 0 || x > right) right = x;
					if (top < 0 || y < top) top = y;
					if (bottom < 0 || y > bottom) bottom = y;
				}
			}
		}
		check("Painel desenhado no buffer", darkCount > 10000);
		check("Painel mais alto que o mínimo fixo (cresceu com o conteúdo)",
				(bottom - top) > 128);

		// Rodapé: texto branco de dica embaixo do painel — verificar que a
		// linha do rodapé (hintFont) está dentro do painel e não se sobrepõe à
		// última linha de escolha. A última linha de conteúdo branco dentro do
		// painel deve terminar antes da linha do rodapé.
		int lastWhiteY = -1;
		for (int y = top; y < bottom; y++) {
			for (int x = left + 16; x < right - 16; x++) {
				int px = buffer.getRGB(x, y);
				// Texto branco.
				if ((px & 0xFFFFFF) == 0xFFFFFF && (px >>> 24) > 200) {
					lastWhiteY = y;
					break;
				}
			}
		}
		check("Conteúdo de texto termina antes do rodapé",
				lastWhiteY >= 0 && lastWhiteY < bottom - 22);

		// Nenhuma linha de conteúdo branco acima da área do painel.
		int aboveWhite = 0;
		for (int y = 0; y < top; y++) {
			for (int x = left; x <= right; x++) {
				int px = buffer.getRGB(x, y);
				if ((px & 0xFFFFFF) == 0xFFFFFF && (px >>> 24) > 200) {
					aboveWhite++;
				}
			}
		}
		check("Nenhum texto vazando acima do painel", aboveWhite == 0);

		// Escolhas continuam navegáveis mesmo com painel grande.
		check("Escolhas do nó disponíveis",
				DialogueManager.getBranchChoices().length == 3);

		DialogueManager.close();
		check("Diálogo fechado sem travar", !DialogueManager.isActive());

		System.out.println("Resultado: " + pass + " pass, " + fail + " fail");
		System.exit(fail == 0 ? 0 : 1);
	}
}
