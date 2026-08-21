# First Game in Java — Android

Este módulo contém a camada jogável RPG Android do projeto. A versão desktop continua preservada na raiz; o aplicativo Android usa uma `SurfaceView` nativa para executar um loop de exploração em paisagem, com mapa de tiles, personagem, monstros, chefes, drops, combate mágico, cadeias de missões, NPCs dialogáveis, inventário, equipamentos, atributos, HUD e controles de toque.

## Build

Com o Android SDK configurado em `androidApp/local.properties`, execute:

```bash
./gradlew -p androidApp assembleDebug --no-daemon
```

O APK será gerado em `androidApp/app/build/outputs/apk/debug/app-debug.apk`.

## Controles

Use o círculo esquerdo para mover o personagem. Toque e arraste no lado direito para mirar e mantenha o toque para lançar magia. O botão **BOLSA** abre o inventário; toque em um item para equipar armas, armaduras e acessórios ou usar poções. A área de **AÇÃO** conversa com NPCs, abre o baú, recupera a relíquia de missão e interage com a ponte. Os botões **SALVAR** e **CARREGAR** registram ou restauram o progresso manualmente; o jogo também salva automaticamente quando o aplicativo é pausado. Depois de ser derrotado, toque na tela para retomar o último save ou iniciar outra aventura.

## Missões e NPCs

**Ava, Comandante** conduz a cadeia principal: iniciar a expedição, derrotar três criaturas da Bruma, encontrar o Fragmento da Aurora, derrotar o Necromante do Véu e, por fim, o Titã da Bruma. **Orin, Curandeiro** explica o uso de poções e equipamentos. **Ilyra, Cartógrafa** aponta a rota da relíquia. A missão ativa e o respectivo progresso ficam visíveis no HUD.

## Salvamento de progresso

O savegame é local ao dispositivo e usa `SharedPreferences`, sem permissões de armazenamento ou conexão de rede. Ele preserva posição, vida, nível, XP, ouro, inimigos derrotados, estado do baú, etapas de missão, relíquia, chefes derrotados, inventário e equipamentos selecionados. Desinstalar o aplicativo remove o save local, salvo quando o Android restaurar o backup do aplicativo.

A camada Android reutiliza os atlases enviados (`base_out_atlas.png` e `terrain_atlas.png`) e os assets validados do Content Studio. O arquivo `local.properties` não é incluído no controle de versão.
