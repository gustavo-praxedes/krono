# Estação de Trabalho
Windows 10. Asus Tuf Gaming B550M-Plus. AMD Ryzen 7 5700X. 16GB RAM. RTX 3060 Ti.
Android Studio (Koala/Ladybug).

# Contexto Geral do Projeto Krono

## 1. Descrição Geral do App Krono
O **Krono** é um aplicativo Android focado em produtividade que atua como um cronômetro flutuante. O principal diferencial do app é sua capacidade de ser exibido sobre outros aplicativos através de uma janela sobreposta (overlay), permitindo ao usuário monitorar o tempo enquanto executa outras tarefas no dispositivo. O app inclui funcionalidades como contagem de tempo, modo foco (Focus Mode), suporte a personalização visual da interface (cores e escala), limites de tempo de uso, persistência do estado (para sobreviver a reboots e fechamentos) e um controle eficiente por notificações nativas do Android.

## 2. Descrição das Tecnologias, Bibliotecas e Linguagem Usada
*   **Linguagem**: Kotlin.
*   **SDK Android**: `minSdk` 26 (Android 8.0) e `targetSdk` 35 (Android 15).
*   **Interface (UI)**: Uso exclusivo de **Jetpack Compose** para a criação de todas as telas, botões flutuantes (overlay) e diálogos de configuração. `Material 3` para os componentes padrões de UI.
*   **Programação Assíncrona e Reativa**: Utiliza extensamente **Kotlin Coroutines** e **StateFlow** para a reatividade do timer e das configurações da interface.
*   **Persistência Local**: 
    *   **DataStore Preferences**: Utilizado no `OverlayDataStore.kt` para salvar configurações de layout, aparência e comportamento.
    *   **SharedPreferences**: Utilizado pelo `TimerPreferences.kt` para leitura e gravação síncrona do estado do cronômetro para recuperação rápida.
*   **Background e Sistema**: **Foreground Service** (`MainService`) com `Notification` persistente, `WindowManager` nativo para manipulação de Overlay, `WakeLock` para manter a tela ligada e integração com permissões `SYSTEM_ALERT_WINDOW`.

## 3. Estrutura de Arquivos e Componentes Principais

### Núcleo e Negócio
*   **`KronoApp.kt`**: Inicializa canais de notificação e provê o singleton do `TimerViewModel`.
*   **`MainService.kt`**: Gerencia o cronômetro em background e a janela flutuante (`WindowManager`).
*   **`TimerViewModel.kt`**: Lógica do cronômetro (loop de 10ms) e controle de estado.
*   **`BootReceiver.kt`**: Reinicia o serviço do cronômetro automaticamente após o reboot do celular.
*   **`NotificationActionReceiver.kt`**: Captura cliques em botões de "Pausar" ou "Resetar" diretamente da notificação.

### Interface (UI / Jetpack Compose)
*   **`MainActivity.kt`**: Tela principal de configurações e navegação.
*   **`FloatingTimerUi.kt`**: UI do cronômetro que flutua sobre outros apps.
*   **`FocusActivity.kt`**: Modo de foco que bloqueia distrações.
*   **Dialogs (`UpdateDialog`, `ChangelogDialog`, `DonationDialog`, `AboutDialog`, `ColorPickerDialog`)**: Componentes de interface para interações específicas.
*   **`KronoTheme.kt`**: Definições de cores e tema Material 3.

### Dados e Utilidades
*   **`TimeState.kt` / `TimeUtils.kt`**: Estrutura de dados do timer e formatação de milissegundos para texto.
*   **`OverlayConfig.kt` / `OverlayDataStore.kt`**: Persistência de preferências de interface.
*   **`UpdateChecker.kt` / `ApkInstaller.kt`**: Lógica para buscar novas versões no GitHub e instalar o APK automaticamente.

### Automação e CI/CD (Raiz do Projeto)
*   **`package.json`**: Define o comando `npm run release` para iniciar o ciclo de nova versão.
*   **`.versionrc`**: Configura o `commit-and-tag-version` (Changelog, bump de versão e hooks).
*   **`scripts/update-version.js`**: Script que sincroniza a versão do `package.json` com o `build.gradle.kts`.
*   **`scripts/update-readme.js`**: Mantém o arquivo README.md atualizado com a última versão.
*   **`.github/workflows/release.yml`**: Automação do GitHub Actions que gera o APK e cria a Release online ao detectar uma nova tag.

## 4. Sistema de Versionamento e Automação (CI/CD)

O projeto adota o padrão **Semantic Versioning (SemVer)** e utiliza **Conventional Commits** para automatizar o ciclo de vida de lançamentos.

### Componentes do Sistema:
*   **Padrão de Commits**: Uso de prefixos como `feat:`, `fix:`, `perf:`, `docs:`, `chore:`. O tipo de prefixo determina se a versão subirá um degrau Major, Minor ou Patch.
*   **Ferramenta Base**: `commit-and-tag-version` (sucessor do Standard Version), configurado via `.versionrc`.
*   **Versionamento Híbrido**:
    *   **VersionName (App)**: Segue o SemVer (ex: 2.4.2), sincronizado entre `package.json` e `app/build.gradle.kts`.
    *   **VersionCode (Google Play)**: Gerado dinamicamente via Kotlin no formato `yyMMddHH` (Data/Hora), garantindo unicidade e ordem crescente sem intervenção manual.

### Scripts e Fluxo Técnico:
1.  **`npm run release`**: Comando mestre que executa:
    *   `commit-and-tag-version`: Analisa commits, gera o changelog e atualiza as versões.
2.  **`scripts/update-version.js` (Custom Updater)**: Script Node.js que atua como uma ponte. Ele permite que o `commit-and-tag-version` leia e escreva o `versionName` dentro de um arquivo Kotlin (`.gradle.kts`), usando Regex para localizar a string correta.
3.  **`scripts/update-readme.js`**: Chamado automaticamente no hook `postbump`. Ele sincroniza a versão e o conteúdo do último changelog no `README.md` antes do commit de release ser fechado.
4.  **`extractChangelog` (Task Gradle)**: Uma automação interna do Android que, durante o build, extrai apenas as notas da versão atual do `CHANGELOG.md` e as salva em `res/raw/changelog.md` para exibição dentro do app.
5.  **GitHub Actions (`release.yml`)**:
    *   Disparado por tags `v*`.
    *   Executa o build em ambiente Linux isolado.
    *   Gera o APK de release e cria uma "GitHub Release" com os binários e notas de versão.

### Como realizar uma nova versão:
1.  Faça seus commits usando o padrão (ex: `feat: add focus mode`).
2.  Execute `npm run release`.
3.  O sistema fará o bump, atualizará arquivos, criará a tag e enviará para o GitHub, onde o APK será gerado online.
