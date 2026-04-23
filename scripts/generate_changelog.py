import subprocess
import os
import sys
import json
from datetime import datetime

# Garante que a saída do console aceite UTF-8
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Caminhos dos arquivos
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
    if tag:
        cmd = ["git", "log", f"{tag}..HEAD", "--pretty=format:%s"]
    else:
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
        if not commit or commit.startswith("Merge ") or commit.startswith("chore(release)"): continue
        
        lower_commit = commit.lower()
        if lower_commit.startswith("feat"):
            categories["✨ Novidades"].append(commit)
        elif lower_commit.startswith("fix"):
            categories["🐛 Correções"].append(commit)
        elif lower_commit.startswith("perf"):
            categories["⚡ Performance"].append(commit)
        elif any(lower_commit.startswith(p) for p in ["chore", "refactor", "style", "build", "ci"]):
            categories["🔧 Manutenção"].append(commit)
        elif lower_commit.startswith("docs"):
            categories["📝 Documentação"].append(commit)
        else:
            categories["🔧 Manutenção"].append(commit)
            
    return {k: v for k, v in categories.items() if v}

def generate_markdown_content(categorized):
    text = ""
    for cat, items in categorized.items():
        text += f"# {cat}\n"
        for item in items:
            line = item.strip()
            if not line.startswith("-"): line = f"- {line}"
            text += f"{line}\n"
        text += "\n"
    return text.strip()

def update_root_changelog(version, content):
    date_str = datetime.now().strftime("%Y-%m-%d")
    header = f"## [{version}] ({date_str})\n\n"
    
    # Transforma os títulos # em ### para o changelog da raiz
    formatted_content = content.replace("# ", "### ")
    new_entry = header + formatted_content + "\n\n---\n\n"
    
    if os.path.exists(ROOT_CHANGELOG):
        with open(ROOT_CHANGELOG, "r", encoding="utf-8") as f:
            lines = f.readlines()
        
        # Procura onde inserir (após o título principal)
        output = []
        inserted = False
        for line in lines:
            output.append(line)
            if "# Changelog" in line and not inserted:
                output.append("\n" + new_entry)
                inserted = True
        
        # Se não achou o título, coloca no começo
        if not inserted:
            output = ["# Changelog\n\n", new_entry] + lines
            
        with open(ROOT_CHANGELOG, "w", encoding="utf-8") as f:
            f.writelines(output)
    else:
        with open(ROOT_CHANGELOG, "w", encoding="utf-8") as f:
            f.write("# Changelog\n\n" + new_entry)

def main():
    print("🚀 Gerando Changelogs (Local e Raiz)...")
    
    version = get_current_version()
    latest_tag = get_latest_tag()
    commits = get_commits_since(latest_tag)
    
    if not commits:
        print("⚠️ Nenhum commit novo encontrado. Usando mensagem padrão.")
        content = "- Melhorias de estabilidade e correções internas."
    else:
        categorized = categorize_commits(commits)
        content = generate_markdown_content(categorized)
        if not content:
            content = "- Melhorias de estabilidade e correções internas."

    # 1. Atualiza o arquivo interno do App (res/raw)
    os.makedirs(os.path.dirname(ANDROID_RAW_CHANGELOG), exist_ok=True)
    with open(ANDROID_RAW_CHANGELOG, "w", encoding="utf-8") as f:
        f.write(content)
    
    # 2. Atualiza o CHANGELOG.md da raiz
    update_root_changelog(version, content)
    
    print(f"✅ {ANDROID_RAW_CHANGELOG} atualizado.")
    print(f"✅ {ROOT_CHANGELOG} atualizado.")

if __name__ == "__main__":
    main()
