#!/usr/bin/env python3
import math
import os
import random
import struct
import wave

RATE = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "res", "sounds")


def clamp(value):
    return max(-1.0, min(1.0, value))


def write_wav(name, samples):
    path = os.path.join(OUT, name + ".wav")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "w") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        frames = b"".join(struct.pack("<h", int(clamp(sample) * 32767)) for sample in samples)
        wav.writeframes(frames)
    print(path)


def tone(duration, start_hz, end_hz=None, volume=0.35, decay=4.0, harmonics=1):
    count = int(duration * RATE)
    end_hz = start_hz if end_hz is None else end_hz
    samples = []
    phase = 0.0
    for i in range(count):
        t = i / max(1, count - 1)
        freq = start_hz + (end_hz - start_hz) * t
        phase += 2 * math.pi * freq / RATE
        env = volume * math.exp(-decay * t)
        value = math.sin(phase)
        for harmonic in range(2, harmonics + 1):
            value += math.sin(phase * harmonic) / (harmonic * 1.8)
        samples.append(value * env)
    return samples


def noise(duration, volume=0.25, decay=6.0, seed=1):
    count = int(duration * RATE)
    rng = random.Random(seed)
    return [rng.uniform(-1.0, 1.0) * volume * math.exp(-decay * (i / max(1, count - 1))) for i in range(count)]


def mix(*tracks):
    length = max(len(track) for track in tracks)
    result = [0.0] * length
    for track in tracks:
        for i, value in enumerate(track):
            result[i] += value
    return result


def silence(duration):
    return [0.0] * int(duration * RATE)


def sequence(*tracks):
    result = []
    for track in tracks:
        result.extend(track)
    return result


def make_assets():
    write_wav("magic_cast", mix(tone(0.62, 180, 980, 0.28, 2.2, 3), tone(0.62, 360, 1960, 0.12, 3.5, 2), noise(0.18, 0.07, 9, 20)))
    write_wav("magic_hit", mix(tone(0.34, 720, 120, 0.38, 5.0, 2), noise(0.22, 0.18, 8, 21)))
    write_wav("experience_orb", sequence(tone(0.11, 660, 880, 0.22, 7), silence(0.015), tone(0.14, 880, 1320, 0.18, 8)))
    write_wav("survival_phase", sequence(tone(0.16, 330, 440, 0.24, 5), silence(0.04), tone(0.16, 440, 660, 0.24, 5), silence(0.04), tone(0.28, 660, 990, 0.3, 4)))
    write_wav("dungeon_open", mix(tone(1.0, 70, 38, 0.30, 1.5, 3), tone(0.55, 240, 65, 0.22, 3, 2), noise(0.9, 0.11, 2.4, 22)))
    write_wav("weapon_ion", tone(0.13, 1100, 1700, 0.26, 8, 2))
    write_wav("weapon_scatter", mix(noise(0.20, 0.24, 10, 23), tone(0.18, 120, 70, 0.24, 9)))
    write_wav("weapon_fusion", mix(tone(0.28, 420, 920, 0.28, 4, 2), tone(0.28, 840, 1680, 0.12, 5, 2)))
    write_wav("weapon_void", mix(tone(0.54, 150, 42, 0.34, 3.2, 3), noise(0.42, 0.13, 4, 24)))
    # Camadas ambientais do Mundo Aberto. São efeitos curtos disparados em
    # intervalos variáveis pelo WeatherAudioManager, portanto podem ser
    # substituídos sem exigir clips longos em loop.
    write_wav("weather_breeze", mix(noise(0.90, 0.10, 0.8, 40), tone(0.90, 140, 96, 0.04, 1.1, 2)))
    write_wav("weather_rain", mix(noise(0.78, 0.18, 0.55, 41), tone(0.30, 1800, 920, 0.03, 6.0, 1)))
    write_wav("weather_fog", mix(tone(1.05, 210, 175, 0.08, 1.0, 2), noise(1.05, 0.045, 0.6, 42)))
    write_wav("weather_ash", mix(noise(0.84, 0.14, 1.3, 43), tone(0.52, 630, 410, 0.06, 2.4, 2)))
    write_wav("weather_storm", mix(noise(0.92, 0.18, 0.9, 44), tone(0.65, 68, 42, 0.15, 1.2, 2), tone(0.18, 950, 1650, 0.08, 8.0, 1)))
    write_wav("weather_acid", mix(noise(0.72, 0.10, 1.7, 45), tone(0.70, 390, 260, 0.09, 1.7, 3)))
    write_wav("weather_crystal", sequence(tone(0.18, 1220, 1580, 0.12, 7.0, 1), silence(0.05), tone(0.30, 980, 670, 0.09, 4.0, 2)))
    write_wav("weather_void", mix(tone(1.18, 82, 38, 0.16, 0.9, 3), noise(1.18, 0.055, 1.0, 46)))
    write_wav("time_dusk", sequence(tone(0.22, 440, 392, 0.14, 5.0, 2), silence(0.06), tone(0.34, 330, 247, 0.14, 3.8, 2)))
    write_wav("time_night", mix(tone(0.72, 196, 110, 0.16, 1.8, 2), tone(0.22, 740, 620, 0.08, 6.0, 1)))
    write_wav("time_dawn", sequence(tone(0.18, 330, 494, 0.12, 7.0, 2), silence(0.04), tone(0.38, 494, 740, 0.15, 4.6, 2)))


if __name__ == "__main__":
    make_assets()
