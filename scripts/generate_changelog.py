#!/usr/bin/env python3
"""
Gera/atualiza CHANGELOG.md com base nos commits desde a última tag git.
Executa automaticamente via git hook post-commit.

Formato de commit esperado (Conventional Commits):
  feat: descrição
  feat(scope): descrição
  fix: descrição
  perf: descrição
  docs: descrição
  chore: descrição
  refactor: descrição
"""

import subprocess
import re
import sys
from datetime import date
from pathlib import Path

# ── Configuração ──────────────────────────────────────────────
CHANGELOG_FILE = Path(__file__).parent.parent / "CHANGELOG.md"
REPO_URL       = "https://github.com/gustavo-praxedes/cronometro-flutuante"

TYPE_LABELS = {
    "feat"    : "✨ Novidades",
    "fix"     : "🐛 Correções",
    "perf"    : "⚡ Performance",
    "refactor": "♻️  Refatoração",
    "docs"    : "📝 Documentação",
    "chore"   : "🔧 Manutenção",
}

TYPE_ORDER = ["feat", "fix", "perf", "refactor", "docs", "chore"]

# ── Helpers Git ───────────────────────────────────────────────

def run(cmd: list[str]) -> str:
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.stdout.strip()

def get_latest_tag() -> str | None:
    tag = run(["git", "describe", "--tags", "--abbrev=0"])
    return tag if tag else None

def get_current_version() -> str:
    """Lê versionName do app/build.gradle.kts"""
    gradle = Path(__file__).parent.parent / "app" / "build.gradle.kts"
    if not gradle.exists():
        return "unreleased"
    for line in gradle.read_text().splitlines():
        m = re.search(r'versionName\s*=\s*"([^"]+)"', line)
        if m:
            return m.group(1)
    return "unreleased"

def get_commits_since(tag: str | None) -> list[str]:
    """Retorna lista de mensagens de commit desde a tag (ou todos se não houver tag)."""
    if tag:
        log_range = f"{tag}..HEAD"
    else:
        log_range = "HEAD"
    output = run(["git", "log", log_range, "--pretty=format:%s"])
    return [line for line in output.splitlines() if line.strip()]

# ── Parser de commits ─────────────────────────────────────────

COMMIT_RE = re.compile(
    r'^(?P<type>feat|fix|perf|refactor|docs|chore|style|test|ci|build)'
    r'(?:\((?P<scope>[^)]+)\))?'
    r'(?P<breaking>!)?'
    r':\s*(?P<desc>.+)$',
    re.IGNORECASE
)

def parse_commits(commits: list[str]) -> dict[str, list[str]]:
    """Agrupa commits por tipo."""
    grouped: dict[str, list[str]] = {t: [] for t in TYPE_ORDER}

    for msg in commits:
        msg = msg.strip()
        if not msg:
            continue

        # Ignora commits de merge e versão
        if msg.startswith("Merge") or re.match(r'^v?\d+\.\d+', msg):
            continue

        m = COMMIT_RE.match(msg)
        if m:
            ctype = m.group("type").lower()
            scope = m.group("scope")
            desc  = m.group("desc").strip()
            # Capitaliza primeira letra
            desc  = desc[0].upper() + desc[1:] if desc else desc
            entry = f"{scope}: {desc}" if scope else desc
            if ctype in grouped:
                grouped[ctype].append(entry)
        else:
            # Commit sem prefixo convencional — vai para chore
            clean = msg[0].upper() + msg[1:] if msg else msg
            grouped["chore"].append(clean)

    # Remove tipos vazios
    return {k: v for k, v in grouped.items() if v}

# ── Gerador de markdown ───────────────────────────────────────

def build_section(version: str, grouped: dict[str, list[str]], tag: str | None) -> str:
    today    = date.today().isoformat()
    tag_prev = tag or ""

    lines = [f"## [{version}] — {today}", ""]

    for ctype in TYPE_ORDER:
        items = grouped.get(ctype)
        if not items:
            continue
        label = TYPE_LABELS.get(ctype, ctype.capitalize())
        lines.append(f"### {label}")
        lines.append("")
        for item in items:
            lines.append(f"- {item}")
        lines.append("")

    if tag_prev:
        compare_url = f"{REPO_URL}/compare/{tag_prev}...v{version}"
        lines.append(f"**Comparação completa:** [{tag_prev}...v{version}]({compare_url})")
        lines.append("")

    return "\n".join(lines)

# ── Leitura e escrita do CHANGELOG.md ────────────────────────

def load_existing() -> str:
    if CHANGELOG_FILE.exists():
        return CHANGELOG_FILE.read_text(encoding="utf-8")
    return ""

def version_already_in_changelog(version: str, content: str) -> bool:
    return bool(re.search(rf"^## \[{re.escape(version)}\]", content, re.MULTILINE))

def prepend_section(new_section: str, existing: str) -> str:
    """
    Insere a nova seção no topo, após o cabeçalho opcional do arquivo.
    Preserva todo o conteúdo anterior.
    """
    header_end = 0

    # Detecta cabeçalho YAML front-matter ou linhas de título (# CHANGELOG)
    lines = existing.splitlines(keepends=True)
    for i, line in enumerate(lines):
        if line.startswith("# ") or line.startswith("<!-- "):
            header_end = i + 1
            break

    before = "".join(lines[:header_end])
    after  = "".join(lines[header_end:])

    separator = "\n" if after.strip() else ""
    return f"{before}{new_section}\n{separator}{after}"

def update_section(version: str, new_section: str, existing: str) -> str:
    """Substitui a seção da versão se já existir (re-geração)."""
    pattern = rf"(## \[{re.escape(version)}\].*?)(?=^## \[|\Z)"
    replaced = re.sub(pattern, new_section + "\n", existing,
                      flags=re.MULTILINE | re.DOTALL)
    return replaced

# ── Ponto de entrada ──────────────────────────────────────────

def main():
    latest_tag = get_latest_tag()
    commits    = get_commits_since(latest_tag)

    if not commits:
        print("ℹ️  Nenhum commit novo desde a última tag — CHANGELOG.md não alterado.")
        return

    version = get_current_version()
    grouped = parse_commits(commits)

    if not any(grouped.values()):
        print("ℹ️  Nenhum commit convencional encontrado — CHANGELOG.md não alterado.")
        return

    new_section = build_section(version, grouped, latest_tag)
    existing    = load_existing()

    if version_already_in_changelog(version, existing):
        updated = update_section(version, new_section, existing)
        print(f"🔄 Seção [{version}] atualizada no CHANGELOG.md")
    else:
        updated = prepend_section(new_section, existing)
        print(f"✅ Seção [{version}] adicionada ao CHANGELOG.md")

    CHANGELOG_FILE.write_text(updated, encoding="utf-8")

if __name__ == "__main__":
    main()