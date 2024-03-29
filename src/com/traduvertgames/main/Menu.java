package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.World;

public class Menu {

	public String[] options = { "novo jogo", "carregar jogo", "sair" };

	public int currentOption = 0;
	public int maxOption = options.length - 1;

	public boolean up, down, enter;

	public static boolean pause = false;
	
	public static boolean saveExists = false;
	public static boolean saveGame = false;
	public static String[] spl2;

	public void update() {
		File file = new File("save.txt");
		if(file.exists()) {
			saveExists = true;
		}else {
			saveExists = false;
		}
		
		if (up) {
			up = false;
			currentOption--;
			if (currentOption < 0)
				currentOption = maxOption;
		}
		if (down) {
			down = false;
			currentOption++;
			if (currentOption > maxOption)
				currentOption = 0;
		}
		if (enter) {
			// Inserindo m�sica
			Sound.music.loop();
			enter = false;
			if (options[currentOption] == "novo jogo" || options[currentOption] == "continuar") {
				Game.gameState = "NORMAL";
				pause = false;
				
// Deletando o arquivo salvo se clicar em novo jogo novamente
				file = new File("save.txt");
				file.delete();
			}else if(options[currentOption] == "carregar jogo") {
				file = new File("save.txt");
				if(file.exists()) {
					String saver = loadGame(20);
					try {
						applySave(saver);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			} else if (options[currentOption] == "sair") {
				JOptionPane.showConfirmDialog(null, "Deseja realmente sair?", "Fechar o jogo", currentOption);
				System.exit(1);
			}
		}
	}

	public static void applySave(String str) throws IOException {
		String[] spl = str.split("/");
		System.out.println("STR: " + str);
		for(int i = 0; i < spl.length; i++)
		{
			spl2 = spl[i].split(":");
			// Criando os cases para inserir novos dados no SAVE
			switch (spl2[0])
			{
				case "vida":
					Player.life = Integer.parseInt(spl2[1]);
					break;
					
				case "mana":
					Player.mana = Integer.parseInt(spl2[1]);
					break;
				
				case "inimigosMortos":
					Enemy.enemies = Integer.parseInt(spl2[1]);
					break;

				case "level":
					System.out.println("level salvo: " + spl2[1]);
					World.restartGame("level" + spl2[1] + ".png");
					Game.gameState = "NORMAL";
					pause = false;
					break;

			}
		}
	}
	
	public static String loadGame(int encode)
	{
		String line = "";
		File file = new File("save.txt");

		if(file.exists())
		{
			try
			{
				String singleLine = null;
				BufferedReader reader = new BufferedReader(new FileReader("save.txt"));

				try
				{
					while((singleLine = reader.readLine()) != null)
					{
						String[] trans = singleLine.split(":");
						char[] val = trans[1].toCharArray();
						System.out.println(val);
						trans[1] = "";

						for(int i = 0; i < val.length; i++)
						{
							val[i] -= encode;
							trans[1] += val[i];
						}

						line += trans[0];
						line += ":";
						line += trans[1];
						line += "/";
					}
				}
				catch (IOException ignored) {}
			}
			catch (FileNotFoundException ignored) {}

		}

		return line;
	}

	public static void saveGame(String[] val1, int[] val2, int encode) {
		BufferedWriter write = null;
		try {
			write = new BufferedWriter(new FileWriter("save.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < val1.length; i++) {
			String current = val1[i];
			current += ":";
			char[] value = Integer.toString(val2[i]).toCharArray();

			for (int n = 0; n < value.length; n++) {
				value[n] += encode;
				current += value[n];
			}
			try {
				assert write != null;
				write.write(current);
				if (i < val1.length - 1)
					write.newLine();
			} catch (IOException e) {
			}
		}
		try {
			assert write != null;
			write.flush();
			write.close();
		} catch (IOException ignored) {
		}
	}

	public void render(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(new Color(0, 0, 0, 150));
		g2.fillRect(0, 0, Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE);
		g.setColor(Color.yellow);
		g.setFont(new Font("arial", Font.BOLD, 40));
		g.drawString(">Traduvert<", 245, 134);

//Menu do jogo
		g.setColor(Color.white);
		g.setFont(new Font("arial", Font.BOLD, 25));
		if (pause == false)
			g.drawString("Novo jogo", 295, 234);
		else
			g.drawString("Continuar", 295, 234);

		g.drawString("Carregar jogo", 275, 274);
		g.drawString("Sair", 335, 314);

		if (options[currentOption] == "novo jogo") {
			g.drawString(">", 255, 234);
		} else if (options[currentOption] == "carregar jogo") {
			g.drawString(">", 235, 274);
		} else if (options[currentOption] == "sair") {
			g.drawString(">", 295, 314);
		}
	}
}
