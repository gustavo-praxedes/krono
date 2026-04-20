import subprocess
import sys
import os
import shutil

os.chdir('E:/KRONO/krono')

subprocess.run(['git', 'checkout', 'main'], capture_output=True)
subprocess.run(['git', 'reset', '--hard', 'e040307'], capture_output=True)

commits = [
    ('4c337a1', 'feat(ui): modernize UI/UX to Pro Max standards, automate releases and enhance update system', 'feat(ui): modernizar UI/UX para padrões Pro Max, automatizar releases e melhorar sistema de atualização'),
    ('d647640', 'fix(automation): correct apk path in release workflow', 'fix(automação): corrigir caminho do apk no workflow de release'),
]

for old_hash, old_msg, new_msg in commits:
    subprocess.run(['git', 'cherry-pick', old_hash, '--no-commit'], capture_output=True)
    subprocess.run(['git', 'commit', '-m', new_msg], capture_output=True)

result = subprocess.run(['git', 'log', '--oneline', '-5'], capture_output=True, text=True)
print(result.stdout)