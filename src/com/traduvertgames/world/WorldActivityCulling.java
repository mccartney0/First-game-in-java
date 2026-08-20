package com.traduvertgames.world;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;

/**
 * Janela de atividade dos mapas RPG extensos.
 *
 * A câmera continua livre para atravessar o mundo, mas somente inimigos próximos
 * recebem atualização de IA e desenho. A margem de atualização cobre mais de uma
 * tela para que uma perseguição comece antes de o inimigo entrar no enquadramento,
 * enquanto entidades que carregam lógica crítica (jogador, portais e NPCs) seguem
 * ativas sempre.
 */
public final class WorldActivityCulling {

    private static final int UPDATE_MARGIN_X = Game.WIDTH * 2;
    private static final int UPDATE_MARGIN_Y = Game.HEIGHT * 2;
    private static final int RENDER_MARGIN = 24;

    private WorldActivityCulling() {
    }

    public static boolean isEnabled() {
        return Game.isRegionalAdventureMode() || Game.isOpenWorldMode();
    }

    public static boolean shouldUpdate(Entity entity) {
        if (!isEnabled() || !(entity instanceof Enemy)) {
            return true;
        }
        return intersects(entity, Camera.x - UPDATE_MARGIN_X, Camera.y - UPDATE_MARGIN_Y,
                Game.WIDTH + UPDATE_MARGIN_X * 2, Game.HEIGHT + UPDATE_MARGIN_Y * 2);
    }

    public static boolean shouldRender(Entity entity) {
        if (!isEnabled() || !(entity instanceof Enemy)) {
            return true;
        }
        return intersects(entity, Camera.x - RENDER_MARGIN, Camera.y - RENDER_MARGIN,
                Game.WIDTH + RENDER_MARGIN * 2, Game.HEIGHT + RENDER_MARGIN * 2);
    }

    private static boolean intersects(Entity entity, int x, int y, int width, int height) {
        int entityLeft = entity.getX();
        int entityTop = entity.getY();
        int entityRight = entityLeft + entity.getWidth();
        int entityBottom = entityTop + entity.getHeight();
        return entityRight >= x && entityLeft <= x + width
                && entityBottom >= y && entityTop <= y + height;
    }
}
