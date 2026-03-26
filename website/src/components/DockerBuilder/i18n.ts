export const LABELS = {
  os: 'OS',
  image: 'Image',
  gpu: 'GPU',
  memory: 'Memory',
  options: 'Options',
  consolLabel: 'Consol.',
  advanced: 'Advanced',
  port: 'Port',
  hostPath: 'Base path',
  gluttonUrl: 'Glutton URL',
  email: 'Email',
  yourCommand: 'Your command',
  copy: 'Copy',
  copied: '\u2713 Copied',
  flags: 'Flags:',
  saveAs: 'Save as',
  hostPathPlaceholderWin: 'C:\\Users\\YOU  or  /mnt/c/Users/YOU (auto-convert)',
  hostPathPlaceholderUnix: '~',
  hostPathHintWsl: 'Windows paths auto-convert to WSL /mnt/ format',
  hostPathHintNative: '',
  hostPathHintBase: 'Base path for all volume mounts',
};

export const OS_LABELS: Record<string, string> = {
  'windows-wsl2': 'Windows WSL2',
  'linux-x86': 'Linux x86_64',
  'linux-arm64': 'Linux ARM64',
  'macos-as': 'macOS Apple Silicon',
  'macos-intel': 'macOS Intel',
};

export const IMAGE_LABELS: Record<string, string> = {
  standard: 'CRF (lightweight)',
  full: 'Full (CRF + Deep Learning)',
};

export const GPU_LABELS: Record<string, string> = {
  none: 'CPU only',
  nvidia: 'NVIDIA',
};

export const CONSOL_LABELS: Record<string, string> = {
  none: 'None',
  glutton: 'Biblio-Glutton',
  crossref: 'CrossRef',
};

export const SHELL_LABELS: Record<string, string> = {
  'wsl-bash': 'WSL Bash',
  'powershell': 'PowerShell',
  'cmd': 'CMD',
};

export const OPTION_LABELS: Record<string, string> = {
  mountPdfs: 'Mount PDFs',
  autoRemove: 'Auto-remove (--rm)',
  detach: 'Detach (-d)',
  allocTty: 'Pretty logs (-t)',
  nonRoot: 'Non-root',
};

export const MEMORY_HINTS: Record<string, string> = {
  tooLow: 'too low',
  singleDocs: 'single docs',
  batchOk: 'batch OK',
  production: 'production',
};

export const FLAG_HINTS: Record<string, string> = {
  '--rm': 'Auto-remove container on exit',
  '--init': 'Clean Ctrl+C shutdown, prevents zombie pdfalto processes',
  '--ulimit core=0': 'Prevents multi-GB core dumps from pdfalto C++ crashes',
  '--platform': 'Required on ARM \u2014 runs x86_64 via emulation',
  '--gpus all': 'NVIDIA GPU passthrough for DeLFT models only',
  '--memory': 'OOM kills are the #1 Docker error \u2014 set this',
  '-v (pdfs)': 'Mount local PDF folder into container for batch processing',
  '-v (config)': 'Mounts custom grobid.yaml with your consolidation settings',
  '--user': 'Non-root for security; may need permission fixes',
  '-d': 'Run in background \u2014 use "docker logs" to see output',
  '-t': 'Allocate pseudo-TTY \u2014 enables colored, formatted log output',
  '--name': 'Names the container for easy reference with docker stop/logs',
  '-e TF_...': 'Prevents TensorFlow from allocating all GPU VRAM at once',
  '-p 8071': 'Admin port \u2014 health check at /healthcheck',
};

export const PILL_TIPS: Record<string, string> = {
  'windows-wsl2': 'Docker Desktop uses WSL2 as its Linux backend. The docker command works from PowerShell, CMD, or WSL Bash.',
  'linux-x86': 'Standard Linux on Intel/AMD processors. Native Docker, best performance.',
  'linux-arm64': 'Linux on ARM64 (e.g. AWS Graviton, Raspberry Pi). CRF image has native ARM64 support.',
  'macos-as': 'Apple M1/M2/M3/M4. Docker runs via Rosetta emulation. 2-3x slower than native x86.',
  'macos-intel': 'Older Mac with Intel CPU. Docker runs natively, no emulation needed.',
  'image-standard': 'Fast, lightweight (~500 MB). Uses Wapiti CRF for all models. Works on x86 and ARM64.',
  'image-full': 'Large (~11 GB). Adds DeLFT deep learning for header, citation, affiliation. x86_64 only. GPU recommended.',
  'gpu-none': 'CRF models are CPU-only. For CRF image, GPU provides zero benefit.',
  'gpu-nvidia': 'Passes NVIDIA GPU to container for DeLFT model inference. Requires nvidia-container-toolkit on host.',
  'consol-none': 'No metadata enrichment. GROBID returns only what it extracts from the PDF.',
  'consol-glutton': 'Self-hosted bibliographic matching. Fast, free, no rate limits. You must run biblio-glutton separately.',
  'consol-crossref': 'Public CrossRef API. No setup needed but rate-limited. Provide your email for the polite pool.',
  mountPdfs: 'Mounts a host folder so GROBID can read your PDFs directly. Useful for batch processing.',
  autoRemove: 'Deletes the container when it stops. Useful for one-off runs. Do NOT enable if you want the container to persist across restarts.',
  detach: 'Runs container in background. Use "docker logs grobid" to see output, "docker stop grobid" to stop.',
  allocTty: 'Allocates a terminal for colored, formatted log output. Disable if piping output to a file.',
  nonRoot: 'Runs the process as your user instead of root. More secure but may need volume permission fixes.',
};

export const TOOLTIPS: Record<string, string> = {
  gpuLabel:
    'GPU acceleration is ONLY used by the Full (DL) image for DeLFT deep learning model inference. ' +
    'CRF models (Wapiti) are CPU-only and do not benefit from GPU at all. ' +
    'When using the Full image: without GPU, DL models run on CPU and are ~10-50x slower than CRF. ' +
    'With an NVIDIA GPU + CUDA, DL inference is fast but still slower than CRF. ' +
    'Requires NVIDIA Container Toolkit (nvidia-docker) installed on the host. ' +
    'AMD and Intel GPUs are not supported by DeLFT/TensorFlow.',
  imageLabel:
    'Two image variants are available from lfoppiano/grobid on Docker Hub. ' +
    'CRF (~500 MB): Uses Wapiti CRF models for all tasks. Fast, lightweight, works on both x86_64 and ARM64. Recommended for most users. ' +
    'Full (~11 GB): Adds DeLFT deep learning models (BidLSTM) for header, citation, affiliation, and reference-segmenter tasks. ' +
    'Better accuracy for these tasks, but requires x86_64 only (no ARM64), needs much more memory, and benefits greatly from an NVIDIA GPU. ' +
    'Models like segmentation and fulltext remain CRF even in the Full image. ' +
    'If unsure, start with CRF \u2014 you can switch to Full later without changing your data.',
  nameLabel:
    'Sets a fixed name for the container instead of Docker\'s auto-generated random name (e.g. "sleepy_hopper"). ' +
    'With a name you can easily reference the container: "docker stop grobid", "docker logs grobid", "docker start grobid". ' +
    'Especially useful with Detach mode. Only one container with a given name can exist at a time \u2014 ' +
    'remove the old one with "docker rm grobid" before creating a new one with the same name.',
  consolLabel:
    'Consolidation enriches extracted metadata by matching against external bibliographic databases. ' +
    'Header consolidation resolves DOIs and corrects titles/authors. ' +
    'Citation consolidation does the same for each reference. ' +
    'Options: Biblio-Glutton (self-hosted, fast, free) or CrossRef (public API, requires email for polite pool).',
  gluttonUrlTitle:
    'URL of the biblio-glutton service that GROBID will query for consolidation. ' +
    'Biblio-glutton and CrossRef return the same kind of bibliographic enrichment; the main difference is operational: ' +
    'biblio-glutton is typically much faster and has no public API rate limits. ' +
    'You can self-host biblio-glutton or use a remote hosted endpoint. ' +
    'When biblio-glutton runs on your own machine outside Docker, host.docker.internal lets the container reach your host.',
  emailLabel:
    'Optional but recommended. Adding a mailto puts your requests in the polite pool (higher limits + contactable). ' +
    'As of Nov 2025, polite pool allows roughly 10 req/s single (3 concurrent) and 3 req/s list (3 concurrent), ' +
    'vs public pool 5 req/s single (1 concurrent) and 1 req/s list (1 concurrent). ' +
    'Limits can change over time; check the X-Rate-Limit headers.',
  adminPortLabel:
    'GROBID exposes a second port (8071) for admin/health checks. ' +
    'Health check endpoint: http://localhost:<port>/healthcheck (returns 200 when healthy, 503 when unhealthy). ' +
    'Also available on the main port: /api/isalive (liveness), /api/version (version info), /health (detailed pool status). ' +
    'Useful for Docker health checks, load balancers, and monitoring.',
  portLabel:
    'The port on YOUR machine that maps to GROBID\'s internal port 8070. ' +
    'After starting, access GROBID at http://localhost:<this port>. ' +
    'Default 8070 works for most setups. Change it if another service already uses that port. ' +
    'On Windows, ports 8000-9000 are a safe range — unlikely to conflict with system services. ' +
    'Avoid ports below 1024 (require admin/root) and the Windows ephemeral range 49152-65535 (auto-assigned by the OS).',
  hostPathLabel:
    'Fallback base path used when individual paths (models, PDFs) are not set. ' +
    'All volume mounts default to subfolders of this path (e.g. <base>/grobid-home, <base>/pdfs). ' +
    'Windows users: paste as-is (e.g. C:\\Users\\John) — auto-converts for WSL. ' +
    'Linux/macOS: absolute path or ~ for home. Left empty = sensible default.',
  configDirLabel:
    'Host directory where the generated grobid.yaml file will live. ' +
    'The builder mounts <this directory>/grobid.yaml into the container at /opt/grobid/grobid-home/config/grobid.yaml. ' +
    'Use a normal writable folder on your machine, for example C:\\ubuntu_data\\docker_containers\\grobid\\config on Windows or ~/grobid/config on Linux/macOS.',
  pdfsPathLabel:
    'Folder on your machine containing PDF files you want GROBID to process. ' +
    'Maps to /opt/grobid/input inside the container. Read-only access is sufficient. ' +
    'Place your PDFs here, then use the batch processing API or the /api/processFulltextDocument endpoint ' +
    'with the file path pointing to /opt/grobid/input/<filename>.pdf. ' +
    'Useful for large batch jobs where uploading via HTTP is impractical.',
};

export const ALERTS = {
  dlArm: {
    title: 'Full image unavailable on ARM',
    body: '\u2014 The Full (DL) image is built for x86_64 only. No ARM64 build exists. DeLFT also requires AVX instructions unavailable under emulation. Use the CRF image or an x86_64 machine.',
  },
  macPerf: {
    title: 'Apple Silicon',
    body: '\u2014 Runs under x86_64 emulation (Rosetta). Expect 2-3x slower. Use Linux x86_64 for production.',
  },
  wsl: {
    title: 'Docker runs via WSL2',
    body: '\u2014 Docker Desktop runs all containers inside a WSL2 Linux environment. You can type the docker command in PowerShell, CMD, or WSL Bash \u2014 the container runs in WSL2 either way.',
    wslTooltip:
      'WSL2 (Windows Subsystem for Linux 2) is a real Linux kernel running inside Windows. ' +
      'Docker Desktop uses WSL2 as its backend to run Linux containers. ' +
      'You do NOT need to open a WSL terminal \u2014 the "docker" command works from PowerShell and CMD because Docker Desktop installs it system-wide. ' +
      'If Docker is not installed: download Docker Desktop from docker.com, install it, and ensure "Use WSL2 based engine" is enabled in Settings > General. ' +
      'If WSL2 itself is missing: open PowerShell as admin and run "wsl --install". Restart when prompted. ' +
      'Volume paths differ depending on which shell you run from \u2014 use the shell selector above to get the correct syntax.',
  },
  wslGpu: {
    body: 'GPU on WSL2 requires NVIDIA GPU driver for WSL2 + nvidia-container-toolkit.',
    linkText: 'GPU setup guide',
  },
  arm64: {
    title: 'ARM64',
    body: '\u2014 No native Wapiti/pdfalto binaries. Runs under x86_64 emulation. Multi-arch images are experimental.',
  },
  memory: {
    title: 'Insufficient memory',
    body: '\u2014 GROBID needs 4GB minimum. Container will be OOM-killed during model loading. Use 8GB+ for single docs, 16GB+ for batch.',
  },
  configNote: 'GROBID does not support env var config overrides. Save this file and the docker command above mounts it into the container.',
  crossrefEmail: {
    title: 'CrossRef email recommended.',
    body: 'Providing a mailto gives access to the polite pool with higher rate limits. Without it, you use the public pool.',
  },
};
