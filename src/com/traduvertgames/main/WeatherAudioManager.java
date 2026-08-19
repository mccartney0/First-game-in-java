package com.traduvertgames.main;

import com.traduvertgames.main.MusicManager.Zone;
import com.traduvertgames.world.WorldWeatherManager;
import com.traduvertgames.world.WorldWeatherManager.TimeOfDay;
import com.traduvertgames.world.WorldWeatherManager.Weather;

/**
 * Orquestra efeitos e trilhas do ambiente do Mundo Aberto. A camada usa os
 * mesmos eventos e arquivos substituíveis de {@link SoundManager}; a música
 * aproveita os crossfades já conduzidos pelo {@link MusicManager}.
 */
public final class WeatherAudioManager {

    private static Weather observedWeather;
    private static TimeOfDay observedTime;
    private static int ambientCooldown;

    private WeatherAudioManager() {
    }

    public static void update() {
        if (!WorldWeatherManager.isActive()) {
            reset();
            return;
        }
        Weather weather = WorldWeatherManager.getCurrentWeather();
        TimeOfDay time = WorldWeatherManager.getTimeOfDay();
        if (time != observedTime) {
            playTimeTransition(time);
            observedTime = time;
            ambientCooldown = 0;
        }
        if (weather != observedWeather) {
            observedWeather = weather;
            ambientCooldown = 0;
        }

        MusicManager.setZone(selectMusicZone(weather, time));
        if (ambientCooldown-- <= 0) {
            SoundManager.play(ambientEventFor(weather));
            ambientCooldown = ambientIntervalFor(weather);
        }
    }

    /** Visível para regressões e telemetria do playthrough. */
    public static Zone selectMusicZone(Weather weather, TimeOfDay time) {
        if (weather == Weather.VOID_ECLIPSE) {
            return Zone.OPEN_WORLD_ECLIPSE;
        }
        if (weather == Weather.ION_STORM || weather == Weather.ACID_DRIZZLE) {
            return Zone.OPEN_WORLD_STORM;
        }
        if (time == TimeOfDay.NIGHT) {
            return Zone.OPEN_WORLD_NIGHT;
        }
        if (time == TimeOfDay.DUSK) {
            return Zone.OPEN_WORLD_DUSK;
        }
        if (time == TimeOfDay.DAWN) {
            return Zone.OPEN_WORLD_DAWN;
        }
        return Zone.OPEN_WORLD_DAY;
    }

    public static SoundManager.Event ambientEventFor(Weather weather) {
        if (weather == Weather.RAIN) return SoundManager.Event.WEATHER_RAIN;
        if (weather == Weather.FOG) return SoundManager.Event.WEATHER_FOG;
        if (weather == Weather.ASH) return SoundManager.Event.WEATHER_ASH;
        if (weather == Weather.ION_STORM) return SoundManager.Event.WEATHER_STORM;
        if (weather == Weather.ACID_DRIZZLE) return SoundManager.Event.WEATHER_ACID;
        if (weather == Weather.CRYSTAL_SQUALL) return SoundManager.Event.WEATHER_CRYSTAL;
        if (weather == Weather.VOID_ECLIPSE) return SoundManager.Event.WEATHER_VOID;
        return SoundManager.Event.WEATHER_BREEZE;
    }

    private static void playTimeTransition(TimeOfDay time) {
        if (time == TimeOfDay.DUSK) SoundManager.play(SoundManager.Event.TIME_DUSK);
        else if (time == TimeOfDay.NIGHT) SoundManager.play(SoundManager.Event.TIME_NIGHT);
        else if (time == TimeOfDay.DAWN) SoundManager.play(SoundManager.Event.TIME_DAWN);
    }

    private static int ambientIntervalFor(Weather weather) {
        if (weather == Weather.ION_STORM || weather == Weather.ACID_DRIZZLE) return 130;
        if (weather == Weather.RAIN || weather == Weather.CRYSTAL_SQUALL) return 160;
        if (weather == Weather.VOID_ECLIPSE) return 210;
        if (weather == Weather.FOG) return 260;
        return 220;
    }

    public static Weather getObservedWeather() {
        return observedWeather;
    }

    public static TimeOfDay getObservedTime() {
        return observedTime;
    }

    public static void reset() {
        observedWeather = null;
        observedTime = null;
        ambientCooldown = 0;
    }
}
