"""Configurações globais utilizadas pela porta em Python."""
from __future__ import annotations

from pathlib import Path

# A janela do pygame utiliza o mesmo tamanho interno da versão Java (384x216),
# ampliado por um fator de escala.
INTERNAL_WIDTH = 384
INTERNAL_HEIGHT = 216
SCALE = 3
DISPLAY_WIDTH = INTERNAL_WIDTH * SCALE
DISPLAY_HEIGHT = INTERNAL_HEIGHT * SCALE
FPS = 60

TILE_SIZE = 16

# Caminho base para localizar recursos reutilizados do projeto Java.
PROJECT_ROOT = Path(__file__).resolve().parents[2]
RESOURCES_PATH = PROJECT_ROOT / "res"
MAP_PATTERN = "level{level}.png"

# A lógica de combo utiliza janelas temporais em segundos.
COMBO_TIMEOUT = 4.0
COMBO_INCREMENT = 1

# Estados do jogo.
from enum import Enum, auto


class GameState(Enum):
    MENU = auto()
    PLAYING = auto()
    GAME_OVER = auto()


class Difficulty(Enum):
    EASY = "easy"
    NORMAL = "normal"
    HARD = "hard"

    @property
    def damage_multiplier(self) -> float:
        return {
            Difficulty.EASY: 0.75,
            Difficulty.NORMAL: 1.0,
            Difficulty.HARD: 1.35,
        }[self]

    @property
    def enemy_spawn_multiplier(self) -> float:
        return {
            Difficulty.EASY: 0.85,
            Difficulty.NORMAL: 1.0,
            Difficulty.HARD: 1.2,
        }[self]

    @property
    def player_stats(self) -> dict[str, float]:
        """Retorna ajustes aplicados ao jogador na inicialização."""
        base = {
            "max_health": 120,
            "max_shield": 80,
            "max_energy": 100,
            "max_ammo": 120,
        }
        if self is Difficulty.EASY:
            return {k: v * 1.2 for k, v in base.items()}
        if self is Difficulty.HARD:
            return {k: v * 0.85 for k, v in base.items()}
        return base


# Paleta utilizada para gerar superfícies coloridas rapidamente.
COLOR_BLACK = (0, 0, 0)
COLOR_WHITE = (255, 255, 255)
COLOR_FLOOR = (30, 30, 30)
COLOR_WALL = (180, 180, 180)
COLOR_PLAYER = (0, 150, 255)
COLOR_ENEMY = (255, 64, 64)
COLOR_PICKUP = (255, 215, 0)
COLOR_SHIELD = (142, 36, 170)
COLOR_ENERGY = (29, 233, 182)

