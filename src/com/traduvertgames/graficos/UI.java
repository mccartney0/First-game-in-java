package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;

public class UI {

	public void render(Graphics g) {
		// UI Life
		g.setColor(Color.red);
		g.fillRect(30, 4, 70, 8);
		g.setColor(Color.green);
		g.fillRect(30, 4, (int) ((Game.player.life / Game.player.maxLife) * 70), 8);

		// UI Mana / Bullet
		g.setColor(Color.gray);
		g.fillRect(162, 4, 70, 8);
		g.setColor(Color.blue);
		g.fillRect(162, 4, (int) ((Player.mana / Player.maxMana) * 70), 8);
//		g.setColor(Color.white);
//		g.setFont(new Font("arial", Font.BOLD,8));
//		g.drawString((int)Player.mana+ "/"+(int)Player.maxMana,190, 12);

		// UI Weapon durabilidade
		if (Player.weapon >= 120) {
			g.setColor(Color.gray);
			g.fillRect(30, 149, 70, 8);
			g.setColor(Color.cyan);
			g.fillRect(30, 149, (int) ((Player.weapon / Player.maxWeapon) * 70), 8);
		}
		if (Player.weapon < 50 && Player.weapon > 0) {
			g.setColor(Color.gray);
			g.fillRect(30, 149, 70, 8);
			g.setColor(Color.yellow);
			g.fillRect(30, 149, (int) ((Player.weapon / Player.maxWeapon) * 70), 8);
		}
	}

}
