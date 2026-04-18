# Estação de Trabalho
Windows 10. Asus Tuf Gaming B550M-Plus. AMD Ryzen 7 5700X. 16GB RAM. RTX 3060 Ti.
Android Studio

# Contexto Geral do Projeto Krono

## 1. Descrição Geral do App Krono
O **Krono** é um aplicativo Android focado em produtividade que atua como um cronômetro flutuante. O principal diferencial do app é sua capacidade de ser exibido sobre outros aplicativos através de uma janela sobreposta (overlay), permitindo ao usuário monitorar o tempo enquanto executa outras tarefas no dispositivo. O app inclui funcionalidades como contagem de tempo, modo foco (Focus Mode), suporte a personalização visual da interface (cores e escala), limites de tempo de uso, persistência do estado (para sobreviver a reboots e fechamentos) e um controle eficiente por notificações nativas do Android.

## 2. Descrição das Tecnologias, Bibliotecas e Linguagem Usada
*   **Linguagem**: Kotlin.
*   **SDK Android**: `minSdk` 26 (Android 8.0) e `targetSdk` 35 (Android 15).
*   **Interface (UI)**: Uso exclusivo de **Jetpack Compose** para a criação de todas as telas, botões flutuantes (overlay) e diálogos de configuração. `Material 3` para os componentes padrões de UI.
*   **Programação Assíncrona e Reativa**: Utiliza extensamente **Kotlin Coroutines** e **StateFlow** para a reatividade do timer e das configurações da interface.
*   **Persistência Local**: 
    *   **DataStore Preferences**: Utilizado no `OverlayDataStore.kt` para salvar configurações de layout, aparência e comportamento de forma assíncrona.
    *   **SharedPreferences**: Utilizado pelo `TimerPreferences.kt` para leitura e gravação síncrona do estado do cronômetro para recuperação rápida.
*   **Background e Sistema**: **Foreground Service** (`MainService`) com `Notification` persistente, `WindowManager` nativo para manipulação de Overlay (janela flutuante), `WakeLock` para forçar a tela ligada e integração com permissões `SYSTEM_ALERT_WINDOW`.

## 3. Caminhos dos Arquivos e Descrições

*   **`build.gradle.kts` e `app/build.gradle.kts`**: 
    *   *Descrição:* Gerenciam a configuração de build, dependências do Jetpack Compose, Coroutines, DataStore e as versões do aplicativo.
*   **`app/src/main/java/com/krono/app/KronoApp.kt`**: 
    *   *Descrição:* Classe base da Aplicação (`Application`). Instancia o canal de notificação (`NotificationChannel`) do Android e provê de forma global o singleton do `TimerViewModel`.
*   **`app/src/main/java/com/krono/app/service/MainService.kt`**: 
    *   *Descrição:* O coração executivo das amarras do Android. Mantém o cronômetro ativo em background, gerencia a interface em formato *Overlay* via `WindowManager` e atualiza a Notificação do timer e opções de manter a tela ligada (`WakeLock`).
*   **`app/src/main/java/com/krono/app/viewmodel/TimerViewModel.kt`**: 
    *   *Descrição:* Classe com a regra de negócios. Contém o loop de repetição em coroutine a cada 10ms para calcular pontualmente o tempo de execução do cronômetro e salva os check-points nas preferências.
*   **`app/src/main/java/com/krono/app/ui/MainActivity.kt`**: 
    *   *Descrição:* Ação principal de Interface. Concentra a navegação básica (Timer / Configurações) do App Principal e toda a gestão da tela de Configuração, onde o usuário pode alterar as cores do cronômetro flutuante, limite de tempo ou habilitar funções sistêmicas.
*   **`app/src/main/java/com/krono/app/ui/FloatingTimerUi.kt`**: 
    *   *Descrição:* O desenho (em ambiente Jetpack Compose) do componente que flutuará sobre a tela dos outros apps (exibição de horas e botões).
*   **`app/src/main/java/com/krono/app/data/TimeState.kt`**: 
    *   *Descrição:* Um `DataClass` limpo que encapsula o estado decorrido, momento do `start` e controle de pausas.
*   **`app/src/main/java/com/krono/app/data/TimeUtils.kt`**: 
    *   *Descrição:* Funções essenciais para converter valores `Long` processados da CPU para formatos de texto de relógio visível para humanos.
*   **`app/src/main/java/com/krono/app/data/OverlayConfig.kt` / `OverlayDataStore.kt`**: 
    *   *Descrição:* Configurações específicas para tamanho, opacidade, visibilidade, vibração/bips da UI flutuante armazenadas em memória persistente.
*   **`app/src/main/java/com/krono/app/util/UpdateChecker.kt` e Dialogs (`UpdateDialog.kt`, `DonationDialog.kt`, `ColorPickerDialog.kt`)**: 
    *   *Descrição:* Partes isoladas do código lidando com interações com a Web (checar update num endpoint ou repositório) e sub-janelas do aplicativo padrão.

