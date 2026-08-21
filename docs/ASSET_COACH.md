# Asset Coach — guia operacional de assets e trilhas RPG

O **Asset Coach** é o conjunto de abas do Content Studio que prepara cópias de trabalho de PNGs para o runtime Android do First Game RPG. O fluxo preserva a arte enviada pelo criador: o original nunca é alterado; apenas a cópia exportada e o manifesto compatível são escritos no pacote gerado do projeto.

> Use o Coach para protagonista, NPCs, armas e projéteis. O fluxo também acompanha a cobertura de caminhada e ataque antes do build do APK.

## Fluxo recomendado

O ciclo profissional é repetível: desenhe uma silhueta legível, diagnostique, compare, normalize uma cópia, confira cobertura de animações, valide o catálogo e só então gere o APK.

| Etapa | Ação | Resultado esperado |
|---|---|---|
| 1. Criar | Desenhe o asset em PNG, com o sujeito separado do fundo. | A função do objeto é reconhecível no tamanho do jogo. |
| 2. Diagnosticar | Abra **Content Studio → Asset Coach → Selecionar PNG… → Diagnosticar**. | Canvas, alfa, margem e silhueta são reportados. |
| 3. Declarar | Escolha tipo runtime, ID, nome, escala, dano e cooldown. | Arte e gameplay compartilham o mesmo contrato. |
| 4. Comparar | Consulte a prévia **Antes/Depois** antes de salvar a cópia. | O tratamento de fundo, recorte e escala ficam visíveis. |
| 5. Normalizar | Exporte um item ou uma fila pelo Asset Coach. | Uma cópia 32×32 e seu manifesto são gerados. |
| 6. Cobrir | Abra **Cobertura animações** e atualize o relatório. | Frames ausentes são identificados por ação e direção. |
| 7. Validar | Rode a validação de conteúdo. | `validateContent` retorna zero erros. |
| 8. Testar | Monte o APK e revise o asset no mapa. | Leitura, origem de disparo e animações ficam consistentes. |

## Especificação de PNG individual

O runtime aceita PNG com canal alfa. O Asset Coach aceita arquivos maiores, mas exporta uma cópia em **32 × 32 pixels**, preservando pixel art com interpolação de vizinho mais próximo. A silhueta é recortada com dois pixels de respiro e centralizada dentro de uma área útil de 28 × 28, reduzindo cortes nas bordas durante a animação.

| Item | Regra de produção | Verificação prática |
|---|---|---|
| Silhueta | A função deve ser compreensível antes de adicionar textura. | Visualize em 32 × 32 e no mapa. |
| Fundo | Use alfa real sempre que possível. | Não deixe cor sólida encostando na borda. |
| Contraste | Priorize rosto, arma e direção de ataque. | Compare contra o piso mais claro e mais escuro. |
| Pivô | Mantenha pés e mão/saída do disparo previsíveis. | Teste caminhada e tiro sem tremor visual. |
| Pixel art | Escale com nearest-neighbor; evite suavização. | Linhas permanecem nítidas depois da exportação. |

Quando um PNG não possui transparência, o Coach remove somente uma cor quase uniforme conectada à borda. Elementos internos cercados pela silhueta permanecem intactos. Revise sempre a prévia quando o fundo se aproxima da paleta do personagem.

## Como criar e usar um sprite RPG

Abra o Content Studio com o comando abaixo. Na aba **Asset Coach**, escolha o PNG e leia o diagnóstico antes de mudar os campos. O **tipo runtime** define a categoria do manifesto; o **ID de saída** vira o nome do PNG; e os campos de gameplay formam o contrato usado pelo jogo e pelo validador.

```bash
./gradlew runContentStudio --no-daemon
```

Para substituir o protagonista, selecione `HERO`, use o ID `hero`, informe o nome de exibição `Protagonista` e mantenha o dano em `0` quando a arte for somente visual. Para armas ou projéteis, preencha dano, cooldown e mantenha a origem de disparo alinhada à mão, cano ou foco mágico.

O Asset Coach exporta para `res/assets/generated/rpg_sprites/<id>.png` e cria `<id>.json` no mesmo diretório. O módulo Android sincroniza esse pacote no `preBuild`; não copie arquivos manualmente para `androidApp/app/src/main/assets/rpg/`.

## Importação em lote, comparação e desfazer

Para uma coleção de PNGs, abra **Content Studio → Lote & comparar** e use **Selecionar PNGs…**. A fila aceita vários arquivos na mesma sessão e produz um relatório individual: um arquivo inválido não impede a normalização dos demais. A primeira seleção aparece nos painéis **Antes — fonte original** e **Depois — prévia normalizada**; essa comparação não grava nada até que você escolha **Normalizar lote e exportar**.

> O lote prepara cópias de trabalho, não edita referências enviadas por artistas. Trate erros de entrada como diagnósticos para corrigir na origem, não como um motivo para substituir automaticamente a arte-fonte.

Após uma exportação, o botão **Desfazer última cópia** remove somente o PNG e o JSON mais recentes produzidos naquela sessão dentro de `res/assets/generated/rpg_sprites/`. O botão verifica o diretório de saída antes da remoção e nunca apaga a fonte escolhida, arquivos fora do pacote gerado ou cópias de sessões anteriores. Para repetir uma exportação com outro resultado, ajuste o asset-fonte, selecione-o novamente e rode a fila outra vez.

| Cenário | Ação no Asset Coach | Garantia |
|---|---|---|
| Um PNG isolado | Use a aba **Asset Coach**. | Diagnóstico e exportação com metadados explícitos. |
| Vários PNGs | Use **Lote & comparar**. | Falhas são isoladas por arquivo e a fila produz um resumo. |
| Dúvida sobre tratamento | Consulte **Antes/Depois** antes de exportar. | A prévia normalizada ainda não toca no disco. |
| Exportação indevida na sessão atual | Use **Desfazer última cópia**. | Somente a última cópia gerada é apagada; a fonte permanece intacta. |
| Override legado de artista | Use `res/assets/incoming/user_uploads/` e `importUserAssets`. | O fluxo legado continua disponível para convenções de override. |

## Upload legado e overrides de artista

O importador de overrides permanece indicado quando o artista segue as convenções de nome existentes. Copie os originais para `res/assets/incoming/user_uploads/` e rode a tarefa abaixo. O processo preserva os originais, gera cópias em `res/assets/generated/` e cria `user_asset_manifest.json`.

```bash
./gradlew importUserAssets --no-daemon
```

| Asset | Nome de upload reconhecido | Uso no APK |
|---|---|---|
| Protagonista | `rpg_hero.png` | `GameView` e inventário |
| Ava | `rpg_npc_commandant.png` | NPC comandante |
| Orin | `rpg_npc_healer.png` | NPC curandeiro |
| Ilyra | `rpg_npc_cartographer.png` | NPC cartógrafa |
| Armas | `rpg_weapon_*.png` | Armas do herói |
| Projéteis | `rpg_projectile_*.png` | Disparos e magia |

Para gerar a base visual completa, incluindo frames direcionais, use:

```bash
./gradlew generateDefaultRpgVisualPack --no-daemon
```

## Cobertura de animações

Abra **Content Studio → Cobertura animações → Atualizar relatório** depois de exportar ou importar sprites. O painel mostra uma grade de **2 × 12** para cada entidade: caminhada e ataque nas quatro direções, com três frames por direção. Cada entidade completa possui **24 frames**; o pacote base acompanha herói, Ava, Orin e Ilyra, totalizando 96 slots previstos.

| Status do painel | Significado | Próxima ação |
|---|---|---|
| Célula presente | O frame foi encontrado pelo nome esperado. | Revise somente a qualidade dentro do mapa. |
| Célula ausente | Falta uma combinação de ação, direção ou índice. | Gere ou importe o frame correspondente. |
| ✓ Pronto para runtime | Os 24 slots da entidade estão disponíveis. | Rode `validateContent` e monte o APK. |
| • Gere frames ausentes | A entidade ainda depende de fallback parcial. | Complete a grade antes de polir novas variantes. |

Os nomes usam `<entidade>_<ação>_<direção>_<frame>.png`, por exemplo `hero_attack_down_1.png`. Há três poses de caminhada e três de ataque para direita, esquerda, cima e baixo. Mantenha pés, cabeça e origem do disparo previsíveis para evitar tremor visual.

## Atlas, metadados e trilhas

O Asset Coach trata sprites individuais. Para atlas, mantenha grade regular, nenhuma célula vazia e o mesmo pivô em cada célula. O manifesto declara categoria, variante, arquivo, dimensão, escala, dano, cooldown, origem do disparo, carregamento em runtime e exigência de alfa.

Os efeitos e loops ficam em `androidApp/app/src/main/res/raw/`. A direção musical usa exploração regional, combate leve/médio/crítico, chefes, vitória e leitmotifs de NPCs. Para adicionar uma faixa, use nome minúsculo com números e sublinhados — por exemplo `rpg_music_nova_regiao.wav` —, registre o recurso em `RpgAudio` e documente sua prioridade na Wiki Viva.

| Prioridade | Estado musical | Regra de retorno |
|---|---|---|
| 1 | Vinheta de vitória de chefe | Ao terminar, resolve novamente ameaça e região. |
| 2 | Leitmotif de diálogo | Ao fechar a conversa, devolve a prioridade ao campo. |
| 3 | Chefe | Prevalece enquanto o chefe estiver ativo. |
| 4 | Combate crítico, médio ou leve | Derivado de ameaça, proximidade e risco. |
| 5 | Exploração regional | Base da Clareira, Águas do Norte ou Fortaleza. |

## Validar e publicar

Execute as verificações abaixo a partir da raiz. A primeira compila o Content Studio, roda testes e valida os contratos de conteúdo; a segunda monta o APK com sprites e áudios sincronizados.

```bash
./gradlew test validateContent --no-daemon
./gradlew -p androidApp assembleDebug --no-daemon
```

O artefato de instalação é `androidApp/app/build/outputs/apk/debug/app-debug.apk`. Antes de publicar, atualize a Wiki Viva e o Códice, substitua link e checksum do portal pelo APK recém-gerado e salve uma versão do portal.

## Diagnóstico rápido

| Sintoma | Causa provável | Ação recomendada |
|---|---|---|
| Fundo ainda aparece | A cor do fundo é semelhante à silhueta ou não está conectada à borda. | Exporte com alfa verdadeiro e consulte Antes/Depois. |
| Sprite parece pequeno | A área visível ocupa pouca parte do canvas. | Recorte antes de enviar ou deixe o Coach centralizar na área útil. |
| Arte fica borrada | Houve interpolação suave antes da importação. | Reexporte pixel art com nearest-neighbor. |
| Lote terminou parcialmente | Há fonte ilegível ou diagnóstico inválido em parte da fila. | Leia o relatório por arquivo e reenvie somente os itens corrigidos. |
| Desfazer não removeu a fonte | Esse é o comportamento seguro esperado. | O botão remove apenas a última cópia de trabalho da sessão. |
| Cobertura incompleta | Falta frame de ação, direção ou índice. | Use o ID indicado pela grade e gere o frame ausente. |
| `validateContent` falha | Manifesto, nome, alfa ou referência perdeu o contrato. | Leia o erro, corrija no Coach e valide novamente. |
| APK não mostra a arte nova | O pacote não foi recompilado após a exportação. | Rode `assembleDebug` e instale o APK recém-gerado. |

Mantenha o arquivo-fonte fora de `res/assets/generated/` quando ele for uma peça de trabalho do artista. Essa separação permite corrigir PNGs sem perder o original e mantém cada exportação reproduzível pelo Content Studio.
