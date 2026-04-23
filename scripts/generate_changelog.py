import subprocess
import os
import sys
import json
import re
from datetime import datetime

# Garante que a saída do console aceite UTF-8
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

ANDROID_RAW_CHANGELOG = "app/src/main/res/raw/changelog.md"
ROOT_CHANGELOG = "CHANGELOG.md"
PACKAGE_JSON = "package.json"

def get_current_version():
    try:
        with open(PACKAGE_JSON, "r", encoding="utf-8") as f:
            data = json.load(f)
            return data.get("version", "0.0.0")
    except:
        return "0.0.0"

def get_latest_tag():
    try:
        return subprocess.check_output(["git", "describe", "--tags", "--abbrev=0"], stderr=subprocess.STDOUT).decode("utf-8").strip()
    except:
        return None

def get_commits_since(tag):
    cmd = ["git", "log", f"{tag}..HEAD" if tag else "HEAD", "--pretty=format:%s"]
    try:
        output = subprocess.check_output(cmd).decode("utf-8").strip()
        return output.split("\n") if output else []
    except:
        return []

def categorize_commits(commits):
    categories = {"✨ Novidades": [], "🐛 Correções": [], "⚡ Performance": [], "🔧 Manutenção": [], "📝 Documentação": []}
    for commit in commits:
        commit = commit.strip()
        if not commit or any(commit.startswith(p) for p in ["Merge ", "chore(release)"]): continue
        
        low = commit.lower()
        if low.startswith("feat"): categories["✨ Novidades"].append(commit)
        elif low.startswith("fix"): categories["🐛 Correções"].append(commit)
        elif low.startswith("perf"): categories["⚡ Performance"].append(commit)
        elif any(low.startswith(p) for p in ["chore", "refactor", "style", "build", "ci"]): categories["🔧 Manutenção"].append(commit)
        elif low.startswith("docs"): categories["📝 Documentação"].append(commit)
        else: categories["🔧 Manutenção"].append(commit)
    return {k: v for k, v in categories.items() if v}

def update_root_changelog(version, content):
    date_str = datetime.now().strftime("%Y-%m-%d")
    # Regex para achar a versão entre colchetes, ex: ## [3.1.1]
    version_pattern = re.compile(rf"##\s*\[{re.escape(version)}\]")
    
    formatted_content = content.replace("# ", "### ")
    new_entry = f"## [{version}] ({date_str})\n\n{formatted_content}\n\n---\n"

    if os.path.exists(ROOT_CHANGELOG):
        with open(ROOT_CHANGELOG, "r", encoding="utf-8") as f:
            full_text = f.read()

        if version_pattern.search(full_text):
            print(f"⚠️ Versão {version} já documentada. Pulando.")
            return

        # Insere após o título principal ou no topo
        if "# Changelog" in full_text:
            new_text = full_text.replace("# Changelog", f"# Changelog\n\n{new_entry}")
        else:
            new_text = f"# Changelog\n\n{new_entry}\n{full_text}"
            
        with open(ROOT_CHANGELOG, "w", encoding="utf-8") as f:
            f.write(new_text)
    else:
        with open(ROOT_CHANGELOG, "w", encoding="utf-8") as f:
            f.write(f"# Changelog\n\n{new_entry}")

def main():
    print("🚀 Gerando Changelogs...")
    version = get_current_version()
    commits = get_commits_since(get_latest_tag())
    
    categorized = categorize_commits(commits)
    content = ""
    for cat, items in categorized.items():
        content += f"# {cat}\n" + "\n".join([f"- {i}" for i in items]) + "\n\n"
    
    if not content: content = "- Melhorias de estabilidade e correções internas."
    content = content.strip()

    # Atualiza Android Raw
    os.makedirs(os.path.dirname(ANDROID_RAW_CHANGELOG), exist_ok=True)
    with open(ANDROID_RAW_CHANGELOG, "w", encoding="utf-8") as f:
        f.write(content)
    
    # Atualiza Raiz
    update_root_changelog(version, content)
    print("✅ Sincronização concluída.")

if __name__ == "__main__":
    main()
