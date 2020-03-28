package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JOptionPane;

public class Menu {

	public String[] options = { "novo jogo", "carregar jogo", "sair" };

	public int currentOption = 0;
	public int maxOption = options.length - 1;

	public boolean up, down, enter;

	public boolean pause = false;

	public void update() {
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
			//Inserindo música
//			Sound.music.loop();
			enter = false;
			if (options[currentOption] == "novo jogo" || options[currentOption] == "continuar") {
				Game.gameState = "NORMAL";
				pause = false;
			}else if(options[currentOption] == "sair") {
//				JOptionPane.showConfirmDialog(null, "Deseja realmente sair?", "Fechar o jogo", currentOption);
				System.exit(1);
			}
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
