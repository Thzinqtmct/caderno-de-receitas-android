# Caderno de Receitas 🍳📖

**Aluno:** Arthur Lopes — RA 842156

## Objetivo do aplicativo

O **Caderno de Receitas** ajuda a pessoa a descobrir receitas e a guardar a própria história com
elas. Você explora o catálogo público da [TheMealDB](https://www.themealdb.com) por categoria
(Sobremesa, Frango, Massas…) ou pelo nome, vê ingredientes, modo de preparo e vídeo, e registra no
seu caderno se **quer fazer** ou **já fez** a receita: quantas vezes, sua nota, observações (“usei
menos açúcar”) e até uma **foto do prato que você fez**. O caderno fica salvo no celular e continua
lá depois de fechar o app, inclusive sem internet.

O repositório tem **dois módulos de aplicativo**, um para cada etapa da disciplina:

| Módulo | Etapa | Tecnologia | O que faz |
|---|---|---|---|
| `parcial` | Parcial | Android Views (XML) + Intent | 8 receitas brasileiras simuladas → detalhes com rendimento ajustável, lista de ingredientes para marcar e favoritas |
| `compose` | Etapa 2 | Jetpack Compose | App completo: catálogo da API → detalhes → formulário do caderno → caderno salvo no aparelho |

---

## Como rodar

### Requisitos
- **Android Studio** recente (com suporte ao Android Gradle Plugin 9.4). O JDK 17+ já vem junto.
- Emulador ou celular com **Android 8.0 (API 26)** ou superior.
- **Internet** no emulador/celular para o catálogo do módulo `compose` (o módulo `parcial` funciona offline).

> **Não precisa de `.env`.** A TheMealDB usa na URL a chave pública de testes `1`, divulgada pela
> própria API para estudo. Mesmo assim, a chave **não fica escrita no código**: o Gradle lê
> `THEMEALDB_API_KEY` do `local.properties` (que não vai para o GitHub) e, se não encontrar, usa `1`.
> Quem tiver uma chave própria só precisa adicionar `THEMEALDB_API_KEY=...` ao `local.properties`.

### Pelo Android Studio
1. `File > Open…` e escolha a pasta do projeto.
2. Espere o *Gradle Sync* terminar (na primeira vez ele baixa as dependências e, se faltar, o SDK 37).
3. Na barra de cima, escolha a configuração de execução:
   - **`compose`** → Etapa 2 — ícone **“Caderno de Receitas”**;
   - **`parcial`** → Parcial — ícone **“Caderno (Views)”**.
4. Clique em **Run ▶**.

### Pela linha de comando
```bash
./gradlew :compose:installDebug    # instala a Etapa 2 (Compose)
./gradlew :parcial:installDebug    # instala a Parcial (Views)
./gradlew test                     # roda os 34 testes automatizados (na JVM, sem emulador)
```
No Windows, use `gradlew.bat` no lugar de `./gradlew`.

### Deep link (opcional, para testar)
Com o app instalado, este comando abre direto os detalhes de uma receita:
```bash
adb shell am start -a android.intent.action.VIEW -d "caderno://receita/52772"
```
O link também vai junto quando você compartilha uma receita pelo app.

---

## Fluxo completo (módulo `compose`)

1. **Explorar** — chips de categoria (`LazyRow`) e busca pelo nome (espera você parar de digitar
   antes de buscar). O app abre na última categoria escolhida. Resultados em `LazyColumn`, com selo
   “Quero fazer”/“Já fiz” nas receitas que já estão no caderno. Puxe a lista para baixo para atualizar.
2. **Receita** — foto, categoria, origem, tags, ingredientes, modo de preparo em passos, botão de
   vídeo (YouTube) e compartilhar.
3. **Registrar no caderno** — formulário com validação:
   - “Já fiz” exige quantas vezes (1 a 99) e uma nota de 1 a 5;
   - observação opcional com até 300 caracteres (contador ao vivo);
   - foto do prato opcional, escolhida pelo **Photo Picker** do Android;
   - erros aparecem no próprio campo e num resumo no topo, anunciado pelo leitor de tela.
4. **Meu caderno** — lista reativa do que foi salvo, com filtros (Todas / Quero fazer / Já fiz),
   quantas receitas você já fez e a média das suas notas.
5. **Reiniciar o app** — tudo continua lá (Room + arquivos + DataStore).

Sem internet, o app mostra a **cópia salva** (com um aviso “Sem conexão”); se ainda não houver
nada salvo, mostra o estado de erro com “Tentar de novo”.

> As receitas vêm da TheMealDB, que só tem conteúdo em inglês. A interface é toda em português e as
> categorias são traduzidas (Dessert → Sobremesa…), mas nomes e modos de preparo ficam no original.

---

## Arquitetura (módulo `compose`)

```
 Tela (Compose) ──evento──▶ ViewModel ──▶ Repository ──┬──▶ Room (cache + caderno) / arquivos (fotos)
      ▲                        │                       ├──▶ DataStore (preferências)
      └── StateFlow<UiState> ──┘                       └──▶ Ktor → API TheMealDB
```

- **Fluxo unidirecional:** a tela só desenha o `UiState` e avisa o ViewModel dos eventos
  (`aoMudarTexto`, `aoSalvar`…). Só o ViewModel muda o estado. Cada tela tem duas versões:
  `XxxScreen` (liga no ViewModel) e `XxxConteudo` (só desenha — fácil de testar).
- **Estados distintos:** carregando, conteúdo, vazio e erro têm visuais próprios
  (`TelaCarregando`, `TelaVazia`, `TelaErro`, `AvisoDesatualizado`).
- **Rotação e recriação:** os ViewModels sobrevivem à rotação. O texto da busca, a categoria e o
  rascunho do formulário ficam também no `SavedStateHandle`, então sobrevivem até quando o Android
  encerra o app em segundo plano. A pilha do Navigation 3 é salva porque as rotas são `@Serializable`.
- **Repository:** `ReceitaRepository` (catálogo + cache) e `CadernoRepository` (dados do usuário)
  são interfaces, com implementação real (`…Padrao`) e falsa nos testes.
- **Injeção de dependências manual:** `Dependencias` (interface) e `DependenciasDoApp` criam tudo uma
  vez; nos testes entra `DependenciasFalsas`.
- **DTO → domínio:** `RefeicaoDto` espelha o JSON (inclusive os 20 pares `strIngredientN`/`strMeasureN`)
  e `Mapeadores.kt` converte para `Receita`, limpando campos vazios, numeração de passos e links inválidos.
- **Cache com validade:** busca = 1 hora, receita = 1 dia, categorias = 7 dias. Dentro do prazo a API
  nem é chamada; “puxar para atualizar” ignora o prazo. Se a rede falhar, usa a cópia antiga
  marcada como desatualizada (*offline-first*).
- **Coroutines e cancelamento:** a busca usa `debounce` (500 ms) e `flatMapLatest`: uma busca nova
  **cancela** a anterior. O detalhe cancela o carregamento anterior com `Job.cancel()`. Tudo roda no
  `viewModelScope` (cancelado quando a tela sai da pilha), a cópia da foto roda em `Dispatchers.IO`
  e `CancellationException` nunca é engolida nos `try/catch`.
- **Telas grandes:** com a estratégia *lista + detalhe* do Navigation 3, em tablets ou celulares
  deitados a lista e a receita aparecem lado a lado.

### Estrutura de pastas
```
compose/src/main/java/br/com/caderno/receitas/
├── CadernoApplication.kt, Dependencias.kt, MainActivity.kt (deep link)
├── dominio/      modelos imutáveis e regras do formulário (ValidadorRegistro)
├── dados/        ReceitaRepository, CadernoRepository
│   ├── remoto/       Ktor (TheMealDbApi), DTOs e mapeadores
│   ├── local/        Room (entidades, DAOs, banco)
│   ├── arquivos/     fotos dos pratos na pasta interna do app
│   └── preferencias/ DataStore (última categoria, filtro do caderno)
└── ui/
    ├── navegacao/  rotas e NavDisplay (Navigation 3)
    ├── componentes/ estados de tela, cartão, estrelas, textos
    ├── explorar/ receita/ registro/ caderno/   uma pasta por tela (Screen + ViewModel)
    └── tema/
```

### Onde está cada requisito da Etapa 2

| Requisito | Onde |
|---|---|
| Navigation 3 (rotas, entries, back stack, argumentos) | `ui/navegacao/Rotas.kt` (`RotaReceita(receitaId)`), `NavegacaoApp.kt` |
| `LazyColumn` / `LazyRow` | resultados e categorias (`ExplorarScreen`), caderno e filtros (`CadernoScreen`), receita (`ReceitaScreen`) |
| Formulário com validação e feedback | `RegistroScreen` + `ValidadorRegistro` (erro por campo, resumo, contador, botão “Salvando…”) |
| `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle` | todos os `*ViewModel.kt` / `*Screen.kt` |
| Carregando / conteúdo / vazio / erro + rotação | `EstadoResultados`, `ReceitaUiState`, `CadernoUiState`; `SavedStateHandle` |
| Repository | `dados/ReceitaRepository.kt`, `dados/CadernoRepository.kt` |
| Operação assíncrona com coroutines e cancelamento | `ExplorarViewModel` (debounce + flatMapLatest), `ReceitaViewModel.carregar`, `ArmazenamentoFotosInterno` |
| Persistência local reativa | Room com `Flow` (caderno), arquivos (fotos), DataStore (preferências) |
| Acessibilidade | `contentDescription` em imagens e ícones, títulos com `heading()`, áreas de toque ≥ 48dp, estrelas como botões de opção (“Nota 3 de 5, selecionado”), erros com `error()`/`liveRegion`, cores próprias com contraste AA, textos em `sp` e telas roláveis para fonte grande |
| Sem segredos no código | chave lida do `local.properties` (fora do Git); `.gitignore` com `local.properties`, `.env`, `*.jks` |

**Opcionais atendidos:** DTO → domínio; testes automatizados (regras, ViewModel, repository, interface
e fluxo completo); validação de entradas (formulário, id do deep link, caminho de arquivo) e nada
sensível em logs; cache com validade; offline-first; animações implícitas (`AnimatedContent`,
`AnimatedVisibility`, `animateItem`, `animateContentSize`); telas grandes (lista + detalhe, largura
máxima de leitura); injeção manual com interfaces e fakes; **Photo Picker**, **compartilhamento** e
**deep link**.

---

## Parcial (módulo `parcial`)

- **Duas telas em XML:** `ListaReceitasActivity` (`RecyclerView` com `ListAdapter`/`DiffUtil`) e
  `DetalheReceitaActivity`. Usa `LinearLayout`, `FrameLayout`, `ConstraintLayout`, `NestedScrollView`,
  `TextView`, `ImageView`, `ImageButton`, `Space` e componentes Material.
- **Intent explícita com dados:** `DetalheReceitaActivity.criarIntent(contexto, receitaId, favorita)`.
  A tela de detalhes devolve o novo estado de favorita pela *Activity Result API*.
- **ViewBinding** em tudo (nenhum `findViewById` no código do app).
- **Interações que atualizam a tela:** coração de favorita na lista (atualiza o resumo
  “8 receitas · 2 favoritas”), filtro “só favoritas” (com estado vazio), botões −/+ do rendimento
  (recalculam as quantidades com regra de três), ingredientes para marcar (“3 de 6 separados”) e
  passos do preparo que ficam riscados ao tocar.
- **Modelos imutáveis:** `Receita`, `Ingrediente` e `ReceitaNaLista` são `data class` só com `val`.
  Valores opcionais (`?`): tempo de preparo (“Tempo não informado”), dica (some da tela), quantidade
  (“Sal a gosto”) e unidade.
- **Dados simulados:** `dados/ReceitasMock.kt`.
- **Opcionais:** componente XML reutilizável (`view_contador_porcoes.xml` + `ContadorPorcoesView`),
  linhas infladas sob demanda (`item_ingrediente.xml`, `item_passo.xml`) e interface em `Fragment`
  (`ModoPreparoFragment`) com ViewBinding respeitando o ciclo de vida (`_binding = null` em `onDestroyView`).

---

## Testes

`./gradlew test` roda **34 testes** na JVM (os de interface usam Robolectric, sem emulador):

| Arquivo | O que testa |
|---|---|
| `ValidadorRegistroTest` | regras do formulário |
| `RemotoTest` | cliente Ktor com `MockEngine` (URLs, JSON, erro HTTP) e mapeamento DTO → domínio |
| `RepositoriesTest` | Room de verdade em memória: validade do cache, modo offline, foto trocada/apagada |
| `ViewModelsTest` | debounce, cancelamento da busca anterior, estados vazio/erro, formulário, `SavedStateHandle` |
| `InterfaceTest` | componentes, caderno vazio, deep link e o **fluxo completo**: explorar → receita → formulário com erro → salvar → caderno |
| `parcial/ParcialTest` | recálculo das porções, Intent explícita, interações e resultado da tela de detalhes |

---

## Bibliotecas externas

| Biblioteca | Para quê |
|---|---|
| **Jetpack Compose** (Material 3, ícones) | Interface declarativa do módulo `compose` |
| **Material 3 Adaptive – Navigation 3** | Estratégia *lista + detalhe*: duas colunas em telas largas |
| **Navigation 3** | Navegação com uma pilha de rotas controlada pelo app |
| **Lifecycle** (ViewModel, runtime-compose, viewmodel-navigation3) | `ViewModel`, `SavedStateHandle`, `collectAsStateWithLifecycle` e um ViewModel por tela da pilha |
| **Room** | Banco local: cache do catálogo e caderno do usuário |
| **DataStore Preferences** | Preferências simples (última categoria, filtro do caderno) |
| **Ktor Client** (Android, Content Negotiation) | Chamadas HTTP à TheMealDB |
| **Kotlinx Serialization** | JSON ↔ classes Kotlin (DTOs, rotas, rascunho do formulário) |
| **Kotlinx Coroutines** | Operações assíncronas e `Flow` |
| **Coil 3** | Carregar fotos das receitas (internet) e dos pratos (arquivos) |
| **KSP** | Gera o código do Room na compilação |
| **AppCompat, Material Components, RecyclerView, Fragment, ConstraintLayout** | Base das telas XML do módulo `parcial` |
| **JUnit, Coroutines Test, Ktor MockEngine, Robolectric, Compose UI Test** | Testes automatizados |

Dados e fotos das receitas: [TheMealDB](https://www.themealdb.com/api.php).
