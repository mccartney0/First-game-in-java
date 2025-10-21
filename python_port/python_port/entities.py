"""Entidades principais utilizadas no jogo em Python."""
from __future__ import annotations

import random
from dataclasses import dataclass
from typing import Callable, Dict, TYPE_CHECKING

import pygame

from . import settings

if TYPE_CHECKING:
    from .game import Game

def make_surface(color: tuple[int, int, int], size: tuple[int, int] = (settings.TILE_SIZE, settings.TILE_SIZE)) -> pygame.Surface:
    surface = pygame.Surface(size)
    surface.fill(color)
    return surface.convert_alpha()


class Entity:
    def __init__(self, x: int, y: int, width: int = settings.TILE_SIZE, height: int = settings.TILE_SIZE, *, color: tuple[int, int, int] = settings.COLOR_FLOOR):
        self.rect = pygame.Rect(x, y, width, height)
        self.position = pygame.Vector2(float(x), float(y))
        self.velocity = pygame.Vector2(0, 0)
        self.sprite = make_surface(color, (width, height))
        self.dead = False
        self.z_index = 0

    def update(self, game: "Game", dt: float) -> None:  # pragma: no cover - interface
        pass

    def draw(self, surface: pygame.Surface) -> None:
        surface.blit(self.sprite, self.rect)

    def sync_rect(self) -> None:
        self.rect.topleft = (int(self.position.x), int(self.position.y))


class LivingEntity(Entity):
    def __init__(self, x: int, y: int, width: int, height: int, *, color: tuple[int, int, int], max_health: float):
        super().__init__(x, y, width, height, color=color)
        self.max_health = max_health
        self.health = max_health
        self.shield = 0.0
        self.max_shield = 0.0
        self.invulnerability_timer = 0.0

    def take_damage(self, amount: float, *, ignore_shield: bool = False) -> None:
        if self.invulnerability_timer > 0:
            return
        remaining = amount
        if self.shield > 0 and not ignore_shield:
            absorbed = min(self.shield, remaining)
            self.shield -= absorbed
            remaining -= absorbed
        if remaining > 0:
            self.health -= remaining
            if self.health <= 0:
                self.dead = True
        self.invulnerability_timer = 0.2

    def heal(self, amount: float) -> None:
        self.health = min(self.max_health, self.health + amount)

    def restore_shield(self, amount: float) -> None:
        self.shield = min(self.max_shield, self.shield + amount)

    def update(self, game: "Game", dt: float) -> None:
        if self.invulnerability_timer > 0:
            self.invulnerability_timer = max(0.0, self.invulnerability_timer - dt)


class Projectile(Entity):
    def __init__(self, x: int, y: int, direction: pygame.Vector2, speed: float, damage: float, owner: str, *, color: tuple[int, int, int]):
        super().__init__(x, y, 6, 6, color=color)
        direction = direction if direction.length_squared() > 0 else pygame.Vector2(1, 0)
        direction = direction.normalize()
        self.velocity = direction * speed
        self.damage = damage
        self.owner = owner

    def update(self, game: "Game", dt: float) -> None:
        self.position += self.velocity * dt
        self.sync_rect()
        if (self.rect.right < 0 or self.rect.left > settings.INTERNAL_WIDTH or self.rect.bottom < 0 or self.rect.top > settings.INTERNAL_HEIGHT):
            self.dead = True
            return
        if game.world.rect_collides(self.rect):
            self.dead = True
            return

        if self.owner == "player":
            for enemy in list(game.enemies):
                if self.rect.colliderect(enemy.rect):
                    enemy.take_damage(self.damage)
                    self.dead = True
                    if enemy.dead:
                        game.on_enemy_killed(enemy)
                    return
        else:
            player = game.player
            if self.rect.colliderect(player.rect):
                player.take_damage(self.damage)
                self.dead = True


class Player(LivingEntity):
    def __init__(self, x: int, y: int, difficulty: settings.Difficulty):
        stats = difficulty.player_stats
        super().__init__(x, y, settings.TILE_SIZE, settings.TILE_SIZE, color=settings.COLOR_PLAYER, max_health=stats["max_health"])
        self.max_shield = stats["max_shield"]
        self.shield = self.max_shield * 0.6
        self.max_energy = stats["max_energy"]
        self.energy = self.max_energy
        self.max_ammo = stats["max_ammo"]
        self.ammo = self.max_ammo // 2
        self.speed = 120
        self.sprint_speed = 180
        self.reload_rate = 30
        self.energy_regen = 20
        self.shoot_cooldown = 0.0
        self.combo_bonus = 0
        self.facing = pygame.Vector2(1, 0)

    def handle_input(self, keys: pygame.key.ScancodeWrapper, dt: float, world: WorldLike) -> None:
        direction = pygame.Vector2(0, 0)
        if keys[pygame.K_w] or keys[pygame.K_UP]:
            direction.y -= 1
        if keys[pygame.K_s] or keys[pygame.K_DOWN]:
            direction.y += 1
        if keys[pygame.K_a] or keys[pygame.K_LEFT]:
            direction.x -= 1
        if keys[pygame.K_d] or keys[pygame.K_RIGHT]:
            direction.x += 1
        if direction.length_squared() > 0:
            direction = direction.normalize()
            self.facing = direction
        speed = self.speed
        if keys[pygame.K_LSHIFT] or keys[pygame.K_RSHIFT]:
            speed = self.sprint_speed
        dx = direction.x * speed * dt
        dy = direction.y * speed * dt
        self._move(dx, dy, world)

    def _move(self, dx: float, dy: float, world: WorldLike) -> None:
        self.position.x += dx
        self.sync_rect()
        if world.rect_collides(self.rect):
            self.position.x -= dx
            self.sync_rect()
        self.position.y += dy
        self.sync_rect()
        if world.rect_collides(self.rect):
            self.position.y -= dy
            self.sync_rect()

    def update(self, game: "Game", dt: float) -> None:
        super().update(game, dt)
        self.energy = min(self.max_energy, self.energy + self.energy_regen * dt)
        self.ammo = min(self.max_ammo, self.ammo + self.reload_rate * dt * 0.1)
        if self.shoot_cooldown > 0:
            self.shoot_cooldown = max(0.0, self.shoot_cooldown - dt)

    def try_shoot(self, game: "Game") -> None:
        if self.shoot_cooldown > 0:
            return
        if self.energy < 5 or self.ammo <= 0:
            return
        self.energy -= 5
        self.ammo -= 1
        self.shoot_cooldown = 0.25
        direction = self.facing.normalize() if self.facing.length_squared() > 0 else pygame.Vector2(1, 0)
        origin = pygame.Vector2(self.rect.center)
        projectile = Projectile(int(origin.x), int(origin.y), direction, speed=360, damage=18, owner="player", color=(255, 240, 170))
        game.projectiles.append(projectile)


@dataclass
class EnemyVariant:
    name: str
    speed: float
    health: float
    attack_cooldown: float
    projectile_speed: float
    projectile_damage: float
    behavior: str = "chaser"


ENEMY_VARIANTS: Dict[str, EnemyVariant] = {
    "scout": EnemyVariant("Scout", speed=80, health=60, attack_cooldown=1.4, projectile_speed=220, projectile_damage=8, behavior="chaser"),
    "teleporter": EnemyVariant("Teleporter", speed=70, health=65, attack_cooldown=2.5, projectile_speed=260, projectile_damage=10, behavior="teleport"),
    "artillery": EnemyVariant("Artillery", speed=40, health=80, attack_cooldown=1.8, projectile_speed=320, projectile_damage=12, behavior="ranged"),
    "warden": EnemyVariant("Warden", speed=55, health=140, attack_cooldown=2.2, projectile_speed=260, projectile_damage=14, behavior="tank"),
    "sentinel": EnemyVariant("Sentinel", speed=65, health=120, attack_cooldown=2.0, projectile_speed=280, projectile_damage=12, behavior="support"),
    "ravager": EnemyVariant("Ravager", speed=110, health=90, attack_cooldown=1.1, projectile_speed=300, projectile_damage=10, behavior="charger"),
    "warbringer": EnemyVariant("Warbringer", speed=75, health=200, attack_cooldown=1.0, projectile_speed=360, projectile_damage=16, behavior="boss"),
    "overseer": EnemyVariant("Overseer", speed=60, health=220, attack_cooldown=1.6, projectile_speed=320, projectile_damage=14, behavior="summoner"),
}


class Enemy(LivingEntity):
    def __init__(self, x: int, y: int, variant: EnemyVariant, *, difficulty: settings.Difficulty):
        adjusted_health = variant.health * difficulty.enemy_spawn_multiplier
        super().__init__(x, y, settings.TILE_SIZE, settings.TILE_SIZE, color=settings.COLOR_ENEMY, max_health=adjusted_health)
        self.variant = variant
        self.attack_timer = random.uniform(0.0, variant.attack_cooldown)
        self.teleport_timer = 0.0
        self.z_index = 1

    def update(self, game: "Game", dt: float) -> None:
        super().update(game, dt)
        player_vector = pygame.Vector2(game.player.rect.center)
        pos = pygame.Vector2(self.rect.center)
        direction = player_vector - pos
        distance = direction.length()
        if distance == 0:
            direction = pygame.Vector2(1, 0)
            distance = 0.1
        else:
            direction = direction.normalize()

        speed = self.variant.speed
        if self.variant.behavior == "charger" and distance < 160:
            speed *= 1.5
        elif self.variant.behavior == "tank":
            speed *= 0.7

        movement = direction * speed * dt
        self.position += movement
        self.sync_rect()
        if game.world.rect_collides(self.rect):
            self.position -= pygame.Vector2(movement.x, 0)
            self.sync_rect()
        if game.world.rect_collides(self.rect):
            self.position -= pygame.Vector2(0, movement.y)
            self.sync_rect()

        self.attack_timer -= dt
        if self.attack_timer <= 0:
            self.attack_timer = self.variant.attack_cooldown
            self.perform_attack(game, direction)

        if self.variant.behavior == "teleport":
            self.teleport_timer -= dt
            if self.teleport_timer <= 0 and distance < 120:
                self.teleport_behind_player(game)
                self.teleport_timer = 3.5

    def perform_attack(self, game: "Game", direction: pygame.Vector2) -> None:
        if self.variant.behavior in {"ranged", "boss", "support"}:
            self.fire_projectile(game, direction)
        elif self.variant.behavior == "summoner":
            self.fire_projectile(game, direction)
            if random.random() < 0.35:
                game.spawn_enemy(self.rect.x, self.rect.y, "scout")
        else:
            if self.rect.colliderect(game.player.rect.inflate(12, 12)):
                game.player.take_damage(self.variant.projectile_damage * 0.6)

    def fire_projectile(self, game: "Game", direction: pygame.Vector2) -> None:
        projectile = Projectile(self.rect.centerx, self.rect.centery, direction, self.variant.projectile_speed, self.variant.projectile_damage, owner="enemy", color=(255, 120, 120))
        game.projectiles.append(projectile)

    def teleport_behind_player(self, game: "Game") -> None:
        player = game.player
        offset = pygame.Vector2(player.facing)
        if offset.length_squared() == 0:
            offset = pygame.Vector2(1, 0)
        teleport_position = pygame.Vector2(player.rect.center) - offset * 40
        self.position.x = max(0, min(settings.INTERNAL_WIDTH - self.rect.width, teleport_position.x))
        self.position.y = max(0, min(settings.INTERNAL_HEIGHT - self.rect.height, teleport_position.y))
        self.sync_rect()


class Pickup(Entity):
    def __init__(self, x: int, y: int, effect: Callable[["Game", Player], None], color: tuple[int, int, int]):
        super().__init__(x, y, settings.TILE_SIZE, settings.TILE_SIZE, color=color)
        self.effect = effect
        self.z_index = -1

    def update(self, game: "Game", dt: float) -> None:
        if self.rect.colliderect(game.player.rect):
            self.effect(game, game.player)
            self.dead = True


# Tipagem auxiliar para evitar import circular.
class WorldLike:
    def rect_collides(self, rect: tuple[int, int, int, int]) -> bool:  # pragma: no cover - protocolo
        ...


def create_pickup(kind: str, x: int, y: int) -> Pickup:
    def heal(game: "Game", player: Player, amount: float) -> None:
        player.heal(amount)
        game.add_message("+Vida", player.rect.midtop)

    def refill_shield(game: "Game", player: Player, amount: float) -> None:
        player.restore_shield(amount)
        game.add_message("+Escudo", player.rect.midtop)

    def refill_energy(game: "Game", player: Player, amount: float) -> None:
        player.energy = min(player.max_energy, player.energy + amount)
        game.add_message("+Energia", player.rect.midtop)

    def refill_ammo(game: "Game", player: Player, amount: float) -> None:
        player.ammo = min(player.max_ammo, player.ammo + amount)
        game.add_message("+Munição", player.rect.midtop)

    def overclock(game: "Game", player: Player) -> None:
        refill_energy(game, player, 60)
        refill_ammo(game, player, 30)
        game.add_message("Overclock!", player.rect.midtop)

    def notify(label: str) -> Callable[["Game", Player], None]:
        def _inner(g: "Game", player: Player) -> None:
            g.increment_quest_progress(label)
            g.add_message(label, player.rect.midtop)
        return _inner

    effects: Dict[str, Callable[["Game", Player], None]] = {
        "lifepack": lambda g, p: heal(g, p, 30),
        "nanomedkit": lambda g, p: heal(g, p, 60),
        "shield": lambda g, p: refill_shield(g, p, 40),
        "energy": lambda g, p: refill_energy(g, p, 40),
        "overclock": overclock,
        "ammo": lambda g, p: refill_ammo(g, p, 50),
        "weapon": lambda g, p: refill_ammo(g, p, 70),
        "quest_item": notify("Artefato"),
        "quest_beacon": notify("Farol"),
        "quest_npc": notify("Sobrevivente"),
        "engineer": notify("Engenheiro"),
        "researcher": notify("Pesquisador"),
        "data_core": notify("Protocolo"),
        "teleport_pad": lambda g, p: g.activate_teleport(),
    }

    colors: Dict[str, tuple[int, int, int]] = {
        "lifepack": settings.COLOR_PICKUP,
        "nanomedkit": (255, 100, 100),
        "shield": settings.COLOR_SHIELD,
        "energy": settings.COLOR_ENERGY,
        "overclock": (0, 229, 255),
        "ammo": (255, 220, 64),
        "weapon": (255, 106, 0),
        "quest_item": (255, 193, 7),
        "quest_beacon": (76, 175, 80),
        "quest_npc": (121, 85, 72),
        "engineer": (255, 183, 77),
        "researcher": (126, 87, 194),
        "data_core": (0, 172, 193),
        "teleport_pad": (103, 58, 183),
    }

    if kind not in effects:
        raise ValueError(f"Pickup desconhecido: {kind}")

    effect = effects[kind]
    return Pickup(x, y, effect, colors[kind])


def create_enemy(kind: str, x: int, y: int, difficulty: settings.Difficulty) -> Enemy:
    variant = ENEMY_VARIANTS.get(kind, ENEMY_VARIANTS["scout"])
    return Enemy(x, y, variant, difficulty=difficulty)
