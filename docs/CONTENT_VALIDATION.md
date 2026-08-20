# Validação de conteúdo

O Content Studio agora valida o pacote de conteúdo antes da exportação para o runtime. A validação é executada pelo motor Java `com.traduvertgames.tools.ContentValidator` e pode ser chamada pela tarefa Gradle ou pela aba **Validação** da interface gráfica.

## Execução

Na raiz do projeto, execute:

```bash
./gradlew validateContent
```

O comando grava o relatório em:

```text
res/assets/generated/content_validation_report.json
```

A tarefa retorna código de saída diferente de zero quando existe pelo menos um erro. Avisos não bloqueiam a execução.

## Testes disponíveis

| Teste | O que verifica | Exemplo de erro |
|---|---|---|
| **Arquivo** | Arquivos referenciados pelo `AssetCatalog` e pelo manifesto, além de PNGs estáticos soltos em diretórios de runtime. | `weapon_plasma.png` foi referenciado ou existe, mas não está disponível no pacote/catalogado. |
| **Transparência** | Canal alfa e existência de pixels transparentes em sprites que exigem fundo transparente. | Fundo sólido encontrado em uma arma, inimigo, companheiro ou efeito. |
| **Escala** | Imagens grandes usadas diretamente sem indicação de contrato de normalização do runtime. | Sprite de 1920 px sem uso explícito por `drawWeaponIcon` ou `normalizeEnemySprite`. |
| **Atlas** | Existência do atlas, quantidade esperada de células e células vazias ou ilegíveis. | Atlas de companheiros com três células esperadas, mas duas encontradas. |
| **Metadados** | Campos de combate em `WeaponType.java` e campos dos manifestos JSON gerados. | Inimigo sem `baseLife`/`baseDamage` ou arma RPG sem `damageBonus`/`rarity`. |
| **Referência** | Assets importados que não são referências e foram marcados como não utilizados no runtime. | Efeito de tiro importado, mas nenhum sistema de arma o utiliza. Assets explicitamente classificados como `reference` são ignorados. |
| **Runtime** | Arquivos declarados pelo catálogo/manifesto, imagens PNG carregáveis e saídas registradas no manifesto. | Arquivo declarado no manifesto não está presente ou não pode ser lido como PNG. |

## Interface gráfica

A aba **Validação** do Content Studio contém o botão **Validar conteúdo**. O relatório textual é exibido na própria tela, também é registrado no painel de atividade e é salvo automaticamente no manifesto JSON de validação.

## Testes automatizados

`ContentValidatorTest` cria um projeto temporário com falhas intencionais para confirmar que todas as sete categorias são detectadas. A suíte completa continua sendo executada com:

```bash
./gradlew test
```

A validação é deliberadamente independente das regras de gameplay. Ela informa que um conteúdo está incompleto ou inconsistente, mas não altera dano, colisão, escala, IA ou renderização por conta própria.
