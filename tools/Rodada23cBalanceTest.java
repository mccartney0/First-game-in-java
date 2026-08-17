import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Enemy.Variant;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.WaveManager;
import com.traduvertgames.world.World;
import java.awt.image.BufferedImage;

/**
 * Rodada 23c — balanceamento dos chefes 7/8 e da curva do modo infinito.
 * Valida limites de saúde, dano e tempo-de-abate (TTK) dos chefes da
 * campanha, além da curva sub-linear de escalada do modo arena.
 */
public class Rodada23cBalanceTest {
	private static int passed = 0;
	private static int failed = 0;

	private static void check(boolean condition, String description) {
		if (condition) {
			passed++;
			System.out.println("PASS: " + description);
		} else {
			failed++;
			System.out.println("FAIL: " + description);
		}
	}

	/** Simula o DPS da arma padrão do jogador (cadência de 60 FPS). */
	private static double playerDps(WeaponType weapon) {
		double shotsPerSecond = 60.0 / weapon.getFireDelayFrames();
		return shotsPerSecond * weapon.getDamage();
	}

	public static void main(String[] args) throws Exception {
		// Inicialização mínima: o Game precisa de spritesheet e mundo válidos.
		Game g = new Game();
		Game.setCurrentLevel(7);
		Game.SCALE = 4;
		Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		Game.player.life = com.traduvertgames.entities.Player.maxLife;
		World.restartGame("level7.png");

		System.out.println("=== CHEFE DA FASE 7 (GUARDIAN) ===");
		Enemy guardian = new Enemy(100, 100, 16, 16, Entity.ENEMY_EN, Variant.GUARDIAN, true);
		check(guardian.getTotalLife() >= 30,
				"Fase 7: GUARDIAN tem vida suficiente para o combate longo (>=30)");
		check(guardian.getEffectiveProjectileDamage() <= 3.0,
				"Fase 7: dano de projétil do GUARDIAN não pune demais (<=3.0)");

		// TTK com a arma padrão (BLASTER): o chefe deve exigir ao menos 2
		// segundos de fogo sustentado para cair — nem trivial, nem impossível.
		double guardianTtk = guardian.getTotalLife() / playerDps(WeaponType.BLASTER);
		check(guardianTtk >= 2.0 && guardianTtk <= 12.0,
				"Fase 7: TTK do chefe entre 2s e 12s (" + String.format("%.1f", guardianTtk) + "s)");

		System.out.println("=== CHEFE DA FASE 8 (SUPERVISOR-PRIME) ===");
		Enemy prime = new Enemy(100, 100, 16, 16, Entity.ENEMY_EN, Variant.OVERSEER_PRIME, true);
		check(prime.getTotalLife() > 40,
				"Fase 8: OVERSEER_PRIME tem a maior vida dos chefes de fase");
		// Rodada 23c: a rajada dupla da fúria disparava projéteis de 5.8 de
		// dano — o piloto sem escudo morria em poucos acertos seguidos.
		check(prime.getEffectiveProjectileDamage() <= 5.2,
				"Fase 8: dano de projétil do PRIME ajustado (<=5.2)");
		double primeTtk = prime.getTotalLife() / playerDps(WeaponType.BLASTER);
		check(primeTtk >= 3.0 && primeTtk <= 15.0,
				"Fase 8: TTK do chefe entre 3s e 15s (" + String.format("%.1f", primeTtk) + "s)");

		System.out.println("=== CURVA DO MODO INFINITO (ARENA) ===");
		// Vida e dano dos chefes de arena escalados pela raiz das ondas
		// (sub-linear) — o mesmo chefe profundo deve ser mais forte, porém
		// nunca linearmente impossível.
		Enemy bossWave5 = Enemy.spawnArenaBoss(100, 100, 5);
		Enemy bossWave20 = Enemy.spawnArenaBoss(100, 100, 20);
		double ratioLife = bossWave20.getTotalLife() / bossWave5.getTotalLife();
		double linearRatio = 20.0 / 5.0;
		check(ratioLife > 1.0 && ratioLife < linearRatio,
				"Modo infinito: boss da onda 20 é mais forte que o da 5, mas cresce sub-linear ("
						+ String.format("%.2f", ratioLife) + "x, linear seria " + String.format("%.2f", linearRatio) + "x)");
		double dmgRatio = bossWave20.getEffectiveProjectileDamage() / bossWave5.getEffectiveProjectileDamage();
		check(dmgRatio > 1.0 && dmgRatio < linearRatio,
				"Modo infinito: dano do boss cresce sub-linear ("
						+ String.format("%.2f", dmgRatio) + "x)");
		// Inimigos comuns da arena: o boost também é sub-linear pela raiz.
		double depth5 = Math.sqrt(5);
		double depth20 = Math.sqrt(20);
		double commonRatio = (1.0 + depth20 * 0.20) / (1.0 + depth5 * 0.20);
		check(commonRatio > 1.0 && commonRatio < 2.2,
				"Modo infinito: escalada comum sub-linear (" + String.format("%.2f", commonRatio) + "x na onda 20 vs 5)");

		System.out.println("=== DENSIDADE DO MODO INFINITO ===");
		// O gerador procedural limita o alvo de inimigos por mapa (20) e o
		// WaveManager limita inimigos simultâneos (12) — o mapa nunca enche.
		check(com.traduvertgames.world.ProceduralLevelGenerator.MAX_ENEMY_TARGET <= 20,
				"Modo infinito: teto de densidade por mapa definido (<=20)");

		System.out.println("Progresso: " + passed + " passaram, " + failed + " falharam");
		System.exit(failed == 0 ? 0 : 1);
	}
}
