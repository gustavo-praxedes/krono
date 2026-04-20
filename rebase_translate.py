import sys
import os

commit_msg_file = sys.argv[1]

with open(commit_msg_file, 'r', encoding='utf-8') as f:
    msg = f.read()

translations = {
    "feat(ui): modernize UI/UX to Pro Max standards, automate releases and enhance update system": "feat(ui): modernizar UI/UX para padrões Pro Max, automatizar releases e melhorar sistema de atualização",
    "fix(automation): correct apk path in release workflow": "fix(automação): corrigir caminho do apk no workflow de release",
    "feat: implement internal auto-update system and refine 'What's New' UI": "feat: implementar sistema interno de auto-atualização e(refinar UI de Novidades"
}

for old, new in translations.items():
    msg = msg.replace(old, new)

with open(commit_msg_file, 'w', encoding='utf-8') as f:
    f.write(msg)

print(f"Mensagem traduzida: {msg}")