# No final do arquivo generate_changelog.py, altere a função de escrita:

def main():
    # ... (restante do código) ...

    new_section = build_section(version, grouped, latest_tag)
    existing = load_existing()

    # GARANTA QUE A ESCRITA ESTEJA ASSIM:
    with open(CHANGELOG_FILE, "w", encoding="utf-8") as f:
        if version_already_in_changelog(version, existing):
            updated = update_section(version, new_section, existing)
            f.write(updated)
        else:
            f.write(insert_new_version(new_section, existing))

    print("✅ CHANGELOG.md atualizado com sucesso (UTF-8).")