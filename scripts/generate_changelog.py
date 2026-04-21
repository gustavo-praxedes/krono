import subprocess
import os
import re
from datetime import datetime

# Caminhos dos arquivos
CHANGELOG_FILE = "CHANGELOG.md"
ANDROID_RAW_CHANGELOG = "app/src/main/res/raw/changelog.md"

def get_latest_tag():
    try:
        return subprocess.check_output(["git", "describe", "--tags", "--abbrev=0"]).decode("utf-8").strip()
    except:
        return None

def get_current_version():
    # Tenta ler a versão do build.gradle.kts ou similar, aqui vamos assumir que você passa via argumento ou usa a última tag + "next"
    return "v1.0.0" # Altere conforme sua lógica de versão

def get_commits_since(tag):
    if tag:
        cmd = ["git", "log", f"{tag}..HEAD", "--pretty=format:%s"]
    else:
        cmd = ["git", "log", "--pretty=format:%s"]
    
    try:
        return subprocess.check_output(cmd).decode("utf-8").split("\n")
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
        
        if commit.lower().startswith("feat"):
            categories["✨ Novidades"].append(commit.split(":", 1)[-1].strip())
        elif commit.lower().startswith("fix"):
            categories["🐛 Correções"].append(commit.split(":", 1)[-1].strip())
        elif commit.lower().startswith("perf"):
            categories["⚡ Performance"].append(commit.split(":", 1)[-1].strip())
        elif commit.lower().startswith("chore") or commit.lower().startswith("refactor"):
            categories["🔧 Manutenção"].append(commit.split(":", 1)[-1].strip())
        elif commit.lower().startswith("docs"):
            categories["📝 Documentação"].append(commit.split(":", 1)[-1].strip())
        else:
            categories["🔧 Manutenção"].append(commit)
            
    return {k: v for k, v in categories.items() if v}

def build_changelog_text(version, categorized):
    date = datetime.now().strftime("%Y-%m-%d")
    text = f"## [{version}] - {date}\n\n"
    
    for cat, items in categorized.items():
        text += f"### {cat}\n"
        for item in items:
            text += f"- {item}\n"
        text += "\n"
    return text

def update_root_changelog(new_content):
    existing_content = ""
    if os.path.exists(CHANGELOG_FILE):
        with open(CHANGELOG_FILE, "r", encoding="utf-8") as f:
            existing_content = f.read()
    
    # Insere a nova versão no topo, após o título principal se existir
    if "# Changelog" in existing_content:
        updated = existing_content.replace("# Changelog", f"# Changelog\n\n{new_content}", 1)
    else:
        updated = f"# Changelog\n\n{new_content}{existing_content}"
        
    with open(CHANGELOG_FILE, "w", encoding="utf-8") as f:
        f.write(updated)

def update_android_raw(categorized):
    # O app espera uma lista simples de marcadores para o parseChangelog funcionar bem
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
    
    with open(ANDROID_RAW_CHANGELOG, "w", encoding="utf-8") as f:
        f.write(text.strip())

def main():
    print("🚀 Gerando changelog...")
    
    latest_tag = get_latest_tag()
    commits = get_commits_since(latest_tag)
    
    if not commits or (len(commits) == 1 and not commits[0]):
        print("ℹ️ Nenhum commit novo encontrado desde a última tag.")
        # Mesmo assim, vamos garantir que o arquivo do Android não esteja com a frase de erro
        update_android_raw({})
        return

    categorized = categorize_commits(commits)
    
    # Aqui você deve definir como pegar a versão atual (ex: ler do build.gradle)
    # Por agora, usaremos um placeholder ou você pode passar via sys.argv
    version = "v1.1.0" 

    new_section = build_changelog_text(version, categorized)
    
    # 1. Atualiza CHANGELOG.md da raiz
    update_root_changelog(new_section)
    print(f"✅ {CHANGELOG_FILE} atualizado.")
    
    # 2. Atualiza app/src/main/res/raw/changelog.md para a Versão Atual no App
    update_android_raw(categorized)
    print(f"✅ {ANDROID_RAW_CHANGELOG} atualizado para uso interno do App.")

if __name__ == "__main__":
    main()
