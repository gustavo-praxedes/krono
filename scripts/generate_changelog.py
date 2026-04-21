import subprocess
import os
import json
import sys
from datetime import datetime

# Garante que a saída do console aceite UTF-8 para evitar erros com emojis no Windows
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Caminhos dos arquivos
ANDROID_RAW_CHANGELOG = "app/src/main/res/raw/changelog.md"
PACKAGE_JSON = "package.json"

def get_latest_tag():
    try:
        # Pega a última tag de versão
        return subprocess.check_output(["git", "describe", "--tags", "--abbrev=0"], stderr=subprocess.STDOUT).decode("utf-8").strip()
    except subprocess.CalledProcessError:
        return None

def get_commits_since(tag):
    if tag:
        # Commits desde a última tag até agora
        cmd = ["git", "log", f"{tag}..HEAD", "--pretty=format:%s"]
    else:
        # Todos os commits se não houver tag
        cmd = ["git", "log", "--pretty=format:%s"]
    
    try:
        output = subprocess.check_output(cmd).decode("utf-8").strip()
        return output.split("\n") if output else []
    except:
        return []

def categorize_commits(commits):
    categories = {
        "✨ Novidades": [],
        "🐛 Correções": [],
        "⚡ Performance": [],
        "🔧 Manutenção": [],
        "📝 Documentação": []
    }
    
    for commit in commits:
        commit = commit.strip()
        if not commit: continue
        
        # Lógica de categorização baseada em prefixos comuns
        lower_commit = commit.lower()
        if lower_commit.startswith("feat"):
            categories["✨ Novidades"].append(commit.split(":", 1)[-1].strip())
        elif lower_commit.startswith("fix"):
            categories["🐛 Correções"].append(commit.split(":", 1)[-1].strip())
        elif lower_commit.startswith("perf"):
            categories["⚡ Performance"].append(commit.split(":", 1)[-1].strip())
        elif any(lower_commit.startswith(p) for p in ["chore", "refactor", "style", "build"]):
            categories["🔧 Manutenção"].append(commit.split(":", 1)[-1].strip())
        elif lower_commit.startswith("docs"):
            categories["📝 Documentação"].append(commit.split(":", 1)[-1].strip())
        else:
            # Commits sem prefixo padrão vão para Manutenção
            categories["🔧 Manutenção"].append(commit)
            
    return {k: v for k, v in categories.items() if v}

def update_android_raw(categorized):
    text = ""
    for cat, items in categorized.items():
        text += f"# {cat}\n"
        for item in items:
            text += f"- {item}\n"
        text += "\n"
    
    if not text:
        text = "- Melhorias de estabilidade e correções internas."

    # Garante que a pasta existe
    os.makedirs(os.path.dirname(ANDROID_RAW_CHANGELOG), exist_ok=True)
    
    # Escreve explicitamente em UTF-8
    with open(ANDROID_RAW_CHANGELOG, "w", encoding="utf-8") as f:
        f.write(text.strip())

def main():
    print("🚀 Sincronizando notas de versão para o App...")
    
    latest_tag = get_latest_tag()
    commits = get_commits_since(latest_tag)
    
    # Processa os commits
    categorized = categorize_commits(commits)
    
    # Atualiza apenas o arquivo interno do Android
    update_android_raw(categorized)
    print(f"✅ {ANDROID_RAW_CHANGELOG} atualizado com sucesso.")

if __name__ == "__main__":
    main()
