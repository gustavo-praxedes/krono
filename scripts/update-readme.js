'use strict';
const fs = require('fs');

const changelog = fs.readFileSync('CHANGELOG.md', 'utf8');
const readme    = fs.readFileSync('README.md', 'utf8');

// Extrai apenas o bloco da versão mais recente do CHANGELOG
const match = changelog.match(/#{2,3} \[[\d.]+\][\s\S]+?(?=#{2,3} \[[\d.]+\]|$)/);
if (!match) process.exit(0);

const latestBlock = match[0].trim();

// Substitui o bloco entre as marcações no README
const updated = readme.replace(
    /\[\/\/\]: # \(CHANGELOG_LATEST_START\)[\s\S]*?\[\/\/\]: # \(CHANGELOG_LATEST_END\)/,
    `[//]: # (CHANGELOG_LATEST_START)\n\n${latestBlock}\n\n[//]: # (CHANGELOG_LATEST_END)`
);

fs.writeFileSync('README.md', updated);
console.log('README.md atualizado com a última versão do CHANGELOG.');