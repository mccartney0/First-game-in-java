package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.MusicManager;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.main.WeatherAudioManager;
import com.traduvertgames.world.WorldWeatherManager;
import com.traduvertgames.world.WorldWeatherManager.TimeOfDay;
import com.traduvertgames.world.WorldWeatherManager.Weather;

class WeatherAudioManagerTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.getInstance().loadOpenWorld(1);
        Game.gameState = "NORMAL";
        WeatherAudioManager.reset();
    }

    @AfterEach
    void tearDown() {
        WeatherAudioManager.reset();
        MusicManager.unload();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void musicSelectionMapsEveryPeriodAndSevereClimate() {
        assertEquals(MusicManager.Zone.OPEN_WORLD_DAY,
                WeatherAudioManager.selectMusicZone(Weather.CLEAR, TimeOfDay.DAY));
        assertEquals(MusicManager.Zone.OPEN_WORLD_DUSK,
                WeatherAudioManager.selectMusicZone(Weather.RAIN, TimeOfDay.DUSK));
        assertEquals(MusicManager.Zone.OPEN_WORLD_NIGHT,
                WeatherAudioManager.selectMusicZone(Weather.FOG, TimeOfDay.NIGHT));
        assertEquals(MusicManager.Zone.OPEN_WORLD_DAWN,
                WeatherAudioManager.selectMusicZone(Weather.CRYSTAL_SQUALL, TimeOfDay.DAWN));
        assertEquals(MusicManager.Zone.OPEN_WORLD_STORM,
                WeatherAudioManager.selectMusicZone(Weather.ACID_DRIZZLE, TimeOfDay.DAY));
        assertEquals(MusicManager.Zone.OPEN_WORLD_ECLIPSE,
                WeatherAudioManager.selectMusicZone(Weather.VOID_ECLIPSE, TimeOfDay.DAY));
    }

    @Test
    void ambientEventsCoverTheCompleteClimateCatalog() {
        assertEquals(SoundManager.Event.WEATHER_BREEZE, WeatherAudioManager.ambientEventFor(Weather.CLEAR));
        assertEquals(SoundManager.Event.WEATHER_RAIN, WeatherAudioManager.ambientEventFor(Weather.RAIN));
        assertEquals(SoundManager.Event.WEATHER_FOG, WeatherAudioManager.ambientEventFor(Weather.FOG));
        assertEquals(SoundManager.Event.WEATHER_ASH, WeatherAudioManager.ambientEventFor(Weather.ASH));
        assertEquals(SoundManager.Event.WEATHER_STORM, WeatherAudioManager.ambientEventFor(Weather.ION_STORM));
        assertEquals(SoundManager.Event.WEATHER_ACID, WeatherAudioManager.ambientEventFor(Weather.ACID_DRIZZLE));
        assertEquals(SoundManager.Event.WEATHER_CRYSTAL, WeatherAudioManager.ambientEventFor(Weather.CRYSTAL_SQUALL));
        assertEquals(SoundManager.Event.WEATHER_VOID, WeatherAudioManager.ambientEventFor(Weather.VOID_ECLIPSE));
    }

    @Test
    void updateObservesClockAndRequestsTheSelectedTrack() {
        Map<String, Object> snapshot = new HashMap<String, Object>();
        snapshot.put("active", true);
        snapshot.put("seed", 3571L);
        snapshot.put("timeTicks", 1550);
        WorldWeatherManager.deserialize(snapshot);

        WeatherAudioManager.update();

        assertEquals(TimeOfDay.DUSK, WeatherAudioManager.getObservedTime());
        assertNotNull(WeatherAudioManager.getObservedWeather());
        assertEquals(WeatherAudioManager.selectMusicZone(WeatherAudioManager.getObservedWeather(), TimeOfDay.DUSK),
                MusicManager.getCurrentZone());
    }
}
