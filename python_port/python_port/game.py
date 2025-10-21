"""Loop principal da versão Python do jogo."""
from __future__ import annotations

import random
from dataclasses import dataclass
from typing import List

import pygame

from . import settings
from .entities import Player, create_enemy, create_pickup, Enemy, Projectile, Pickup
from .ui import FloatingMessage, draw_hud
from .world import World


@dataclass
class QuestTracker:
    title: str
    target: int
    progress: int = 0

    def increment(self) -> None:
        self.progress += 1

    @property
    def completed(self) -> bool:
        return self.progress >= self.target

    def render_text(self) -> str:
        return f"{self.title}: {self.progress}/{self.target}"


class Game:
    def __init__(self, *, difficulty: settings.Difficulty = settings.Difficulty.NORMAL, start_level: int = 1):
        pygame.init()
        pygame.mixer.quit()  # Evita erros em ambientes sem áudio.
        self.clock = pygame.time.Clock()
        self.screen = pygame.display.set_mode((settings.DISPLAY_WIDTH, settings.DISPLAY_HEIGHT))
        pygame.display.set_caption("First Game in Python")
        self.canvas = pygame.Surface((settings.INTERNAL_WIDTH, settings.INTERNAL_HEIGHT))
        self.difficulty = difficulty
        self.state = settings.GameState.MENU
        self.level = start_level
        self.score = 0
        self.combo_multiplier = 1
        self.combo_timer = 0.0
        self.messages: List[FloatingMessage] = []
        self.quest: QuestTracker | None = None
        self.teleport_ready = False
        self.font_large = pygame.font.Font(None, 36)
        self.font_small = pygame.font.Font(None, 24)

        self.world: World | None = None
        self.player: Player | None = None
        self.enemies: List[Enemy] = []
        self.pickups: List[Pickup] = []
        self.projectiles: List[Projectile] = []

    def load_level(self, level: int) -> None:
        self.world = World.from_level(level)
        self.player = Player(self.world.player_spawn[0], self.world.player_spawn[1], self.difficulty)
        self.player.sync_rect()
        self.enemies = []
        self.pickups = []
        self.projectiles = []
        quest_targets = 0
        for spawn in self.world.spawns:
            x, y = spawn.position
            if spawn.kind == "enemy":
                self.spawn_enemy(x, y, spawn.variant or "scout")
            else:
                pickup = create_pickup(spawn.kind, x, y)
                self.pickups.append(pickup)
                if spawn.kind in {"quest_item", "quest_beacon", "quest_npc", "engineer", "researcher", "data_core"}:
                    quest_targets += 1
        if quest_targets:
            self.quest = QuestTracker("Missão", quest_targets)
        else:
            self.quest = None
        self.teleport_ready = False
        self.combo_multiplier = 1
        self.combo_timer = 0
        self.score = 0
        self.messages.clear()

    def spawn_enemy(self, x: int, y: int, variant: str) -> None:
        enemy = create_enemy(variant, x, y, self.difficulty)
        enemy.sync_rect()
        self.enemies.append(enemy)

    def add_message(self, text: str, position: tuple[int, int]) -> None:
        message = FloatingMessage(text=text, position=pygame.Vector2(position))
        self.messages.append(message)

    def on_enemy_killed(self, enemy: Enemy) -> None:
        base_score = 100 + enemy.variant.health * 0.5
        self.score += int(base_score * self.combo_multiplier)
        self.combo_multiplier += settings.COMBO_INCREMENT
        self.combo_timer = settings.COMBO_TIMEOUT
        if random.random() < 0.25:
            pickup_kind = random.choice(["lifepack", "ammo", "energy", "shield"])
            pickup = create_pickup(pickup_kind, enemy.rect.x, enemy.rect.y)
            self.pickups.append(pickup)
        self.add_message("Combo +", enemy.rect.midtop)

    def increment_quest_progress(self, label: str) -> None:
        if self.quest:
            self.quest.increment()
            if self.quest.completed:
                self.add_message("Objetivo concluído!", (settings.INTERNAL_WIDTH // 2 - 40, 40))

    def activate_teleport(self) -> None:
        self.teleport_ready = True
        self.add_message("Teleporte pronto", (settings.INTERNAL_WIDTH // 2 - 40, 60))

    @property
    def quest_status(self) -> str:
        if self.teleport_ready:
            return "Ative o teleporte!"
        if not self.quest:
            return "Explore a estação"
        return self.quest.render_text()

    def update_messages(self, dt: float) -> None:
        for message in list(self.messages):
            message.update(dt)
            if not message.alive:
                self.messages.remove(message)

    def start_game(self) -> None:
        self.load_level(self.level)
        self.state = settings.GameState.PLAYING

    def reset_to_menu(self) -> None:
        self.state = settings.GameState.MENU

    def handle_menu_event(self, event: pygame.event.Event) -> None:
        if event.type == pygame.KEYDOWN:
            if event.key == pygame.K_RETURN:
                self.start_game()
            elif event.key == pygame.K_ESCAPE:
                pygame.event.post(pygame.event.Event(pygame.QUIT))
            elif event.key == pygame.K_TAB:
                options = list(settings.Difficulty)
                idx = options.index(self.difficulty)
                self.difficulty = options[(idx + 1) % len(options)]

    def handle_playing_event(self, event: pygame.event.Event) -> None:
        if not self.player:
            return
        if event.type == pygame.KEYDOWN:
            if event.key == pygame.K_ESCAPE:
                self.reset_to_menu()
            elif event.key == pygame.K_x:
                self.player.try_shoot(self)
        elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
            self.player.try_shoot(self)

    def update_gameplay(self, dt: float) -> None:
        if not self.player or not self.world:
            return
        keys = pygame.key.get_pressed()
        self.player.handle_input(keys, dt, self.world)
        self.player.update(self, dt)

        mouse_buttons = pygame.mouse.get_pressed()
        if keys[pygame.K_x] or mouse_buttons[0]:
            self.player.try_shoot(self)

        for enemy in list(self.enemies):
            enemy.update(self, dt)
            if enemy.dead:
                self.enemies.remove(enemy)

        for pickup in list(self.pickups):
            pickup.update(self, dt)
            if pickup.dead:
                self.pickups.remove(pickup)

        for projectile in list(self.projectiles):
            projectile.update(self, dt)
            if projectile.dead:
                self.projectiles.remove(projectile)

        if self.player.dead:
            self.state = settings.GameState.GAME_OVER
            self.add_message("Você foi derrotado", (settings.INTERNAL_WIDTH // 2 - 60, settings.INTERNAL_HEIGHT // 2))

        if self.combo_multiplier > 1:
            self.combo_timer -= dt
            if self.combo_timer <= 0:
                self.combo_multiplier = 1

        self.update_messages(dt)

        if self.quest and self.quest.completed and not self.teleport_ready:
            self.activate_teleport()

    def draw_world(self) -> None:
        if not self.world or not self.player:
            return
        self.canvas.fill(settings.COLOR_BLACK)
        for tile in self.world.iter_tiles():
            pygame.draw.rect(self.canvas, tile.color, tile.rect)

        for pickup in self.pickups:
            pickup.draw(self.canvas)
        for enemy in self.enemies:
            enemy.draw(self.canvas)
        for projectile in self.projectiles:
            projectile.draw(self.canvas)
        self.player.draw(self.canvas)

        draw_hud(self.canvas, self)

    def draw_menu(self) -> None:
        self.canvas.fill((12, 12, 18))
        title = self.font_large.render("First Game in Python", True, settings.COLOR_WHITE)
        prompt = self.font_small.render("Pressione Enter para iniciar", True, (200, 200, 200))
        difficulty = self.font_small.render(f"Dificuldade: {self.difficulty.name.title()}", True, (180, 200, 255))
        self.canvas.blit(title, (40, 60))
        self.canvas.blit(prompt, (40, 120))
        self.canvas.blit(difficulty, (40, 160))

    def draw_game_over(self) -> None:
        self.canvas.fill((30, 0, 0))
        title = self.font_large.render("Game Over", True, (255, 120, 120))
        prompt = self.font_small.render("Pressione Enter para tentar novamente", True, settings.COLOR_WHITE)
        score = self.font_small.render(f"Pontuação final: {int(self.score)}", True, settings.COLOR_WHITE)
        self.canvas.blit(title, (80, 60))
        self.canvas.blit(prompt, (20, 120))
        self.canvas.blit(score, (20, 160))

    def draw(self) -> None:
        if self.state == settings.GameState.MENU:
            self.draw_menu()
        elif self.state == settings.GameState.PLAYING:
            self.draw_world()
        elif self.state == settings.GameState.GAME_OVER:
            self.draw_game_over()
        scaled = pygame.transform.scale(self.canvas, (settings.DISPLAY_WIDTH, settings.DISPLAY_HEIGHT))
        self.screen.blit(scaled, (0, 0))
        pygame.display.flip()

    def update(self, dt: float) -> None:
        if self.state == settings.GameState.PLAYING:
            self.update_gameplay(dt)
        elif self.state == settings.GameState.GAME_OVER:
            self.update_messages(dt)

    def handle_event(self, event: pygame.event.Event) -> None:
        if self.state == settings.GameState.MENU:
            self.handle_menu_event(event)
        elif self.state == settings.GameState.PLAYING:
            self.handle_playing_event(event)
        elif self.state == settings.GameState.GAME_OVER:
            if event.type == pygame.KEYDOWN and event.key == pygame.K_RETURN:
                self.start_game()
            elif event.type == pygame.KEYDOWN and event.key == pygame.K_ESCAPE:
                self.reset_to_menu()

    def run(self) -> None:
        running = True
        while running:
            dt = self.clock.tick(settings.FPS) / 1000.0
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    running = False
                else:
                    self.handle_event(event)
            self.update(dt)
            self.draw()
        pygame.quit()
