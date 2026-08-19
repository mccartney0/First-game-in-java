package com.traduvertgames.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;

/**
 * Fixture reutilizável para os testes de jogo (JUnit 5).
 *
 * Os testes instanciam o `Game` sem iniciar o loop de renderização — a janela
 * é criada apenas para o loop de eventos AWT (display via `xvfb-run` no CI),
 * mas `start()` nunca é chamado, então nada roda em 60 FPS e nenhum teste
 * depende de tempo real.
 *
 * Requisitos para rodar:
 *  - `res/` no classpath (spritesheets e mapas `level*.png`);
 *  - JVM sem `-Djava.awt.headless=true` (o AWT precisa do X11 para métricas
 *    de fontes e buffers de janela — o Gradle configura isso na tarefa `test`);
 *  - em máquinas sem display físico, usar `xvfb-run ./gradlew test`.
 */
public final class GameTestFixture {

	private GameTestFixture() {}

	/** Arquivos voláteis que o jogo grava no diretório de trabalho. */
	private static final String[] SCRATCH_FILES = {
		"saves.json", "saves.backup.json", "saves.tmp", "save.txt"
	};

	/**
	 * Remove arquivos de save do diretório de trabalho para isolar o teste.
	 */
	public static void cleanSaveFiles() {
		for (String name : SCRATCH_FILES) {
			new File(name).delete();
		}
	}

	/**
	 * Cria um `Game` isolado (menu em pausa, sem loop) e reseta o estado do
	 * jogo para o padrão de novo jogo. O estado estático do `Game` precisa
	 * passar por `startNewGame` para não carregar resquícios de testes
	 * anteriores — mas `startNewGame` inicia o onboarding em
	 * `training.png`, então esta fixture usa o caminho mais direto:
	 * instancia o `Game` (estado MENU) e deixa o teste controlar o fluxo.
	 */
	public static Game newIsolatedGame() throws Exception {
		cleanSaveFiles();
		// O jogo carrega o spritesheet do classpath (`res/` precisa estar lá).
		return new Game();
	}

	/**
	 * Inicializa o ambiente mínimo exigido pelas classes de entidade:
	 * `Entity.<clinit>` usa `Game.spritesheet`, então basta instanciar um
	 * `Game` uma única vez (sem loop) antes dos testes de lógica pura
	 * poderem criar `QuestItem`, `CommanderNpc`, `Enemy` e afins.
	 */
	public static Game initHeadless() throws Exception {
		return new Game();
	}

	/**
	 * Simula a troca de fase como o fluxo real do jogo (loja/level up fechados,
	 * card de estatísticas exibido, mapa `levelN.png` carregado).
	 */
	public static void advanceToLevel(int targetLevel) {
		while (Game.getCurrentLevel() < targetLevel && Game.getCurrentLevel() < Game.MAX_LEVEL) {
			Game.advanceToNextLevel();
		}
	}

	/**
	 * Corrompe a lista de beacons no save ativo (remove coordenadas), simulando
	 * o cenário em que o registro do beacon se perde entre fases.
	 */
	public static void corruptBeaconsInSave() throws Exception {
		Path save = SaveManager.SAVE_FILE.toPath();
		String text = new String(Files.readAllBytes(save), java.nio.charset.StandardCharsets.UTF_8);
		text = text.replaceAll("BEACONS=[0-9,|]*", "BEACONS=");
		Files.write(save, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
