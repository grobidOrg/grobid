import { winPathToWsl, winPathNative } from './pathUtils';

const cases: {
  desc: string;
  input: string;
  wsl: string;
  native: string;
}[] = [
  {
    desc: 'forward slash windows path',
    input: 'C:/ZoteroPDFs/GLM_OCR_output',
    wsl: '/mnt/c/ZoteroPDFs/GLM_OCR_output',
    native: 'C:\\ZoteroPDFs\\GLM_OCR_output',
  },
  {
    desc: 'backslash windows path with trailing slash',
    input: 'C:\\ZoteroPDFs\\GLM_OCR_output\\',
    wsl: '/mnt/c/ZoteroPDFs/GLM_OCR_output',
    native: 'C:\\ZoteroPDFs\\GLM_OCR_output',
  },
  {
    desc: 'backslash windows path no trailing',
    input: 'C:\\Users\\John\\Documents',
    wsl: '/mnt/c/Users/John/Documents',
    native: 'C:\\Users\\John\\Documents',
  },
  {
    desc: 'lowercase drive letter',
    input: 'd:\\data\\pdfs',
    wsl: '/mnt/d/data/pdfs',
    native: 'D:\\data\\pdfs',
  },
  {
    desc: 'drive root only with backslash',
    input: 'C:\\',
    wsl: '/mnt/c',
    native: 'C:\\',
  },
  {
    desc: 'drive root only no slash',
    input: 'C:',
    wsl: '/mnt/c',
    native: 'C:\\',
  },
  {
    desc: 'drive root with forward slash',
    input: 'D:/',
    wsl: '/mnt/d',
    native: 'D:\\',
  },
  {
    desc: 'already WSL path passthrough',
    input: '/mnt/c/Users/John',
    wsl: '/mnt/c/Users/John',
    native: 'C:\\Users\\John',
  },
  {
    desc: 'already WSL path with trailing slash',
    input: '/mnt/d/data/',
    wsl: '/mnt/d/data',
    native: 'D:\\data',
  },
  {
    desc: 'unix home tilde',
    input: '~',
    wsl: '~',
    native: '~',
  },
  {
    desc: 'unix home tilde with subpath',
    input: '~/Documents/pdfs',
    wsl: '~/Documents/pdfs',
    native: '~/Documents/pdfs',
  },
  {
    desc: 'empty string',
    input: '',
    wsl: '',
    native: '',
  },
  {
    desc: 'whitespace only',
    input: '   ',
    wsl: '',
    native: '',
  },
  {
    desc: 'path with spaces',
    input: 'C:\\Program Files\\GROBID data',
    wsl: '/mnt/c/Program Files/GROBID data',
    native: 'C:\\Program Files\\GROBID data',
  },
  {
    desc: 'mixed separators',
    input: 'C:\\Users/John\\Documents/pdfs',
    wsl: '/mnt/c/Users/John/Documents/pdfs',
    native: 'C:\\Users\\John\\Documents\\pdfs',
  },
  {
    desc: 'double backslashes',
    input: 'C:\\\\Users\\\\John',
    wsl: '/mnt/c/Users/John',
    native: 'C:\\Users\\John',
  },
  {
    desc: 'whitespace padding',
    input: '  C:\\Users\\John  ',
    wsl: '/mnt/c/Users/John',
    native: 'C:\\Users\\John',
  },
  {
    desc: 'forward slash windows with trailing',
    input: 'E:/research/papers/',
    wsl: '/mnt/e/research/papers',
    native: 'E:\\research\\papers',
  },
  {
    desc: 'WSL mnt root only',
    input: '/mnt/c',
    wsl: '/mnt/c',
    native: 'C:\\',
  },
  {
    desc: 'single depth subdir',
    input: 'C:\\grobid',
    wsl: '/mnt/c/grobid',
    native: 'C:\\grobid',
  },
];

let passed = 0;
let failed = 0;

for (const c of cases) {
  const gotWsl = winPathToWsl(c.input);
  const gotNative = winPathNative(c.input);

  if (gotWsl !== c.wsl) {
    console.error(`FAIL [WSL] "${c.desc}": input="${c.input}" expected="${c.wsl}" got="${gotWsl}"`);
    failed++;
  } else {
    passed++;
  }

  if (gotNative !== c.native) {
    console.error(`FAIL [NAT] "${c.desc}": input="${c.input}" expected="${c.native}" got="${gotNative}"`);
    failed++;
  } else {
    passed++;
  }
}

console.log(`\nResults: ${passed} passed, ${failed} failed out of ${cases.length * 2} assertions`);
if (failed > 0) process.exit(1);
