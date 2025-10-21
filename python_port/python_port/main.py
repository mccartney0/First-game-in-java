"""Ponto de entrada do jogo em Python."""
from __future__ import annotations

import argparse

from . import settings
from .game import Game


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="First Game in Python")
    parser.add_argument("--difficulty", choices=[d.name.lower() for d in settings.Difficulty], default=settings.Difficulty.NORMAL.name.lower(), help="Define a dificuldade inicial")
    parser.add_argument("--level", type=int, default=1, help="Mapa inicial (ex.: 1 gera level1.png)")
    return parser


def main(argv: list[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    difficulty = next(d for d in settings.Difficulty if d.name.lower() == args.difficulty)
    game = Game(difficulty=difficulty, start_level=args.level)
    game.run()


if __name__ == "__main__":  # pragma: no cover
    main()
