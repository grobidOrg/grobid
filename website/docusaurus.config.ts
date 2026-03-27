import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const githubOwner = process.env.DOCS_GITHUB_OWNER ?? 'VooDisss';
const githubRepo = process.env.DOCS_GITHUB_REPO ?? 'grobid';
const docsBranch = process.env.DOCS_BRANCH ?? 'docs_V2_docusaurus';
const siteUrl = process.env.DOCS_SITE_URL ?? `https://${githubOwner}.github.io`;
const siteBaseUrl = process.env.DOCS_BASE_URL ?? `/${githubRepo}/`;
const githubRepoUrl = `https://github.com/${githubOwner}/${githubRepo}`;
const editBaseUrl = `${githubRepoUrl}/tree/${docsBranch}/website/`;
const discussionRepo = `${githubOwner}/${githubRepo}`;

const config: Config = {
  title: 'GROBID Documentation',
  tagline: 'Machine learning library for extracting structured data from scholarly PDFs',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  url: siteUrl,
  baseUrl: siteBaseUrl,

  organizationName: githubOwner,
  projectName: githubRepo,
  trailingSlash: false,

  onBrokenLinks: 'warn',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  headTags: [
    {
      tagName: 'script',
      attributes: {
        src: 'https://widget.tluma.ai/widget.js',
        'data-repo': discussionRepo,
        async: 'true',
        defer: 'true',
      },
    },
  ],

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  plugins: [
    './plugins/llms-txt-plugin',
  ],

  themes: [
    [
      require.resolve('@easyops-cn/docusaurus-search-local'),
      {
        hashed: true,
        docsRouteBasePath: '/',
        indexBlog: false,
      },
    ],
  ],

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          routeBasePath: '/',
          editUrl: editBaseUrl,
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/grobid-social-card.png',
    colorMode: {
      defaultMode: 'dark',
      respectPrefersColorScheme: false,
    },
    navbar: {
      title: 'GROBID',
      logo: {
        alt: 'GROBID Logo',
        src: 'img/logo.svg',
        href: '/',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          type: 'docSidebar',
          sidebarId: 'referenceSidebar',
          position: 'left',
          label: 'Reference',
        },
        {
          href: githubRepoUrl,
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Getting Started',
          items: [
            {label: 'Quick Start (Docker)', to: '/getting-started/quickstart-docker'},
            {label: 'Build from Source', to: '/getting-started/quickstart-local'},
            {label: 'Your First Extraction', to: '/getting-started/your-first-extraction'},
          ],
        },
        {
          title: 'Guides',
          items: [
            {label: 'Docker Setup', to: '/guides/docker/docker-setup'},
            {label: 'REST API', to: '/guides/api/rest-api-usage'},
            {label: 'Troubleshooting', to: '/guides/troubleshooting'},
          ],
        },
        {
          title: 'Community',
          items: [
            {label: 'GitHub Issues', href: `${githubRepoUrl}/issues`},
            {label: 'GitHub Discussions', href: `${githubRepoUrl}/discussions`},
            {label: 'License', to: '/community/license'},
            {label: 'Cite GROBID', to: '/community/references'},
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} GROBID Contributors. Licensed under Apache 2.0.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['bash', 'java', 'python', 'markup', 'yaml', 'json'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
