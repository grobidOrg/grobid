import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/index',
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
        'guides/docker/docker-production',
        'guides/docker/docker-troubleshooting',
      ],
    },
    {
      type: 'category',
      label: 'Using the API',
      items: [
        'guides/api/rest-api-usage',
        'guides/api/python-client',
        'guides/api/java-client',
        'guides/api/batch-processing',
        'guides/api/consolidation',
      ],
    },
    {
      type: 'category',
      label: 'Training Models',
      items: [
        'guides/training/training-overview',
        'guides/training/annotation-guidelines',
        'guides/training/training-workflow',
        'guides/training/model-selection',
        'guides/training/evaluation',
      ],
    },
    'guides/configuration',
    'guides/performance-tuning',
    'guides/troubleshooting',
    {
      type: 'category',
      label: 'Understanding GROBID',
      items: [
        'explanation/architecture',
        'explanation/ml-pipeline',
        'explanation/deep-learning',
        'explanation/tei-rationale',
        'explanation/design-principles',
      ],
    },
    {
      type: 'category',
      label: 'Community',
      items: [
        'community/faq',
        'community/contributing',
        'community/references',
        'community/license',
      ],
    },
  ],

  referenceSidebar: [
    'reference/api-endpoints',
    'reference/configuration-reference',
    'reference/tei-output-format',
    'reference/coordinates',
    'reference/processing-flavors',
    'reference/platform-compatibility',
    {
      type: 'category',
      label: 'Model Annotation',
      items: [
        'reference/models/segmentation',
        'reference/models/header',
        'reference/models/fulltext',
        'reference/models/citation',
        'reference/models/affiliation',
        'reference/models/date',
      ],
    },
    {
      type: 'category',
      label: 'Benchmarks',
      items: [
        'reference/benchmarks/overview',
        'reference/benchmarks/pmc',
        'reference/benchmarks/plos',
        'reference/benchmarks/biorxiv',
        'reference/benchmarks/elife',
      ],
    },
  ],
};

export default sidebars;
