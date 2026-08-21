#!/usr/bin/env python3
"""Gera efeitos WAV leves e determinísticos para a expedição RPG Android."""

from __future__ import annotations

import math
import random
import struct
import wave
from pathlib import Path

RATE = 22_050
DESTINATION = Path(__file__).resolve().parents[1] / "androidApp/app/src/main/res/raw"
RNG = random.Random(0xF1A57)


def envelope(t: float, duration: float, attack: float = 0.015, release: float = 0.07) -> float:
    if t < attack:
        return t / attack
    remaining = duration - t
    if remaining < release:
        return max(0.0, remaining / release)
    return 1.0


def clamp(sample: float) -> int:
    return max(-32767, min(32767, int(sample * 32767)))


def render(name: str, duration: float, generator) -> None:
    DESTINATION.mkdir(parents=True, exist_ok=True)
    total = int(RATE * duration)
    frames = bytearray()
    for index in range(total):
        t = index / RATE
        frames.extend(struct.pack("<h", clamp(generator(t, duration))))
    with wave.open(str(DESTINATION / f"{name}.wav"), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(frames)


def step_grass(t: float, duration: float) -> float:
    e = envelope(t, duration, 0.004, 0.075) * (1.0 - t / duration) ** 0.45
    noise = RNG.uniform(-1.0, 1.0)
    thump = math.sin(2 * math.pi * (105 - 55 * t / duration) * t)
    return e * (noise * 0.18 + thump * 0.13)


def magic_cast(t: float, duration: float) -> float:
    e = envelope(t, duration, 0.012, 0.095)
    phase = 260 * t + 0.5 * (1280 / duration) * t * t
    shimmer = math.sin(2 * math.pi * (920 * t + 150 * math.sin(t * 15)))
    return e * (math.sin(2 * math.pi * phase) * 0.34 + shimmer * 0.10)


def arcane_impact(t: float, duration: float) -> float:
    e = envelope(t, duration, 0.003, 0.10)
    phase = 880 * t - 0.5 * (620 / duration) * t * t
    crackle = RNG.uniform(-1.0, 1.0) * (1.0 - t / duration)
    return e * (math.sin(2 * math.pi * phase) * 0.30 + crackle * 0.16)


def dialogue_open(t: float, duration: float) -> float:
    e = envelope(t, duration, 0.01, 0.12)
    first = math.sin(2 * math.pi * 740 * t) * math.exp(-t * 10)
    second_t = max(0.0, t - 0.055)
    second = math.sin(2 * math.pi * 1110 * second_t) * math.exp(-second_t * 14)
    return e * (first * 0.26 + second * 0.22)


def ui_confirm(t: float, duration: float) -> float:
    e = envelope(t, duration, 0.006, 0.055)
    first = math.sin(2 * math.pi * 540 * t) * math.exp(-t * 15)
    second_t = max(0.0, t - 0.040)
    second = math.sin(2 * math.pi * 810 * second_t) * math.exp(-second_t * 19)
    return e * (first * 0.22 + second * 0.20)


def achievement(t: float, duration: float) -> float:
    e = envelope(t, duration, 0.01, 0.16)
    notes = ((0.00, 523.25), (0.09, 659.25), (0.18, 783.99))
    sample = 0.0
    for start, frequency in notes:
        local = t - start
        if local >= 0.0:
            sample += math.sin(2 * math.pi * frequency * local) * math.exp(-local * 9) * 0.17
    return e * sample


def main() -> None:
    render("rpg_step_grass", 0.115, step_grass)
    render("rpg_magic_cast", 0.260, magic_cast)
    render("rpg_arcane_impact", 0.185, arcane_impact)
    render("rpg_dialogue_open", 0.180, dialogue_open)
    render("rpg_ui_confirm", 0.125, ui_confirm)
    render("rpg_achievement", 0.410, achievement)
    print(f"Generated RPG SFX in {DESTINATION}")


if __name__ == "__main__":
    main()
