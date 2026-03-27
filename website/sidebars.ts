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
        'guides/docker/docker-production',
      ],
    },
    {
      type: 'category',
      label: 'Explanation',
      collapsed: true,
      items: [
        'explanation/architecture',
        'explanation/deep-learning',
        'explanation/ml-pipeline',
        'explanation/design-principles',
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
    {
      type: 'category',
      label: 'Advanced Workflows',
      collapsed: true,
      items: [
        'guides/training/training-overview',
        'guides/training/training-workflow',
        'guides/training/model-selection',
        'guides/training/evaluation',
        'guides/training/annotation-guidelines',
      ],
    },
    'guides/configuration',
    'guides/performance-tuning',
    'guides/troubleshooting',
    {
      type: 'category',
      label: 'Community',
      collapsed: true,
      items: [
        'community/license',
        'community/references',
      ],
    },
  ],

  referenceSidebar: [
    'reference/api-endpoints',
    'reference/configuration-reference',
    'reference/tei-output-format',
    'reference/coordinates',
    'reference/processing-flavors',
    {
      type: 'category',
      label: 'Model Reference',
      collapsed: true,
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
      label: 'Archive (Legacy & Historical)',
      collapsed: true,
      items: [
        'reference/archive/grobid-batch',
        'reference/archive/grobid-java-library',
        'reference/archive/notes-grobid-developers',
        'reference/archive/recompiling-crf-libraries',
        {
          type: 'category',
          label: 'Historical Benchmarks',
          collapsed: true,
          items: [
            'reference/archive/benchmarks/overview',
            'reference/archive/benchmarks/pmc',
            'reference/archive/benchmarks/biorxiv',
            'reference/archive/benchmarks/plos',
            'reference/archive/benchmarks/elife',
            'reference/archive/benchmarks/models',
            {
              type: 'category',
              label: 'Flavor Benchmarks',
              collapsed: true,
              items: [
                'reference/archive/benchmarks/flavors/article-light-pmc',
                'reference/archive/benchmarks/flavors/article-light-biorxiv',
                'reference/archive/benchmarks/flavors/article-light-plos',
                'reference/archive/benchmarks/flavors/article-light-elife',
                'reference/archive/benchmarks/flavors/article-light-ref-pmc',
                'reference/archive/benchmarks/flavors/article-light-ref-biorxiv',
                'reference/archive/benchmarks/flavors/article-light-ref-plos',
                'reference/archive/benchmarks/flavors/article-light-ref-elife',
              ],
            },
          ],
        },
      ],
    },
  ],
};

export default sidebars;
