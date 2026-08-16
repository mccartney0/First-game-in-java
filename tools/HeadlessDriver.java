import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;

import javax.imageio.ImageIO;

/**
 * Driver headless de validação visual: envia eventos de teclado via
 * java.awt.Robot (mesma sessão de display :120 do jogo) e captura
 * screenshots para verificação manual das telas implementadas.
 * Uso: java -cp bin tools HeadlessDriver [arquivo.png]
 */
public class HeadlessDriver {

	private static Robot robot;

	public static void main(String[] args) throws Exception {
		robot = new Robot();
		if (args.length < 1) {
			System.err.println("uso: HeadlessDriver saida.png");
			return;
		}
		String out = args[0];
		// Sequência: desce no menu principal até "Carregar jogo" e confirma,
		// depois captura a tela do carregamento (melhor partida).
		waitMs(1500);
		keyPress(KeyEvent.VK_DOWN);
		keyPress(KeyEvent.VK_DOWN);
		keyPress(KeyEvent.VK_DOWN);
		keyPress(KeyEvent.VK_ENTER);
		waitMs(2000);
		capture(out);
	}

	public static void waitMs(long ms) {
		Instant start = Instant.now();
		while (Duration.between(start, Instant.now()).toMillis() < ms) {
			robot.waitForIdle();
			try {
				Thread.sleep(50);
			} catch (InterruptedException ignored) {}
		}
	}

	public static void keyPress(int keyCode) {
		robot.setAutoWaitForIdle(true);
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
	}

	public static void capture(String path) throws Exception {
		BufferedImage img = robot.createScreenCapture(
			new java.awt.Rectangle(0, 0,
				Toolkit_getWidth(), Toolkit_getHeight()));
		ImageIO.write(img, "png", new File(path));
		System.out.println("capturado: " + path);
	}

	private static int Toolkit_getWidth() {
		return (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	}

	private static int Toolkit_getHeight() {
		return (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight();
	}
}
