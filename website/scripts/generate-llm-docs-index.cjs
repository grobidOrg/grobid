const fs = require('fs');
const path = require('path');
const matter = require('gray-matter');

const root = path.resolve(__dirname, '..');
const docsDir = path.resolve(root, 'docs');
const outputFile = path.resolve(root, 'src', 'generated', 'llmDocIndex.ts');

const docs = {};
collectDocs(docsDir, docsDir, docs);

const output = [
  '// Auto-generated from website/docs/*.md. Do not edit manually.',
  '',
  'export type GeneratedLlmDocRecord = {',
  '  id: string;',
  '  title: string;',
  '  description: string;',
  '  path: string;',
  '  source: string;',
  '  content: string;',
  '};',
  '',
  `export const generatedLlmDocIndex: Record<string, GeneratedLlmDocRecord> = ${JSON.stringify(docs, null, 2)};`,
  '',
].join('\n');

fs.mkdirSync(path.dirname(outputFile), {recursive: true});
fs.writeFileSync(outputFile, output, 'utf8');
console.log(`[llm-docs] Generated ${path.relative(root, outputFile)} (${Object.keys(docs).length} docs)`);

function collectDocs(dir, rootDir, docsIndex) {
  const entries = fs.readdirSync(dir, {withFileTypes: true});

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);

    if (entry.isDirectory()) {
      collectDocs(fullPath, rootDir, docsIndex);
      continue;
    }

    if (!entry.name.endsWith('.md') && !entry.name.endsWith('.mdx')) {
      continue;
    }

    const raw = fs.readFileSync(fullPath, 'utf8');
    const {data: frontMatter, content} = matter(raw);
    const relativePath = path.relative(rootDir, fullPath).replace(/\\/g, '/');
    const id = relativePath.replace(/\.mdx?$/, '');
    const title = typeof frontMatter.title === 'string' && frontMatter.title.trim().length > 0
      ? frontMatter.title.trim()
      : path.basename(fullPath, path.extname(fullPath));
    const description = typeof frontMatter.description === 'string' ? frontMatter.description.trim() : '';

    docsIndex[id] = {
      id,
      title,
      description,
      path: normalizeDocPath(id, frontMatter.slug),
      source: `docs/${relativePath}`,
      content: content.trim().replace(/\r\n/g, '\n'),
    };
  }
}

function normalizeDocPath(id, slug) {
  if (typeof slug === 'string' && slug.trim().length > 0) {
    return slug.trim();
  }

  if (id === 'intro') {
    return '/';
  }

  if (id.endsWith('/index')) {
    return `/${id.slice(0, -'/index'.length)}`;
  }

  return `/${id}`;
}
