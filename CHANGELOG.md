# Changelog

Histórico de mudanças do Krono.

## [3.1.0](https://github.com/gustavo-praxedes/krono/compare/v3.0.0...v3.1.0) (2026-04-23)


### ✨ Novidades

* Persistencia do tempo adicionada. ([325b237](https://github.com/gustavo-praxedes/krono/commit/325b2378c34f1c12d870ad01faf030504f0fbfb1))


### 🐛 Correções

* Bug do overlay não encostar em baixo resolvido. ([ec58b06](https://github.com/gustavo-praxedes/krono/commit/ec58b06c4831b5e0358cc5ccee4566ac7dbc9ac4))
* Correção do overlay fantasma no modo foco. ([fb069af](https://github.com/gustavo-praxedes/krono/commit/fb069af42fce28d1524f164110b51b12d76b8b96))


### ⚡ Performance

* Agora o overlay segue os temas escolhidos pelo usuário. ([12941c0](https://github.com/gustavo-praxedes/krono/commit/12941c0aa955c823ebe103d6695563e5da36403a))
* Ajuste estético da janela de permissão. ([ef27ce5](https://github.com/gustavo-praxedes/krono/commit/ef27ce5f7a00dc7e7b82d777b7153f190a35e693))
* Padronização da janela UpdateDialog. ([7ef6680](https://github.com/gustavo-praxedes/krono/commit/7ef66808de790c4ba758039fde34f5badd3e110c))
* Unificação dos pedidos de permissão. ([a6fa33a](https://github.com/gustavo-praxedes/krono/commit/a6fa33ac394cbf07f1da239b4bd05f81217eec9a))


### 🔧 Manutenção

* Refatoração do FloatingTimerUi. ([2388fd3](https://github.com/gustavo-praxedes/krono/commit/2388fd3cf2cbcf6146e2d96e7a3d4a0eea2624bb))

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
