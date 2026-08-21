# Asset Coach — guia operacional de assets e trilhas RPG

O **Asset Coach** é a aba do Content Studio destinada a preparar uma cópia de trabalho de um PNG para o runtime Android do First Game RPG. Ele evita alterações destrutivas: o arquivo fornecido pelo artista permanece intacto, enquanto a cópia exportada é escrita no pacote gerado do projeto com um manifesto compatível.

> Use o Asset Coach para sprites individuais de protagonista, NPCs, armas e projéteis. Para atlas, referências conceituais ou importações em lote, utilize o pipeline de importação descrito em `docs/USER_ASSET_PIPELINE.md`.

## Fluxo recomendado

O fluxo profissional é curto e repetível: criar uma silhueta legível, diagnosticar, normalizar uma cópia, revisar no jogo, validar o catálogo e então gerar o APK. A validação deve ocorrer antes de cada publicação, pois ela verifica transparência, escala, atlas, metadados, referências e empacotamento em runtime.

| Etapa | Ação | Resultado esperado |
|---|---|---|
| 1. Criar | Desenhe o asset em PNG, com sujeito separado do fundo. | A função do objeto é reconhecível no tamanho do jogo. |
| 2. Diagnosticar | Abra **Content Studio → Asset Coach → Selecionar PNG… → Diagnosticar**. | Canvas, alfa, margem e silhueta são reportados. |
| 3. Declarar | Escolha o tipo runtime, ID, nome, escala, dano e cooldown. | Arte e gameplay compartilham o mesmo contrato. |
| 4. Normalizar | Acione **Normalizar e exportar para RPG**. | Uma cópia 32×32 e seu manifesto são gerados. |
| 5. Validar | Acione **Validar conteúdo** e corrija qualquer erro. | `validateContent` retorna zero erros. |
| 6. Testar | Monte o APK e teste no mapa, em tamanho real. | Leitura, origem de disparo e animações ficam consistentes. |

## Especificação de PNG individual

O runtime aceita PNG com canal alfa. O Asset Coach aceita arquivos maiores, mas exporta uma cópia em **32 × 32 pixels**, preservando pixel art com interpolação de vizinho mais próximo. A silhueta é recortada com dois pixels de respiro e é centralizada dentro de uma área útil de 28 × 28; isso reduz cortes nas bordas durante a animação.

| Item | Regra de produção | Verificação prática |
|---|---|---|
| Silhueta | A função deve ser compreensível antes de adicionar textura. | Visualize em 32 × 32 e no mapa. |
| Fundo | Use alfa real sempre que possível. | Não deixe cor sólida encostando na borda. |
| Contraste | Priorize rosto, arma e direção de ataque. | Compare contra o piso mais claro e mais escuro. |
| Pivô | Mantenha os pés e a mão/saída do disparo previsíveis. | Teste caminhada e tiro sem tremor visual. |
| Pixel art | Escale com nearest-neighbor; evite suavização. | Linhas permanecem nítidas depois da exportação. |

Quando um PNG não possui transparência, o Coach remove somente uma cor quase uniforme conectada à borda. Ele não remove elementos internos cercados pela silhueta. Revise a prévia antes de exportar quando o fundo estiver próximo da paleta da personagem.

## Como criar e usar um sprite RPG

Abra o Content Studio com o comando abaixo. Na aba **Asset Coach**, escolha o PNG e leia o diagnóstico antes de mudar os campos. O **tipo runtime** define a categoria do manifesto; o **ID de saída** vira o nome do PNG; e os campos de gameplay formam o contrato usado pelo jogo e pelo validador.

```bash
./gradlew runContentStudio --no-daemon
```

Um exemplo seguro para substituir o protagonista é selecionar `HERO`, usar o ID `hero`, informar o nome de exibição `Protagonista` e manter o dano em `0` quando a arte for apenas visual. Para uma arma ou projétil, preencha dano, cooldown e mantenha a origem de disparo alinhada à mão, cano ou foco mágico no editor apropriado.

O Asset Coach exporta para `res/assets/generated/rpg_sprites/<id>.png` e cria o arquivo `<id>.json` no mesmo diretório. O módulo Android sincroniza esse pacote no `preBuild`; não é necessário copiar manualmente os arquivos para `androidApp/app/src/main/assets/rpg/`.

## Upload em lote e overrides de artista

O importador em lote continua disponível quando houver vários arquivos ou quando o artista seguir as convenções de override. Copie os originais para `res/assets/incoming/user_uploads/` e rode a tarefa de importação. O processo preserva os originais, gera cópias de trabalho em `res/assets/generated/` e cria `user_asset_manifest.json`.

| Asset | Nome de upload reconhecido | Uso no APK |
|---|---|---|
| Protagonista | `rpg_hero.png` | `GameView` e inventário |
| Ava | `rpg_npc_commandant.png` | NPC comandante |
| Orin | `rpg_npc_healer.png` | NPC curandeiro |
| Ilyra | `rpg_npc_cartographer.png` | NPC cartógrafa |
| Armas | `rpg_weapon_*.png` | Armas do herói |
| Projéteis | `rpg_projectile_*.png` | Disparos e magia |

```bash
./gradlew importUserAssets --no-daemon
```

Para gerar a base visual completa, incluindo os frames direcionais de caminhada e ataque, use a tarefa abaixo. Os arquivos seguem a convenção `<entidade>_<ação>_<direção>_<frame>.png`, por exemplo `hero_attack_down_1.png`.

```bash
./gradlew generateDefaultRpgVisualPack --no-daemon
```

## Animação, atlas e metadados

Cada personagem usa três poses de caminhada e três de ataque nas quatro direções. A caminhada deve comunicar contato, passagem e empurrão; o ataque deve comunicar preparação, impacto e retorno. Não altere a posição dos pés a cada frame e mantenha a origem de tiro fixa o suficiente para que projéteis não pareçam saltar.

O Asset Coach trata sprites individuais. Quando o asset for um atlas, mantenha a grade regular, nenhuma célula vazia e o mesmo pivô em cada célula. O manifesto de um sprite RPG declara categoria, variante, arquivo, dimensão, escala, dano, cooldown, origem do disparo, carregamento em runtime e a exigência de alfa. Para frames, ele também declara estado, direção, índice e quantidade de frames.

## Trilhas, efeitos e importação de áudio

Os efeitos de passos, ataques e diálogos ficam em `androidApp/app/src/main/res/raw/`. A direção musical usa loops de exploração regional, combate leve/médio/crítico, chefes, vitória e leitmotifs de NPCs. Para adicionar uma faixa, use um arquivo de áudio de loop com nome minúsculo, números e sublinhados, por exemplo `rpg_music_nova_regiao.wav`; depois, registre o recurso no diretor `RpgAudio` e documente a prioridade na Wiki Viva.

| Prioridade | Estado musical | Regra de retorno |
|---|---|---|
| 1 | Vinheta de vitória de chefe | Ao terminar, resolve novamente ameaça e região. |
| 2 | Leitmotif de diálogo | Ao fechar a conversa, devolve a prioridade ao campo. |
| 3 | Chefe | Prevalece enquanto o chefe estiver ativo. |
| 4 | Combate crítico, médio ou leve | Derivado de ameaça, proximidade e risco. |
| 5 | Exploração regional | Base da Clareira, Águas do Norte ou Fortaleza. |

## Validar e publicar

Execute as verificações abaixo a partir da raiz. A primeira valida o núcleo e os contratos de conteúdo; a segunda monta o APK que receberá os sprites e áudios sincronizados.

```bash
./gradlew test validateContent --no-daemon
./gradlew -p androidApp assembleDebug --no-daemon
```

O artefato de instalação é `androidApp/app/build/outputs/apk/debug/app-debug.apk`. Antes de publicar uma mudança, atualize a Wiki Viva e o Códice, substitua o link e o checksum exibidos no portal pelo APK recém-gerado e salve uma versão do portal.

## Diagnóstico rápido

| Sintoma | Causa provável | Ação recomendada |
|---|---|---|
| Fundo ainda aparece | A cor do fundo é semelhante à silhueta ou não está conectada à borda. | Exporte o PNG com alfa verdadeiro e rode o diagnóstico novamente. |
| Sprite parece pequeno | A área visível original ocupa pouca parte do canvas. | Recorte antes de enviar ou deixe o Coach centralizar na área útil. |
| Arte fica borrada | Houve interpolação suave antes da importação. | Reexporte pixel art com nearest-neighbor. |
| `validateContent` falha | Manifesto, nome, alfa ou referência perdeu o contrato. | Leia o erro, corrija no Coach e valide outra vez. |
| APK não mostra a arte nova | O pacote não foi recompilado depois da exportação. | Rode `assembleDebug` e instale o APK recém-gerado. |

O arquivo-fonte deve permanecer fora de `res/assets/generated/` quando ele for uma peça de trabalho do artista. Essa separação permite ajustar o PNG sem perder o original e torna cada exportação reproduzível pelo Content Studio.
