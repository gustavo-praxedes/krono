# Changelog

Histórico de mudanças do Krono.

## [3.0.0] (2026-04-22)

### ✨ Novidades
- feat: Persistência do tempo adicionada. O tempo acumulado agora sobrevive ao fechamento do app.

### 🐛 Correções
- fix: Correção do overlay fantasma que aparecia no Modo Foco.
- fix: Ajuste no arraste para permitir que o overlay encoste na borda inferior da tela.
- fix: Correção do check de permissão que não marcava instantaneamente.

### ⚡ Performance
- perf: Otimização do tick do cronômetro para 250ms visando economia de bateria.
- perf: Redução de recomposições desnecessárias na UI do overlay.
- perf: Unificação das chamadas de permissão no `AppNavigation`.

### 🔧 Manutenção
- chore: Refatoração do `FloatingTimerUi` para uso de tokens de design.
- chore: Implementação do sistema centralizado de tokens em `KronoTokens`.
- chore: Configuração do `keystore.properties` e ajustes no CI/CD (GitHub Actions).

---

## [2.5.12] (2026-04-22)

### 🐛 Correções
- fix: Bug do overlay no modo foco não sendo recriado corrigido.
- fix: Ajuste de ícones no `ChangelogDialog` e `AboutDialog`.

### ⚡ Performance
- perf: Padronização de caixas de diálogo e botões.
- perf: Refinamento da tipografia no campo de limite de tempo.

### 🔧 Manutenção
- chore: Limpeza de código e remoção de recursos não utilizados.
- chore: Aprimoramento da construção do APK no GitHub.

---
*Para ver o histórico anterior completo, consulte as Releases no GitHub.*
