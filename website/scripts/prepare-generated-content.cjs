const path = require('path');
const {execFileSync} = require('child_process');

const scripts = [
  'generate-docker-configs.cjs',
  'generate-llm-docs-index.cjs',
];

for (const script of scripts) {
  execFileSync(process.execPath, [path.join(__dirname, script)], {
    stdio: 'inherit',
  });
}
