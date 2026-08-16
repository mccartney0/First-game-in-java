import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Rodada de QA da loja — valida o fluxo completo da ShopManager sem precisar
 * instanciar o jogo (AWT), usando reflexão para acessar os campos estáticos
 * e o Spritesheet real do jogo para permitir a aplicação de compras.
 */
public class ShopQaTest {

	static int passed = 0;
	static int failed = 0;

	static void check(boolean ok, String name) {
		if (ok) {
			passed++;
			System.out.println("  PASS " + name);
		} else {
			failed++;
			System.out.println("  FAIL " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		// Carrega todas as classes SEM inicializá-las: Game importa Player que
		// importa Entity, e Entity.<clinit> lê Game.spritesheet — que só pode
		// ser injetado depois. A inicialização fica para os invokes abaixo.
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Class<?> shopClass = Class.forName("com.traduvertgames.main.ShopManager", false, cl);
		Class<?> gameClass = Class.forName("com.traduvertgames.main.Game", false, cl);
		Class.forName("com.traduvertgames.entities.Player", false, cl);
		Field selection = shopClass.getDeclaredField("selection"); selection.setAccessible(true);
		Field openF = shopClass.getDeclaredField("open"); openF.setAccessible(true);
		Field cooldown = shopClass.getDeclaredField("escCooldown"); cooldown.setAccessible(true);
		Field score = gameClass.getDeclaredField("score"); score.setAccessible(true);
		Class<?> playerClass = Class.forName("com.traduvertgames.entities.Player", false, cl);
		Field maxLife = playerClass.getDeclaredField("maxLife"); maxLife.setAccessible(true);
		Field life = playerClass.getDeclaredField("life"); life.setAccessible(true);
		Field gsSprite = gameClass.getDeclaredField("spritesheet"); gsSprite.setAccessible(true);
		Field closeNext = shopClass.getDeclaredField("closeOnNextEnter"); closeNext.setAccessible(true);
		Field shield = playerClass.getDeclaredField("shield"); shield.setAccessible(true);
		Field maxShield = playerClass.getDeclaredField("maxShield"); maxShield.setAccessible(true);
		Method navigateA = shopClass.getDeclaredMethod("navigateA");
		Method navigateD = shopClass.getDeclaredMethod("navigateD");
		Method navigateUp = shopClass.getDeclaredMethod("navigateUp");
		Method navigateDown = shopClass.getDeclaredMethod("navigateDown");
		Method confirmOrPurchase = shopClass.getDeclaredMethod("confirmOrPurchase");
		Method open = shopClass.getDeclaredMethod("open");

		// Ambiente mínimo: as entidades usam Game.spritesheet na
		// inicialização de classe (Entity.<clinit>), por isso a
		// Spritesheet real é carregada SEM inicializar as classes e
		// injetada no Game antes de qualquer outra referência ao jogo.
		Class<?> spriteClass = Class.forName("com.traduvertgames.graficos.Spritesheet", false, shopClass.getClassLoader());
		java.lang.reflect.Constructor<?> spriteCtor = spriteClass.getDeclaredConstructor(String.class);
		spriteCtor.setAccessible(true);
		Object spritesheet = spriteCtor.newInstance("/spritesheet.png");
		gsSprite.setAccessible(true);
		gsSprite.set(null, spritesheet);

		// --- Navegação ---
		open.invoke(null);
		check((int) selection.get(null) == 0, "loja abre na seleção 0");
		navigateDown.invoke(null);
		check((int) selection.get(null) == 1, "seta para baixo avança 0→1");
		navigateUp.invoke(null);
		check((int) selection.get(null) == 0, "seta para cima volta 1→0");
		navigateA.invoke(null);
		// Navegação A/D iguala as setas (lista vertical): A sobe com wrap-around
		check((int) selection.get(null) == 12, "A navega (wrap: 0→12 itens)");
		navigateD.invoke(null);
		check((int) selection.get(null) == 0, "D navega (wrap: 12→0)");

		// Força a inicialização das classes do jogo SOMENTE agora, depois
		// que a spritesheet real já foi injetada (Game.<clinit> não toca
		// spritesheet; as entidades sim — Entity.<clinit> lerá o valor injetado).
		Class.forName("com.traduvertgames.main.Game", true, cl);
		Class.forName("com.traduvertgames.entities.Player", true, cl);
		Class.forName("com.traduvertgames.main.ShopManager", true, cl);

		// --- Compra sem pontuação ---
		score.set(null, 10);
		maxLife.set(null, 100); life.set(null, 100);
		confirmOrPurchase.invoke(null);
		check((boolean) openF.get(null), "compra sem pontuação não fecha a loja");
		check((double) life.get(null) == 100.0, "vida não muda sem pontuação");

		// --- Compra bem-sucedida mantém a loja aberta (compras múltiplas) ---
		score.set(null, 10000);
		maxLife.set(null, 200); // CURAR é capado em maxLife: sem isso o teste
		confirmOrPurchase.invoke(null);
		check((boolean) openF.get(null), "loja permanece aberta após compra");
		check((boolean) closeNext.get(null), "closeOnNextEnter ativo após compra");
		check((double) life.get(null) == 160.0, "vida subiu com a compra (CURAR 60)");
		check((int) score.get(null) == 9200, "pontuação debitada (800)");
		// Nova compra sem fechar: navega e compra de novo (compras múltiplas)
		// (o closeOnNextEnter só é consumido no Enter de confirmação abaixo)
		navigateDown.invoke(null);
		confirmOrPurchase.invoke(null);
		check((boolean) openF.get(null), "segunda compra mantém a loja aberta");
		check((int) score.get(null) == 8600, "segunda compra debita (600, ESCUDO)");

		// --- Enter de confirmação fecha a loja ---
		// Com closeOnNextEnter ativo (e sem navegar desde a compra), o Enter
		// confirma e FECHA a loja sem recomprar.
		confirmOrPurchase.invoke(null);
		check(!(boolean) openF.get(null), "Enter de confirmação fecha a loja");
		check((int) score.get(null) == 8600, "nada foi debitado (loja fechou antes de comprar)");
		check((int) cooldown.get(null) == 15, "ESC em cooldown evita o 'brilho' da pausa");

		// --- ESC fecha e o cooldown ignora key-repeat ---
		// Após fechar (ESC), closeOnNextEnter já foi consumido; reabrir a loja
		// e dar Enter compra um item novo em vez de fechar (comportamento correto).
		open.invoke(null);
		confirmOrPurchase.invoke(null);
		check((boolean) openF.get(null), "reaberta a loja, Enter compra novo item (não fecha)");
		check((int) score.get(null) == 7800, "compra pós-reabertura debita (800, CURAR)");
		// O campo público isEscOnCooldown cobre o key-repeat no handler do Game
		Method escCooldown = shopClass.getDeclaredMethod("isEscOnCooldown");
		check((boolean) escCooldown.invoke(null), "isEscOnCooldown ativo após fechar");

		// Limpeza
		openF.set(null, false);
		System.out.println("Resultados: " + passed + " passaram, " + failed + " falharam");
	}
}
