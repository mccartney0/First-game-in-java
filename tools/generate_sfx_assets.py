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


if __name__ == "__main__":
    make_assets()
