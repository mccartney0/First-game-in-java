import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.MusicManager;
import com.traduvertgames.main.WeatherAudioManager;
import com.traduvertgames.world.OpenWorldManager;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.World;
import com.traduvertgames.world.WorldWeatherManager;

/**
 * Playthrough headless controlado do Mundo Aberto. Percorre setores de todas
 * as regiões e amostra dia, crepúsculo, noite e aurora com o áudio adaptativo
 * e os modificadores de um Guardião climático.
 */
public final class OpenWorldClimatePlaythrough {
    private static final int[] CLOCK_SAMPLES = { 180, 1570, 2140, 3350 };

    private OpenWorldClimatePlaythrough() {
    }

    public static void main(String[] args) throws Exception {
        Game.resetAllForTest();
        new Game();
        Game.getInstance().loadOpenWorld(1);
        Game.gameState = "NORMAL";

        System.out.println("PLAYTHROUGH CLIMÁTICO — MUNDO ABERTO");
        System.out.println("Passo | Setor | Região | Período | Clima | Gigante (vel/dano/cd/regen) | Trilha");
        int step = 1;
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            int[] tile = findTile(region);
            for (int clock : CLOCK_SAMPLES) {
                restoreClock(clock);
                int pixelX = tile[0] * World.TILE_SIZE + World.TILE_SIZE / 2;
                int pixelY = tile[1] * World.TILE_SIZE + World.TILE_SIZE / 2;
                Game.player.setX(pixelX);
                Game.player.setY(pixelY);
                OpenWorldManager.updatePlayerPosition(pixelX, pixelY);
                WorldWeatherManager.update();
                WeatherAudioManager.update();
                WorldWeatherManager.GiantModifier modifier = WorldWeatherManager.getGiantModifier(pixelX, pixelY);
                Enemy guardian = new Enemy(pixelX + 12, pixelY + 12, 16, 16, Entity.ENEMY_EN, Enemy.Variant.GUARDIAN);
                if (!guardian.isClimateGiant()) {
                    throw new IllegalStateException("Guardião não recebeu classificação de gigante climático.");
                }
                System.out.printf("%02d | %s | %s | %s | %s | %.2f/%.2f/%.2f/%.3f | %s%n",
                        step++, OpenWorldManager.getActiveSectorCode(), region.name(), modifier.getTime().name(),
                        modifier.getWeather().name(), modifier.getSpeedMultiplier(), modifier.getDamageMultiplier(),
                        modifier.getCooldownMultiplier(), modifier.getRegenPerTick(), MusicManager.getCurrentZone());
            }
        }
        System.out.println("RESULTADO: rota concluída com setores, períodos, clima, gigantes e áudio adaptativo ativos.");
        Game.resetAllForTest();
    }

    private static void restoreClock(int timeTicks) {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("active", true);
        state.put("seed", OpenWorldManager.getSeed());
        state.put("timeTicks", timeTicks);
        WorldWeatherManager.deserialize(state);
    }

    private static int[] findTile(RpgWorldManager.RegionType expected) {
        for (int y = 0; y < OpenWorldManager.getWorldHeight(); y += 8) {
            for (int x = 0; x < OpenWorldManager.getWorldWidth(); x += 8) {
                if (RpgWorldManager.regionForTile(x, y) == expected) {
                    return new int[] { x + 2, y + 2 };
                }
            }
        }
        throw new IllegalStateException("Região ausente no mundo: " + expected);
    }
}
