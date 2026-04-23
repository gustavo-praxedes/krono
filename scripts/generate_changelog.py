import subprocess
import os
import sys

# Garante que a saída do console aceite UTF-8
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Caminhos dos arquivos
ANDROID_RAW_CHANGELOG = "app/src/main/res/raw/changelog.md"
ROOT_CHANGELOG = "CHANGELOG.md"

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
        if not commit or commit.startswith("Merge "): continue
        
        lower_commit = commit.lower()
        # Mantemos o prefixo para que o parser do Android identifique o ícone individualmente se necessário
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
            # Sem prefixo vai para manutenção mas sem adicionar prefixo falso
            categories["🔧 Manutenção"].append(commit)
            
    return {k: v for k, v in categories.items() if v}

def generate_markdown(categorized):
    text = ""
    for cat, items in categorized.items():
        text += f"# {cat}\n"
        for item in items:
            # Garante que o item comece com hífen
            line = item.strip()
            if not line.startswith("-"): line = f"- {line}"
            text += f"{line}\n"
        text += "\n"
    return text.strip() if text else "- Melhorias de estabilidade e correções internas."

def update_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

def main():
    print("🚀 Gerando Changelogs sincronizados...")
    
    latest_tag = get_latest_tag()
    commits = get_commits_since(latest_tag)
    
    if not commits:
        print("⚠️ Nenhum commit novo encontrado desde a última tag.")
        return

    categorized = categorize_commits(commits)
    content = generate_markdown(categorized)
    
    # Atualiza o arquivo interno do App
    update_file(ANDROID_RAW_CHANGELOG, content)
    
    # Para o CHANGELOG da raiz, vamos apenas imprimir para o usuário anexar ou 
    # poderíamos fazer um append, mas por segurança de histórico, 
    # vamos atualizar o raw que é o mais crítico para o app.
    
    print(f"✅ {ANDROID_RAW_CHANGELOG} atualizado.")
    print("\n--- CONTEÚDO GERADO ---\n")
    print(content)
    print("\n-----------------------\n")

if __name__ == "__main__":
    main()
