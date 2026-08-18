#!/usr/bin/env python3
"""Gera saves.json com o slot 1 apontando para a fase 9 (Vale dos Refugiados),
para o playthrough de validação pular direto para o novo conteúdo.

Formato: os saves reais do jogo são FLAT — as chaves ficam direto no objeto do
slot (não dentro de uma chave "session"), pois saveCurrentGame escreve
diretamente no slot. O getSession() do jogo aceita os dois formatos.
"""
import json

slot = {
    "id": 1,
    "name": "Slot 1",
    "level": 9,
    "levelPlus": 0,
    "vida": 120.0,
    "mana": 200.0,
    "escudo": 50.0,
    "arma": 0.0,
    "armaAtual": 0,
    "armasDesbloqueadas": 1,
    "pontuacao": 0,
    "recorde": 0,
    "melhorCombo": 0,
    "melhorComboSessao": 0,
    "inimigosMortos": 0,
    "inimigosMortosSet": [],
    "survivalRecord": 0,
}

save = {
    "activeSlot": 1,
    "slots": [slot],
}

with open("saves.json", "w") as f:
    json.dump(save, f, indent=2)
print("saves.json criado — slot 1 na fase 9")
