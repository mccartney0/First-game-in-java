package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Player;

public class UI {

	public void render(Graphics g) {
		//UI Life
		g.setColor(Color.red);
		g.fillRect(30, 4, 70, 8);
		g.setColor(Color.green);
		g.fillRect(30, 4, (int)((Player.life / Player.maxLife) * 70), 8);
		
		
		// UI Bullet
		g.setColor(Color.gray);
		g.fillRect(162, 4, 70, 8);
		g.setColor(Color.blue);
		g.fillRect(162, 4, (int)((Player.mana / Player.maxMana) * 70), 8);
//		g.setColor(Color.white);
//		g.setFont(new Font("arial", Font.BOLD,8));
//		g.drawString((int)Player.mana+ "/"+(int)Player.maxMana,190, 12);
	}

}
