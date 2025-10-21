"""Rotinas de desenho da HUD."""
from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

import pygame

from . import settings

if TYPE_CHECKING:
    from .game import Game


@dataclass
class FloatingMessage:
    text: str
    position: pygame.Vector2
    timer: float = 0.0
    duration: float = 1.2

    def update(self, dt: float) -> None:
        self.timer += dt
        self.position.y -= 12 * dt

    @property
    def alive(self) -> bool:
        return self.timer < self.duration


def draw_meter(surface: pygame.Surface, label: str, value: float, maximum: float, position: tuple[int, int], size: tuple[int, int], color: tuple[int, int, int]) -> None:
    x, y = position
    width, height = size
    pygame.draw.rect(surface, (20, 20, 20), (x, y, width, height))
    if maximum > 0:
        fill_width = int((value / maximum) * width)
        pygame.draw.rect(surface, color, (x, y, fill_width, height))
    font = pygame.font.Font(None, 18)
    label_surface = font.render(f"{label}: {int(value)}/{int(maximum)}", True, settings.COLOR_WHITE)
    surface.blit(label_surface, (x + 4, y + 2))


def draw_hud(surface: pygame.Surface, game: "Game") -> None:
    player = game.player
    draw_meter(surface, "Vida", player.health, player.max_health, (8, 8), (120, 16), (220, 68, 68))
    draw_meter(surface, "Escudo", player.shield, player.max_shield, (8, 30), (120, 16), settings.COLOR_SHIELD)
    draw_meter(surface, "Energia", player.energy, player.max_energy, (8, 52), (120, 16), (64, 196, 255))
    draw_meter(surface, "Munição", player.ammo, player.max_ammo, (8, 74), (120, 16), (255, 214, 102))

    font = pygame.font.Font(None, 24)
    score_surface = font.render(f"Pontuação: {int(game.score)}", True, settings.COLOR_WHITE)
    combo_surface = font.render(f"Combo x{game.combo_multiplier}", True, (255, 200, 80))
    quest_surface = font.render(game.quest_status, True, (140, 200, 255))

    surface.blit(score_surface, (settings.INTERNAL_WIDTH - 180, 8))
    surface.blit(combo_surface, (settings.INTERNAL_WIDTH - 180, 32))
    surface.blit(quest_surface, (settings.INTERNAL_WIDTH - 220, settings.INTERNAL_HEIGHT - 28))

    for message in game.messages:
        alpha = max(0, 255 * (1 - message.timer / message.duration))
        message_surface = font.render(message.text, True, (255, 255, 255))
        message_surface.set_alpha(alpha)
        surface.blit(message_surface, (int(message.position.x), int(message.position.y)))
