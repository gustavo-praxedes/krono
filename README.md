<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="96" alt="Krono Logo"/>

<h1>Krono</h1>

<p><em>O cronômetro minimalista que flutua sobre sua produtividade.</em></p>

<p>
  <a href="https://github.com/gustavo-praxedes/krono/releases/latest">
    <img src="https://img.shields.io/github/v/release/gustavo-praxedes/krono?style=flat-square&logo=github&color=4f46e5&label=Vers%C3%A3o" alt="Versão"/>
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+"/>
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/gustavo-praxedes/krono?style=flat-square&color=6b7280" alt="Licença"/>
  </a>
  <img src="https://img.shields.io/badge/offline--first-6366f1?style=flat-square" alt="Offline First"/>
  <img src="https://img.shields.io/badge/sem%20an%C3%BAncios-10b981?style=flat-square" alt="Sem Anúncios"/>
</p>

<p>
  <a href="https://github.com/gustavo-praxedes/krono/releases/latest">
    <img src="https://img.shields.io/badge/↓ Baixar APK-4f46e5?style=for-the-badge" alt="Baixar APK"/>
  </a>
  &nbsp;
  <a href="https://github.com/gustavo-praxedes/krono/issues">
    <img src="https://img.shields.io/badge/Reportar Bug-ef4444?style=for-the-badge" alt="Reportar Bug"/>
  </a>
  &nbsp;
  <a href="https://ko-fi.com/gustavo-praxedes">
    <img src="https://img.shields.io/badge/Apoiar o Projeto-f59e0b?style=for-the-badge" alt="Apoiar Projeto"/>
  </a>
</p>

</div>

---

## O que é o Krono

O **Krono** é um cronômetro Android de código aberto com widget flutuante. Ele roda sobre qualquer aplicativo, é controlado por gestos e não coleta nenhum dado do usuário.

**O Krono faz:**
- Iniciar, pausar e resetar o tempo com um toque
- Flutuar sobre qualquer app sem bloquear a interação com ele
- Persistir o estado do cronômetro após reinicialização do dispositivo
- Permitir personalização completa de cor, opacidade e tamanho

**O Krono não faz:**
- Coletar dados, exibir anúncios ou requerer internet
- Funcionar como agenda, lembrete ou temporizador regressivo
- Suportar múltiplos cronômetros simultâneos (por enquanto)

---

## Pré-requisitos

Antes de instalar, verifique se o seu dispositivo atende aos requisitos:

| Requisito | Mínimo | Recomendado |
|:---|:---|:---|
| **Versão do Android** | 8.0 (Oreo, API 26) | 11+ |
| **Permissão de sobreposição** | Obrigatória | — |
| **Conexão com internet** | Não necessária | — |
| **Espaço em disco** | ~5 MB | — |

---

## Instalação

1. Acesse **[Releases](https://github.com/gustavo-praxedes/krono/releases)** e baixe o `.apk` mais recente.
2. No Android, vá em **Configurações → Aplicativos → Instalar apps desconhecidos** e habilite para o seu gerenciador de arquivos.
3. Abra o arquivo `.apk` baixado e confirme a instalação.
4. Na primeira execução, o Krono solicitará a permissão de **"Sobrepor a outros apps"** — conceda-a para que o widget funcione.

---

## Funcionalidades

<table>
  <tr>
    <td valign="top" width="50%">
      <h4>Widget Flutuante</h4>
      <ul>
        <li><strong>WindowManager</strong> — Flutua sobre qualquer app</li>
        <li><strong>Física de Borda</strong> — Gruda suavemente nas laterais</li>
        <li><strong>Persistência</strong> — Lembra a última posição ao reabrir</li>
        <li><strong>Passivo</strong> — Não bloqueia toques no app de fundo</li>
      </ul>
    </td>
    <td valign="top" width="50%">
      <h4>Personalização</h4>
      <ul>
        <li><strong>Cores HSB</strong> — Ajuste de tom e saturação</li>
        <li><strong>Opacidade</strong> — Transparência de 0% a 100%</li>
        <li><strong>Tamanho</strong> — Escala de 0.5× a 1.5×</li>
        <li><strong>Bordas</strong> — Arredondamento customizável</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td valign="top" width="50%">
      <h4>Controle por Gestos</h4>
      <ul>
        <li><strong>Toque simples</strong> — Play / Pause</li>
        <li><strong>Toque duplo</strong> — Resetar</li>
        <li><strong>Notificação</strong> — Controles na barra de status</li>
        <li><strong>Pós-reboot</strong> — Retoma o estado automaticamente</li>
      </ul>
    </td>
    <td valign="top" width="50%">
      <h4>Confiabilidade</h4>
      <ul>
        <li><strong>Código aberto</strong> — Auditável e transparente</li>
        <li><strong>Sem rastreadores</strong> — Zero coleta de dados</li>
        <li><strong>Offline</strong> — Sem dependência de rede</li>
        <li><strong>Leve</strong> — Consumo mínimo de CPU e bateria</li>
      </ul>
    </td>
  </tr>
</table>

---

## Changelog Recente

[//]: # (CHANGELOG_LATEST_START)

## [2.5.5](https://github.com/gustavo-praxedes/krono/compare/v2.5.4...v2.5.5) (2026-04-21)

[//]: # (CHANGELOG_LATEST_END)

---

<div align="center">
  <sub>Se o Krono foi útil para você, considere deixar uma ⭐ no repositório.</sub>
</div>
