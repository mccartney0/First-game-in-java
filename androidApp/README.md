# First Game in Java — Android

Este módulo contém a camada jogável RPG Android do projeto. A versão desktop continua preservada na raiz; o aplicativo Android usa uma `SurfaceView` nativa para executar um loop de exploração em paisagem, com mapa de tiles, personagem, monstros, chefes, drops, combate mágico, missão, NPC, baú, inventário, equipamentos, atributos, HUD e controles de toque.

## Build

Com o Android SDK configurado em `androidApp/local.properties`, execute:

```bash
./gradlew -p androidApp assembleDebug --no-daemon
```

O APK será gerado em `androidApp/app/build/outputs/apk/debug/app-debug.apk`.

## Controles

Use o círculo esquerdo para mover o personagem. Toque e arraste no lado direito para mirar e mantenha o toque para lançar magia. O botão BOLSA abre o inventário; toque em um item para equipar armas, armaduras e acessórios ou usar poções. A área de AÇÃO conversa com Ava, abre o baú quando você estiver próximo e interage com a ponte. Derrotar inimigos gera XP, ouro e drops coletáveis. Depois de ser derrotado, toque na tela para reiniciar a aventura.

A camada Android reutiliza os atlases enviados (`base_out_atlas.png` e `terrain_atlas.png`) e os assets validados do Content Studio. O arquivo `local.properties` não é incluído no controle de versão.
