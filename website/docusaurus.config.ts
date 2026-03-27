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
const tlumaSource = process.env.DOCS_TLUMA_SOURCE ?? discussionRepo;
const giscusRepo = process.env.DOCS_GISCUS_REPO ?? discussionRepo;
const giscusRepoId = process.env.DOCS_GISCUS_REPO_ID ?? 'R_kgDORx5luw';
const giscusCategory = process.env.DOCS_GISCUS_CATEGORY ?? 'Announcements';
const giscusCategoryId = process.env.DOCS_GISCUS_CATEGORY_ID ?? 'DIC_kwDORx5lu84C5Ytr';
const giscusMapping = process.env.DOCS_GISCUS_MAPPING ?? 'og:title';
const giscusThemeDark = process.env.DOCS_GISCUS_THEME_DARK ?? 'noborder_dark';
const giscusThemeLight = process.env.DOCS_GISCUS_THEME_LIGHT ?? 'light';

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

  customFields: {
    giscus: {
      repo: giscusRepo,
      repoId: giscusRepoId,
      category: giscusCategory,
      categoryId: giscusCategoryId,
      mapping: giscusMapping,
      themeDark: giscusThemeDark,
      themeLight: giscusThemeLight,
    },
    tluma: {
      source: tlumaSource,
      theme: process.env.DOCS_TLUMA_THEME ?? 'auto',
      brandColor: process.env.DOCS_TLUMA_BRAND_COLOR ?? 'blue',
      button: process.env.DOCS_TLUMA_BUTTON ?? 'bottom-right',
      welcomePulse: (process.env.DOCS_TLUMA_WELCOME_PULSE ?? 'true') === 'true',
      edgePadding: process.env.DOCS_TLUMA_EDGE_PADDING ?? '1rem',
      autoOpen: (process.env.DOCS_TLUMA_AUTO_OPEN ?? 'false') === 'true',
      desktopFullscreenByDefault: (process.env.DOCS_TLUMA_DESKTOP_FULLSCREEN ?? 'false') === 'true',
      prefillStarterPrompt: process.env.DOCS_TLUMA_PREFILL_PROMPT ?? 'How do I set up GROBID with Docker, and if that fails, what should I check first?',
    },
  },

  headTags: [
    {
      tagName: 'script',
      attributes: {},
      innerHTML: `window.tlumaConfig = ${JSON.stringify({
        source: tlumaSource,
        theme: process.env.DOCS_TLUMA_THEME ?? 'auto',
        brandColor: process.env.DOCS_TLUMA_BRAND_COLOR ?? 'blue',
        button: process.env.DOCS_TLUMA_BUTTON ?? 'bottom-right',
        welcomePulse: (process.env.DOCS_TLUMA_WELCOME_PULSE ?? 'true') === 'true',
        edgePadding: process.env.DOCS_TLUMA_EDGE_PADDING ?? '1rem',
        autoOpen: (process.env.DOCS_TLUMA_AUTO_OPEN ?? 'false') === 'true',
        desktopFullscreenByDefault: (process.env.DOCS_TLUMA_DESKTOP_FULLSCREEN ?? 'false') === 'true',
        prefillStarterPrompt: process.env.DOCS_TLUMA_PREFILL_PROMPT ?? 'How do I set up GROBID with Docker, and if that fails, what should I check first?',
      })};`,
    },
    {
      tagName: 'script',
      attributes: {
        src: 'https://tluma.ai/widget.js',
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
    docs: {
      sidebar: {
        hideable: true,
        autoCollapseCategories: false,
      },
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
