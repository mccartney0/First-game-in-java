#!/usr/bin/env python3
"""Gera efeitos sonoros sintéticos 8-bit mono 8kHz para o jogo.

Cada evento ganha um arquivo WAV em bin/sounds/. Os sons são procedurais,
com envelopes e formas de onda simples — estilo "chip" coerente com o jogo.
"""
import math
import os
import struct

OUT = "/home/ubuntu/First-game-in-java/bin/sounds"
OUT_RES = "/home/ubuntu/First-game-in-java/res/sounds"
os.makedirs(OUT, exist_ok=True)
os.makedirs(OUT_RES, exist_ok=True)

SR = 8000


def sine(freq, t, phase=0.0):
    return math.sin(2 * math.pi * freq * t + phase)


def noise(t):
    # pseudo-ruído determinístico por frame
    import hashlib
    return (int(hashlib.md5(str(int(t * SR * 13)).encode()).hexdigest()[-4:], 16) / 65535.0 - 0.5) * 2


def gen(duration, fn):
    n = int(SR * duration)
    return bytes(
        clamp(int(fn(i / SR) * 90)) for i in range(n)
    )


def clamp(v):
    # unsigned 8-bit: -1.0..1.0 -> 0..255
    return max(0, min(255, int(v * 127) + 128))


def wave_header(data):
    n = len(data)
    return (
        b"RIFF" + struct.pack("<I", 36 + n)
        + b"WAVEfmt " + struct.pack("<IHHIIHH", 16, 1, 1, SR, SR, 1, 8)
        + b"data" + struct.pack("<I", n)
        + data
    )


def save(name, data):
    wav = wave_header(data)
    with open(os.path.join(OUT, name), "wb") as f:
        f.write(wav)
    with open(os.path.join(OUT_RES, name), "wb") as f:
        f.write(wav)


# Tiro: pulso quadrado descendente rápido (o pitch varia por arma na integração)
def shot(freq=660.0):
    def fn(t):
        env = math.exp(-t * 22.0)
        sq = 1.0 if sine(freq, t) > 0 else -1.0
        return sq * env + env * 0.15 * noise(t)
    return gen(0.10, fn)


# Laser agudo: senoide curta com pitch descendente
def laser(freq=1200.0):
    def fn(t):
        env = math.exp(-t * 30.0)
        f = freq * (1 - 0.4 * t / 0.10)
        return sine(f, t) * env
    return gen(0.10, fn)


# Acerto em inimigo: ruído filtrado com queda
def hit():
    def fn(t):
        env = math.exp(-t * 28.0)
        return noise(t) * env + sine(220.0, t) * env * 0.6
    return gen(0.10, fn)


# Dano no jogador: ruído grave + tom grave
def damage():
    def fn(t):
        env = math.exp(-t * 10.0)
        return noise(t) * env * 0.7 + sine(110.0, t) * env
    return gen(0.18, fn)


# Morte de inimigo / XP: senoide ascendente
def kill():
    def fn(t):
        f = 380.0 + 260.0 * t / 0.14
        return sine(f, t) * math.exp(-t * 14.0)
    return gen(0.14, fn)


# Teleporte: whoosh com pitch glide descendente
def teleport():
    def fn(t):
        env = math.sin(math.pi * min(t / 0.22, 1.0))
        f = 500.0 - 380.0 * t / 0.24
        return (sine(f, t) * 0.55 + noise(t) * 0.45) * env
    return gen(0.24, fn)


# Coleta de item/orb: "coin" clássico, duple senoide
def pickup():
    def fn(t):
        f = 784.0 if t < 0.07 else 1046.0
        env = math.exp(-t * 9.0)
        return sine(f, t) * env
    return gen(0.16, fn)


# Level-up: arpeggio curto
def levelup():
    def fn(t):
        notes = [523.0, 659.0, 784.0, 1046.0]
        per = 0.09
        i = int(t / per)
        f = notes[min(i, 3)]
        phase = (t % per) / per
        env = 1.0 - phase
        return sine(f, t) * env * math.exp(-t * 4.0)
    return gen(0.36, fn)


# Boss alertado: grave com vibrato
def boss_alert():
    def fn(t):
        env = 0.5 + 0.5 * math.exp(-t * 6.0)
        vib = 1 + 0.05 * sine(5.5, t)
        return sine(88.0 * vib, t) * env
    return gen(0.70, fn)


# Boss derrotado: vitória em 4 notas
def boss_defeat():
    def fn(t):
        notes = [392.0, 494.0, 587.0, 784.0]
        per = 0.11
        i = int(t / per)
        f = notes[min(i, 3)]
        env = 1.0 - ((t % per) / per)
        return sine(f, t) * env * math.exp(-t * 2.4)
    return gen(0.52, fn)


# Loja abre/fecha: blip suave
def blip():
    def fn(t):
        f = 880.0 if t < 0.04 else 660.0
        return sine(f, t) * math.exp(-t * 28.0)
    return gen(0.10, fn)


# Nova onda: tom duplo de alerta
def wave():
    def fn(t):
        f = 660.0 if t < 0.09 else 880.0
        env = math.exp(-t * 9.0)
        return sine(f, t) * env
    return gen(0.22, fn)


# Passo do tutorial: blip curto
def tutorial():
    def fn(t):
        return sine(988.0, t) * math.exp(-t * 40.0)
    return gen(0.06, fn)


# Fase concluída: fanfarra curta em 5 notas (celebração, mais longa que o level-up)
def level_complete():
    def fn(t):
        notes = [523.0, 659.0, 784.0, 1046.0, 784.0]
        per = 0.09
        i = int(t / per)
        f = notes[min(i, 4)]
        env = 1.0 - ((t % per) / per)
        return sine(f, t) * env * math.exp(-t * 2.2)
    return gen(0.45, fn)


# Vitória da campanha: fanfarra completa com acorde final sustentado
def victory():
    def fn(t):
        if t < 0.48:
            notes = [523.0, 659.0, 784.0, 1046.0]
            per = 0.12
            i = int(t / per)
            f = notes[min(i, 3)]
            phase = (t % per) / per
            env = 1.0 - phase
            return sine(f, t) * env
        # acorde final de sol maior (G4+B4+D5) com release lento
        a = (sine(392.0, t) + sine(494.0, t) * 0.8 + sine(587.0, t) * 0.8) / 2.4
        return a * math.exp(-(t - 0.48) * 1.8)
    return gen(1.2, fn)


# Diálogo iniciado: blip duplo grave

def dialogue_start():
    def fn(t):
        f = 440.0 if t < 0.05 else 523.0
        return sine(f, t) * math.exp(-t * 25.0)
    return gen(0.14, fn)


# Compra na loja: "coin" ascendente em 3 notas rápidas
def purchase():
    def fn(t):
        notes = [784.0, 988.0, 1175.0]
        per = 0.055
        i = int(t / per)
        f = notes[min(i, 2)]
        env = 1.0 - ((t % per) / per)
        return sine(f, t) * env * math.exp(-t * 4.0)
    return gen(0.17, fn)


# Seleção de menu: blip muito curto
def menu_select():
    def fn(t):
        return sine(1175.0, t) * math.exp(-t * 45.0)
    return gen(0.05, fn)


# Tutorial concluído: subida em 3 notas
def tutorial_done():
    def fn(t):
        notes = [659.0, 880.0, 1318.0]
        per = 0.08
        i = int(t / per)
        f = notes[min(i, 2)]
        env = 1.0 - ((t % per) / per)
        return sine(f, t) * env * math.exp(-t * 5.0)
    return gen(0.28, fn)


save("shot.wav", shot())
save("laser.wav", laser())
save("hit.wav", hit())
save("damage.wav", damage())
save("kill.wav", kill())
save("teleport.wav", teleport())
save("pickup.wav", pickup())
save("levelup.wav", levelup())
save("boss_alert.wav", boss_alert())
save("boss_defeat.wav", boss_defeat())
save("blip.wav", blip())
save("wave.wav", wave())
save("tutorial_step.wav", tutorial())
save("tutorial_done.wav", tutorial_done())
save("level_complete.wav", level_complete())
save("victory.wav", victory())
save("dialogue_start.wav", dialogue_start())
save("purchase.wav", purchase())
save("menu_select.wav", menu_select())

print("Sons gerados em", OUT, "->", sorted(os.listdir(OUT)))
