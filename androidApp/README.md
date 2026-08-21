# First Game in Java — Android

Este módulo contém a primeira camada jogável Android do projeto. A versão desktop continua preservada na raiz; o aplicativo Android usa uma `SurfaceView` nativa para executar um loop de jogo em paisagem, com renderização Canvas, inimigos Scout, projéteis, colisões, HUD e controles de toque.

## Build

Com o Android SDK configurado em `androidApp/local.properties`, execute:

```bash
./gradlew -p androidApp assembleDebug --no-daemon
```

O APK será gerado em `androidApp/app/build/outputs/apk/debug/app-debug.apk`.

## Controles

Use o círculo esquerdo para mover a nave. Toque e arraste no lado direito para mirar e mantenha o toque para disparar. Depois de perder a nave, toque na tela para reiniciar a partida.

A camada Android reutiliza os assets validados do Content Studio (`scout_ref.png` e `blaster_clean.png`) e não inclui `local.properties` no controle de versão.
