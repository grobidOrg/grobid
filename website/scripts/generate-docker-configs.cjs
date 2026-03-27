const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const sourceDir = path.resolve(root, '..', 'grobid-home', 'config');
const outputFile = path.resolve(root, 'src', 'generated', 'dockerBaseConfigs.ts');

const crfPath = path.join(sourceDir, 'grobid.yaml');
const fullPath = path.join(sourceDir, 'grobid-full.yaml');

const crf = fs.readFileSync(crfPath, 'utf8');
const full = fs.readFileSync(fullPath, 'utf8');

const content = `// Auto-generated from ../grobid-home/config/*.yaml. Do not edit manually.\n` +
  `export const GROBID_CRF_BASE_CONFIG: string = ${JSON.stringify(crf)};\n` +
  `export const GROBID_FULL_BASE_CONFIG: string = ${JSON.stringify(full)};\n`;

fs.mkdirSync(path.dirname(outputFile), { recursive: true });
fs.writeFileSync(outputFile, content, 'utf8');
console.log(`[docker-configs] Generated ${path.relative(root, outputFile)}`);
