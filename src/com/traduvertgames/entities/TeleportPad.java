package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.entities.FloatingText;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.FloorTile;
import com.traduvertgames.world.Tile;
import com.traduvertgames.world.World;

/**
 * Teleportador em pares: cada pad teletransporta o jogador para o pad par do
 * mesmo mapa (pad i &harr; pad i+1). O cooldown é GLOBAL ao jogador: pisar em
 * qualquer pad bloqueia todos por {@link #PLAYER_TELEPORT_COOLDOWN_FRAMES}
 * frames, impedindo o loop infinito que ocorria quando o jogador aterrissava
 * em outro pad durante o efeito.
 */
public class TeleportPad extends Entity {

	private static final int PLAYER_TELEPORT_COOLDOWN_FRAMES = 45;
	private static final int FLASH_FRAMES = 20;

	/** Cooldown global do jogador: compartilhado por todos os pads do mapa. */
	private static int playerCooldown = 0;

	/** Pares formados na ordem em que os pads foram parseados do mapa. */
	private static final List<TeleportPad> pads = new ArrayList<TeleportPad>();

	private int flash = 0;
	private int pairIndex = -1; // índice do pad no par (0 ou 1)

	public TeleportPad(int x, int y) {
		super(x, y, 16, 16, null);
		setMask(0, 0, 16, 16);
		pads.add(this);
		this.pairIndex = pads.size() - 1;
	}

	/** Recalcula o emparelhamento após o parse completo do mapa. */
	public static void linkPairs() {
		for (int i = 0; i < pads.size(); i++) {
			pads.get(i).pairIndex = i;
		}
	}

	/** Libera os pads ao trocar de fase ou recarregar o jogo. */
	public static void reset() {
		pads.clear();
		playerCooldown = 0;
	}

	@Override
	public void update() {
		if (flash > 0) {
			flash--;
		}
		if (playerCooldown > 0) {
			playerCooldown--;
			return;
		}

		if (Entity.isColliding(this, Game.player)) {
			if (teleportPlayerToPairedPad()) {
				playerCooldown = PLAYER_TELEPORT_COOLDOWN_FRAMES;
				for (TeleportPad pad : pads) {
					pad.flash = FLASH_FRAMES;
				}
				FloatingText.show("Teleporte!", Game.player.getX(), Game.player.getY() - 8,
						new Color(171, 71, 188));
				SoundManager.play(SoundManager.Event.TELEPORT);
			}
		}
	}

	/**
	 * Teletransporta o jogador para o pad par do mapa. Se o pad não tem par
	 * (número ímpar de pads), o jogador é movido para um tile livre próximo ao
	 * centro em vez de ficar parado sobre o mesmo pad.
	 */
	private boolean teleportPlayerToPairedPad() {
		TeleportPad partner = null;
		if (pairIndex >= 0 && pairIndex % 2 == 0 && pairIndex + 1 < pads.size()) {
			partner = pads.get(pairIndex + 1);
		} else if (pairIndex > 0) {
			partner = pads.get(pairIndex - 1);
		}

		if (partner != null && partner != this) {
			int px = partner.getX();
			int py = partner.getY();
			if (collidesWithBlockingEntity(px, py)) {
				return moveToFreeTileNearCenter();
			}
			Game.player.setX(px);
			Game.player.setY(py);
			Game.player.updateCamera();
			return true;
		}

		return moveToFreeTileNearCenter();
	}

	/** Destino de reserva: tile livre mais próximo do centro do mapa. */
	private boolean moveToFreeTileNearCenter() {
		List<int[]> candidates = collectCentralFloorTiles();
		if (candidates.isEmpty()) {
			return false;
		}
		// Ordena pelo mais próximo do centro para um destino determinístico.
		int centerX = World.WIDTH / 2;
		int centerY = World.HEIGHT / 2;
		candidates.sort((a, b) -> {
			int da = Math.abs(a[0] - centerX) + Math.abs(a[1] - centerY);
			int db = Math.abs(b[0] - centerX) + Math.abs(b[1] - centerY);
			return Integer.compare(da, db);
		});
		for (int[] tilePos : candidates) {
			int px = tilePos[0] * World.TILE_SIZE;
			int py = tilePos[1] * World.TILE_SIZE;
			if (collidesWithBlockingEntity(px, py)) {
				continue;
			}
			Game.player.setX(px);
			Game.player.setY(py);
			Game.player.updateCamera();
			return true;
		}
		return false;
	}

	private List<int[]> collectCentralFloorTiles() {
		List<int[]> positions = new ArrayList<>();
		if (World.tiles == null || World.tiles.length == 0) {
			return positions;
		}

		int centerX = World.WIDTH / 2;
		int centerY = World.HEIGHT / 2;
		int radius = Math.min(5, Math.min(centerX, centerY));
		if (radius < 2) {
			radius = Math.max(1, radius);
		}

		int startX = Math.max(0, centerX - radius);
		int endX = Math.min(World.WIDTH - 1, centerX + radius);
		int startY = Math.max(0, centerY - radius);
		int endY = Math.min(World.HEIGHT - 1, centerY + radius);

		for (int x = startX; x <= endX; x++) {
			for (int y = startY; y <= endY; y++) {
				Tile tile = World.tiles[x + (y * World.WIDTH)];
				if (tile instanceof FloorTile) {
					positions.add(new int[] { x, y });
				}
			}
		}

		if (positions.isEmpty()) {
			positions.add(new int[] { centerX, centerY });
		}

		return positions;
	}

	private boolean collidesWithBlockingEntity(int px, int py) {
		Rectangle playerMask = new Rectangle(px + Game.player.maskx, py + Game.player.masky, Game.player.mwidth,
				Game.player.mheight);
		for (Entity entity : Game.entities) {
			if (entity == Game.player || entity == this) {
				continue;
			}
			if (entity.maskx == 0 && entity.masky == 0 && entity.mwidth == 0 && entity.mheight == 0) {
				continue;
			}
			Rectangle entityMask = new Rectangle(entity.getX() + entity.maskx,
					entity.getY() + entity.masky, entity.mwidth, entity.mheight);
			if (playerMask.intersects(entityMask)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void render(Graphics g) {
		int screenX = this.getX() - Camera.x;
		int screenY = this.getY() - Camera.y;

		if (playerCooldown > 0) {
			// Padrão: escurecido durante o cooldown global.
			g.setColor(new Color(60, 20, 90, 120));
			g.fillOval(screenX + 1, screenY + 1, 14, 14);
			return;
		}

		Color ring = flash > 0 ? new Color(255, 255, 255, 220) : new Color(255, 255, 255, 180);
		g.setColor(new Color(88, 28, 135, 160));
		g.fillOval(screenX + 1, screenY + 1, 14, 14);
		g.setColor(new Color(171, 71, 188, 200));
		g.fillOval(screenX + 4, screenY + 4, 8, 8);
		g.setColor(ring);
		g.drawOval(screenX + 2, screenY + 2, 12, 12);
	}
}
