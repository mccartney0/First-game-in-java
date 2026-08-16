import json, os

repo = '/home/ubuntu/First-game-in-java'
path = os.path.join(repo, 'saves.json')

# BOOMERANG_ARCANO = ordinal 10 no enum WeaponType (11 armas)
slot = {
    'id': 1,
    'versao': 2,
    'timestamp': 1755356000000,
    'vida': 100,
    'maxLife': 100,
    'mana': 300,
    'maxMana': 500,
    'arma': 1.0,
    'armaAtual': 8,
    'armasDesbloqueadas': 1023,
    'escudo': 0,
    'inimigosMortos': 5,
    'level': 1,
    'levelPlus': 0,
    'pontuacao': 250,
    'recorde': 250,
    'melhorCombo': 3,
    'melhorComboSessao': 3,
    'experiencia': 120,
    'campaign': [],
    'progress': {},
    'objectiveState': {}
}

data = {'versao': 2, 'slots': [slot, {}, {}]}
json.dump(data, open(path, 'w'), indent=2)
print('save gravado como array: slot 1 com armaAtual=10 (BOOMERANG_ARCANO, ordinal 8)')
