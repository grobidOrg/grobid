import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/quickstart-docker',
        'getting-started/quickstart-local',
        'getting-started/your-first-extraction',
        'getting-started/understanding-the-output',
        'getting-started/next-steps',
      ],
    },
    {
      type: 'category',
      label: 'Docker',
      items: [
        'guides/docker/docker-setup',
        'guides/docker/docker-gpu',
        'guides/docker/docker-platforms',
        'guides/docker/docker-troubleshooting',
      ],
    },
    {
      type: 'category',
      label: 'Using the API',
      items: [
        'guides/api/rest-api-usage',
        'guides/api/python-client',
        'guides/api/batch-processing',
        'guides/api/consolidation',
      ],
    },
    'guides/configuration',
    'guides/performance-tuning',
    'guides/troubleshooting',
  ],

  referenceSidebar: [
    'reference/api-endpoints',
    'reference/configuration-reference',
    'reference/tei-output-format',
    'reference/coordinates',
    'reference/processing-flavors',
  ],
};

export default sidebars;
