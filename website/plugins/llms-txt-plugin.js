const fs = require('fs');
const path = require('path');
const matter = require('gray-matter');

module.exports = function llmsTxtPlugin(context) {
  return {
    name: 'llms-txt-plugin',

    async postBuild({outDir, content}) {
      const docsDir = path.join(context.siteDir, 'docs');
      const baseUrl = context.siteConfig.baseUrl;
      const siteUrl = context.siteConfig.url;

      const pages = [];
      collectDocs(docsDir, docsDir, pages);
      pages.sort((a, b) => a.path.localeCompare(b.path));

      const llmsTxt = generateLlmsTxt(pages, siteUrl, baseUrl);
      fs.writeFileSync(path.join(outDir, 'llms.txt'), llmsTxt, 'utf-8');

      const llmsFullTxt = generateLlmsFullTxt(pages);
      fs.writeFileSync(path.join(outDir, 'llms-full.txt'), llmsFullTxt, 'utf-8');

      console.log(`[llms-txt] Generated llms.txt (${pages.length} pages) and llms-full.txt`);
    },
  };
};

function collectDocs(dir, rootDir, pages) {
  const entries = fs.readdirSync(dir, {withFileTypes: true});
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      collectDocs(fullPath, rootDir, pages);
    } else if (entry.name.endsWith('.md') || entry.name.endsWith('.mdx')) {
      const raw = fs.readFileSync(fullPath, 'utf-8');
      const {data: frontmatter, content} = matter(raw);
      const relPath = path.relative(rootDir, fullPath).replace(/\\/g, '/');
      const slug = frontmatter.slug ||
        '/' + relPath
          .replace(/\/index\.md(x)?$/, '')
          .replace(/\.mdx?$/, '');

      pages.push({
        path: slug,
        title: frontmatter.title || path.basename(fullPath, path.extname(fullPath)),
        description: frontmatter.description || '',
        content: content.trim(),
      });
    }
  }
}

function generateLlmsTxt(pages, siteUrl, baseUrl) {
  const lines = [
    '# GROBID',
    '> Machine learning library for extracting structured data from scholarly PDFs.',
    '',
    '## About',
    'GROBID (GeneRation Of BIbliographic Data) is a machine learning tool for',
    'extracting, parsing, and restructuring raw documents (PDF) into structured',
    'TEI-encoded XML. It is designed for technical and scientific publications.',
    '',
    '## Documentation',
    '',
  ];

  const sections = {
    'Getting Started': [],
    'Docker Guides': [],
    'API Guides': [],
    'Training Guides': [],
    'Configuration & Troubleshooting': [],
    'Reference': [],
    'Explanation': [],
    'Community': [],
  };

  for (const page of pages) {
    const url = `${siteUrl}${baseUrl}${page.path.replace(/^\//, '')}`;
    const entry = `- [${page.title}](${url}): ${page.description}`;

    if (page.path.startsWith('/getting-started')) sections['Getting Started'].push(entry);
    else if (page.path.startsWith('/guides/docker')) sections['Docker Guides'].push(entry);
    else if (page.path.startsWith('/guides/api')) sections['API Guides'].push(entry);
    else if (page.path.startsWith('/guides/training')) sections['Training Guides'].push(entry);
    else if (page.path.startsWith('/guides/')) sections['Configuration & Troubleshooting'].push(entry);
    else if (page.path.startsWith('/reference')) sections['Reference'].push(entry);
    else if (page.path.startsWith('/explanation')) sections['Explanation'].push(entry);
    else if (page.path.startsWith('/community')) sections['Community'].push(entry);
    else sections['Getting Started'].unshift(entry);
  }

  for (const [heading, entries] of Object.entries(sections)) {
    if (entries.length > 0) {
      lines.push(`### ${heading}`);
      lines.push(...entries);
      lines.push('');
    }
  }

  return lines.join('\n');
}

function generateLlmsFullTxt(pages) {
  const lines = [
    '# GROBID - Complete Documentation',
    '',
    'Machine learning library for extracting structured data from scholarly PDFs.',
    '',
  ];

  for (const page of pages) {
    lines.push(`---`);
    lines.push(`## ${page.title}`);
    lines.push(`Path: ${page.path}`);
    if (page.description) {
      lines.push(`Description: ${page.description}`);
    }
    lines.push('');
    lines.push(page.content);
    lines.push('');
  }

  return lines.join('\n');
}
