import React, {useState, useMemo, useRef, useEffect} from 'react';
import styles from './styles.module.css';
import {
  LABELS, OS_LABELS, IMAGE_LABELS, GPU_LABELS, CONSOL_LABELS, SHELL_LABELS,
  OPTION_LABELS, MEMORY_HINTS, FLAG_HINTS, PILL_TIPS, TOOLTIPS, ALERTS,
} from './i18n';

import { winPathToWsl, winPathNative } from './pathUtils';
import { GROBID_CRF_BASE_CONFIG, GROBID_FULL_BASE_CONFIG } from '../../generated/dockerBaseConfigs';

type OS = 'windows-wsl2' | 'linux-x86' | 'linux-arm64' | 'macos-as' | 'macos-intel';
type Image = 'standard' | 'full';
type GPU = 'none' | 'nvidia';
type ConsolService = 'none' | 'crossref' | 'glutton';
type Shell = 'wsl-bash' | 'powershell' | 'cmd';

const OS_ORDER: OS[] = ['windows-wsl2', 'linux-x86', 'linux-arm64', 'macos-as', 'macos-intel'];
const CONSOL_ORDER: ConsolService[] = ['none', 'glutton', 'crossref'];
const SHELL_ORDER: Shell[] = ['powershell', 'cmd', 'wsl-bash'];
const STORAGE_KEY = 'grobid-docker-builder-v1';

export default function DockerBuilder(): React.ReactElement {
  const [os, setOs] = useState<OS>('windows-wsl2');
  const [image, setImage] = useState<Image>('standard');
  const [gpu, setGpu] = useState<GPU>('none');
  const [memory, setMemory] = useState(8);
  const [port, setPort] = useState(8070);
  const [mountPdfs, setMountPdfs] = useState(false);
  const [autoRemove, setAutoRemove] = useState(false);
  const [detach, setDetach] = useState(false);
  const [allocTty, setAllocTty] = useState(true);
  const [nonRoot, setNonRoot] = useState(false);
  const [adminPort, setAdminPort] = useState(false);
  const [containerName, setContainerName] = useState('');
  const [consolService, setConsolService] = useState<ConsolService>('none');
  const [gluttonUrl, setGluttonUrl] = useState('http://host.docker.internal:8080');
  const [crossrefEmail, setCrossrefEmail] = useState('');
  const [hostPath, setHostPath] = useState('');
  const [configPath, setConfigPath] = useState('');
  const [pdfsPath, setPdfsPath] = useState('');
  const [shell, setShell] = useState<Shell>('powershell');
  const [hydrated, setHydrated] = useState(false);
  const [resetTick, setResetTick] = useState(0);

  const keepPathTailVisible = (el: HTMLInputElement | null) => {
    if (!el) return;
    requestAnimationFrame(() => {
      el.scrollLeft = el.scrollWidth;
    });
  };

  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        setHydrated(true);
        return;
      }
      const data = JSON.parse(raw);
      if (data.os) setOs(data.os);
      if (data.image) setImage(data.image);
      if (data.gpu) setGpu(data.gpu);
      if (typeof data.memory === 'number') setMemory(data.memory);
      if (typeof data.port === 'number') setPort(data.port);
      if (typeof data.mountPdfs === 'boolean') setMountPdfs(data.mountPdfs);
      if (typeof data.autoRemove === 'boolean') setAutoRemove(data.autoRemove);
      if (typeof data.detach === 'boolean') setDetach(data.detach);
      if (typeof data.allocTty === 'boolean') setAllocTty(data.allocTty);
      if (typeof data.nonRoot === 'boolean') setNonRoot(data.nonRoot);
      if (typeof data.adminPort === 'boolean') setAdminPort(data.adminPort);
      if (typeof data.containerName === 'string') setContainerName(data.containerName);
      if (data.consolService) setConsolService(data.consolService);
      if (typeof data.gluttonUrl === 'string') setGluttonUrl(data.gluttonUrl);
      if (typeof data.crossrefEmail === 'string') setCrossrefEmail(data.crossrefEmail);
      if (typeof data.hostPath === 'string') setHostPath(data.hostPath);
      if (typeof data.configPath === 'string') setConfigPath(data.configPath);
      if (typeof data.pdfsPath === 'string') setPdfsPath(data.pdfsPath);
      if (data.shell) setShell(data.shell);
    } catch {
      // ignore malformed storage
    } finally {
      setHydrated(true);
    }
  }, [resetTick]);

  useEffect(() => {
    if (!hydrated || typeof window === 'undefined') return;
    const data = {
      os, image, gpu, memory, port,
      mountPdfs, autoRemove, detach, allocTty, nonRoot,
      adminPort, containerName,
      consolService, gluttonUrl, crossrefEmail,
      hostPath, configPath, pdfsPath, shell,
    };
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch {
      // ignore storage errors
    }
  }, [hydrated, os, image, gpu, memory, port, mountPdfs, autoRemove, detach, allocTty, nonRoot, adminPort, containerName, consolService, gluttonUrl, crossrefEmail, hostPath, configPath, pdfsPath, shell]);

  const resetAll = () => {
    if (typeof window !== 'undefined') {
      try {
        window.localStorage.removeItem(STORAGE_KEY);
      } catch {
        // ignore
      }
    }
    setOs('windows-wsl2');
    setImage('standard');
    setGpu('none');
    setMemory(8);
    setPort(8070);
    setMountPdfs(false);
    setAutoRemove(false);
    setDetach(false);
    setAllocTty(true);
    setNonRoot(false);
    setAdminPort(false);
    setContainerName('');
    setConsolService('none');
    setGluttonUrl('http://host.docker.internal:8080');
    setCrossrefEmail('');
    setHostPath('');
    setConfigPath('');
    setPdfsPath('');
    setShell('powershell');
    setResetTick((v) => v + 1);
  };

  const isArm = os === 'macos-as' || os === 'linux-arm64';
  const isMac = os === 'macos-as' || os === 'macos-intel';
  const isWindows = os === 'windows-wsl2';
  const gpuDisabled = isMac || image === 'standard';
  const dlBrokenOnArm = isArm && image === 'full';
  const needsConfigMount = consolService !== 'none';

  const useWslPaths = isWindows && shell === 'wsl-bash';
  const useNativePaths = isWindows && shell !== 'wsl-bash';
  const lineCont = (isWindows && shell === 'powershell') ? ' `' : ' \\';
  const imageTag = image === 'full' ? 'lfoppiano/grobid:latest-full' : 'lfoppiano/grobid:latest-crf';

  const resolveOne = useMemo(() => {
    return (raw: string) => {
      if (!raw) return raw;
      if (!isWindows) return raw;
      return shell === 'wsl-bash' ? winPathToWsl(raw) : winPathNative(raw);
    };
  }, [isWindows, shell]);

  const resolveBase = useMemo(() => {
    if (!isWindows) {
      const base = hostPath || '~';
      return (suffix: string) => `${base}/${suffix}`;
    }
    if (shell === 'wsl-bash') {
      const base = hostPath ? winPathToWsl(hostPath) : '/mnt/c/Users/YOU';
      return (suffix: string) => `${base}/${suffix}`;
    }
    const base = hostPath ? winPathNative(hostPath) : 'C:\\Users\\YOU';
    return (suffix: string) => `${base}\\${suffix}`;
  }, [isWindows, shell, hostPath]);

  const volPdfs = pdfsPath ? resolveOne(pdfsPath) : resolveBase('pdfs');
  const volConfig = configPath ? `${resolveOne(configPath).replace(/[/\\]+$/, '')}${isWindows && shell !== 'wsl-bash' ? '\\' : '/'}grobid.yaml` : resolveBase('config/grobid.yaml');

  const useGpu = gpu === 'nvidia' && !gpuDisabled;
  const isFull = image === 'full';

  const commandLines = useMemo(() => {
    let base = 'docker run';
    if (detach) base += ' -d';
    if (allocTty && !detach) base += ' -t';
    if (autoRemove) base += ' --rm';
    base += ' --init --ulimit core=0';
    const p: string[] = [base];
    if (containerName) p.push(`  --name ${containerName}`);
    if (isArm) p.push('  --platform linux/amd64');
    if (useGpu) p.push('  --gpus all');
    if (useGpu) p.push("  -e TF_FORCE_GPU_ALLOW_GROWTH='true'");
    p.push(`  -p ${port}:8070`);
    if (adminPort) p.push('  -p 8071:8071');
    p.push(`  --memory=${memory}g`);
    if (mountPdfs) p.push(`  -v ${volPdfs}:/opt/grobid/input`);
    if (needsConfigMount) p.push(`  -v ${volConfig}:/opt/grobid/grobid-home/config/grobid.yaml:ro`);
    if (nonRoot && !isWindows) p.push('  --user $(id -u):$(id -g)');
    p.push(`  ${imageTag}`);
    return p;
  }, [os, image, gpu, memory, port, mountPdfs, needsConfigMount, nonRoot, autoRemove, detach, allocTty, adminPort, containerName, isArm, useGpu, gpuDisabled, volPdfs, volConfig, imageTag]);

  const command = commandLines.map((l, i) =>
    i < commandLines.length - 1 ? l + lineCont : l
  ).join('\n');

  const prevLinesRef = useRef<Set<string>>(new Set(commandLines));
  const [newLines, setNewLines] = useState<Set<string>>(new Set());

  useEffect(() => {
    const prev = prevLinesRef.current;
    const added = new Set<string>();
    for (const line of commandLines) {
      if (!prev.has(line)) added.add(line);
    }
    setNewLines(added);
    prevLinesRef.current = new Set(commandLines);
    if (added.size > 0) {
      const timer = setTimeout(() => setNewLines(new Set()), 800);
      return () => clearTimeout(timer);
    }
  }, [commandLines]);

  const yamlSnippet = useMemo(() => {
    if (consolService === 'none') return '';

    let yaml = image === 'full' ? GROBID_FULL_BASE_CONFIG : GROBID_CRF_BASE_CONFIG;

    yaml = yaml.replace(/^(\s*service:\s*")[^"]*("\s*)$/m, `$1${consolService}$2`);

    if (consolService === 'glutton') {
      yaml = yaml.replace(/^(\s*url:\s*")[^"]*("\s*)$/m, `$1${gluttonUrl}$2`);
    }

    if (consolService === 'crossref') {
      yaml = yaml.replace(/^(\s*mailto:\s*).*$/m, `$1${crossrefEmail ? `"${crossrefEmail}"` : ''}`);
    }

    return yaml;
  }, [image, consolService, gluttonUrl, crossrefEmail]);

  const configDir = useMemo(() => {
    const idx = Math.max(volConfig.lastIndexOf('/'), volConfig.lastIndexOf('\\'));
    return idx >= 0 ? volConfig.slice(0, idx) : '.';
  }, [volConfig]);

  const createConfigCommand = useMemo(() => {
    if (shell === 'powershell') {
      return `New-Item -ItemType Directory -Force -Path \"${configDir}\"; New-Item -ItemType File -Force -Path \"${volConfig}\"`;
    }
    if (shell === 'cmd') {
      return `if not exist \"${configDir}\" mkdir \"${configDir}\" && if not exist \"${volConfig}\" type nul > \"${volConfig}\"`;
    }
    return `mkdir -p \"${configDir}\" && touch \"${volConfig}\"`;
  }, [shell, configDir, volConfig]);

  const openConfigCommand = useMemo(() => {
    if (shell === 'powershell') {
      return `notepad \"${volConfig}\"`;
    }
    if (shell === 'cmd') {
      return `notepad \"${volConfig}\"`;
    }
    return `nano \"${volConfig}\"`;
  }, [shell, volConfig]);

  const activeHints = useMemo(() => {
    const h: [string, string][] = [];
    if (detach) h.push(['-d', FLAG_HINTS['-d']]);
    if (allocTty && !detach) h.push(['-t', FLAG_HINTS['-t']]);
    if (autoRemove) h.push(['--rm', FLAG_HINTS['--rm']]);
    h.push(['--init', FLAG_HINTS['--init']]);
    h.push(['--ulimit core=0', FLAG_HINTS['--ulimit core=0']]);
    if (containerName) h.push(['--name', FLAG_HINTS['--name']]);
    if (isArm) h.push(['--platform', FLAG_HINTS['--platform']]);
    if (useGpu) {
      h.push(['--gpus all', FLAG_HINTS['--gpus all']]);
      h.push(['-e TF_...', FLAG_HINTS['-e TF_...']]);
    }
    if (adminPort) h.push(['-p 8071', FLAG_HINTS['-p 8071']]);
    h.push(['--memory', FLAG_HINTS['--memory']]);
    if (mountPdfs) h.push(['-v (pdfs)', FLAG_HINTS['-v (pdfs)']]);
    if (needsConfigMount) h.push(['-v (config)', FLAG_HINTS['-v (config)']]);
    if (nonRoot && !isWindows) h.push(['--user', FLAG_HINTS['--user']]);
    return h;
  }, [isArm, useGpu, autoRemove, detach, allocTty, adminPort, containerName, mountPdfs, needsConfigMount, nonRoot, isWindows]);

  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard.writeText(command);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  const [copiedYaml, setCopiedYaml] = useState(false);
  const copyYaml = () => {
    navigator.clipboard.writeText(yamlSnippet);
    setCopiedYaml(true);
    setTimeout(() => setCopiedYaml(false), 2000);
  };
  const [copiedCreate, setCopiedCreate] = useState(false);
  const copyCreate = () => {
    navigator.clipboard.writeText(createConfigCommand);
    setCopiedCreate(true);
    setTimeout(() => setCopiedCreate(false), 2000);
  };
  const [copiedOpen, setCopiedOpen] = useState(false);
  const copyOpen = () => {
    navigator.clipboard.writeText(openConfigCommand);
    setCopiedOpen(true);
    setTimeout(() => setCopiedOpen(false), 2000);
  };
  const [copiedSave, setCopiedSave] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const saveSummary = useMemo(() => {
    const lines: string[] = [];
    lines.push('# GROBID Docker Builder (saved)');
    lines.push(`Date: ${new Date().toISOString().slice(0, 10)}`);
    lines.push('');
    lines.push('## Platform');
    lines.push(`OS: ${OS_LABELS[os]}`);
    if (isWindows) lines.push(`Shell: ${SHELL_LABELS[shell]}`);
    lines.push('');
    lines.push('## Image & Resources');
    lines.push(`Image: ${imageTag}`);
    lines.push(`GPU: ${gpuDisabled ? 'disabled' : GPU_LABELS[gpu]}`);
    lines.push(`Memory: ${memory} GB`);
    lines.push('');
    lines.push('## Options');
    lines.push(`Mount PDFs: ${mountPdfs ? 'yes' : 'no'}`);
    lines.push(`Detach: ${detach ? 'yes' : 'no'}`);
    lines.push(`TTY: ${allocTty && !detach ? 'yes' : 'no'}`);
    lines.push(`Auto-remove: ${autoRemove ? 'yes' : 'no'}`);
    lines.push(`Admin port: ${adminPort ? 'yes' : 'no'}`);
    if (containerName) lines.push(`Name: ${containerName}`);
    lines.push('');
    lines.push('## Paths');
    if (mountPdfs) lines.push(`PDFs: ${volPdfs}`);
    if (needsConfigMount) lines.push(`Config dir: ${configDir}`);
    lines.push('');
    lines.push('## Consolidation');
    lines.push(`Service: ${CONSOL_LABELS[consolService]}`);
    if (consolService === 'glutton') lines.push(`URL: ${gluttonUrl}`);
    if (consolService === 'crossref') lines.push(`Email: ${crossrefEmail || '(not set)'}`);
    lines.push('');
    lines.push('## Commands');
    if (needsConfigMount) {
      lines.push('Create file:');
      lines.push(createConfigCommand);
      lines.push('');
      lines.push('Open file:');
      lines.push(openConfigCommand);
      lines.push('');
    }
    lines.push('Docker run:');
    lines.push(command);
    return lines.join('\n');
  }, [os, isWindows, shell, imageTag, gpuDisabled, gpu, memory, mountPdfs, detach, allocTty, autoRemove, adminPort, containerName, volPdfs, needsConfigMount, configDir, consolService, gluttonUrl, crossrefEmail, createConfigCommand, openConfigCommand, command]);

  const copySave = () => {
    navigator.clipboard.writeText(saveSummary);
    setCopiedSave(true);
    setTimeout(() => setCopiedSave(false), 2000);
  };

  const closeMenu = () => setMenuOpen(false);

  useEffect(() => {
    if (!menuOpen) return;
    const handleClick = (event: MouseEvent) => {
      const target = event.target as Node;
      if (menuRef.current && !menuRef.current.contains(target)) {
        setMenuOpen(false);
      }
    };
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handleClick);
      document.removeEventListener('keydown', handleKey);
    };
  }, [menuOpen]);

  const pill = (val: string, current: string, label: string, onClick: () => void, disabled = false, rec = false, tip?: string) => (
    <button
      key={val}
      type="button"
      className={`${styles.pill} ${val === current ? styles.pillActive : ''} ${disabled ? styles.pillDisabled : ''} ${tip ? styles.pillTip : ''}`}
      onClick={disabled ? undefined : onClick}
      data-tip={tip}
    >
      {label}
      {rec && <span className={`${styles.badge} ${styles.badgeRec}`}>rec</span>}
    </button>
  );

  const tog = (label: string, active: boolean, onChange: () => void, tip?: string) => (
    <button
      key={label}
      type="button"
      className={`${styles.toggle} ${active ? styles.toggleActive : ''} ${tip ? styles.pillTip : ''}`}
      onClick={onChange}
      data-tip={tip}
    >
      <span className={styles.toggleCheck}>{active ? '\u2713' : '\u2015'}</span>
      {label}
    </button>
  );

  const memHint = memory < 4 ? MEMORY_HINTS.tooLow
    : memory <= 8 ? MEMORY_HINTS.singleDocs
    : memory <= 16 ? MEMORY_HINTS.batchOk
    : MEMORY_HINTS.production;

  const alerts: React.ReactNode[] = [];

  if (dlBrokenOnArm) alerts.push(
    <div key="dl-arm" className={`${styles.alert} ${styles.alertDanger}`}>
      <span className={styles.alertIcon}>{'\u26D4'}</span>
      <p><strong>{ALERTS.dlArm.title}</strong> {ALERTS.dlArm.body}</p>
    </div>
  );
  if (os === 'macos-as' && !dlBrokenOnArm) alerts.push(
    <div key="mac-perf" className={`${styles.alert} ${styles.alertWarn}`}>
      <span className={styles.alertIcon}>{'\u26A0'}</span>
      <p><strong>{ALERTS.macPerf.title}</strong> {ALERTS.macPerf.body}</p>
    </div>
  );
  if (isWindows) alerts.push(
    <div key="wsl" className={`${styles.alert} ${styles.alertInfo}`}>
      <span className={styles.alertIcon}>{'\u2139'}</span>
      <p><strong>{ALERTS.wsl.title}</strong> {ALERTS.wsl.body} <span className={styles.wslLink} data-tip={ALERTS.wsl.wslTooltip}>What is WSL2?</span></p>
    </div>
  );
  if (isWindows && gpu === 'nvidia') alerts.push(
    <div key="wsl-gpu" className={`${styles.alert} ${styles.alertInfo}`}>
      <span className={styles.alertIcon}>{'\u2139'}</span>
      <p>{ALERTS.wslGpu.body} <a href="../docker-gpu">{ALERTS.wslGpu.linkText}</a></p>
    </div>
  );
  if (os === 'linux-arm64') alerts.push(
    <div key="arm" className={`${styles.alert} ${styles.alertWarn}`}>
      <span className={styles.alertIcon}>{'\u26A0'}</span>
      <p><strong>{ALERTS.arm64.title}</strong> {ALERTS.arm64.body}</p>
    </div>
  );
  if (memory < 4) alerts.push(
    <div key="mem" className={`${styles.alert} ${styles.alertDanger}`}>
      <span className={styles.alertIcon}>{'\u26D4'}</span>
      <p><strong>{ALERTS.memory.title}</strong> {ALERTS.memory.body}</p>
    </div>
  );

  return (
    <div className={styles.builder}>
      <div className={styles.row}>
        <span className={styles.rowLabel}>{LABELS.os}</span>
        <div className={styles.pills}>
          {OS_ORDER.map((val) => pill(val, os, OS_LABELS[val], () => setOs(val), false, false, PILL_TIPS[val]))}
        </div>
        <div className={styles.menuWrap} ref={menuRef}>
          <button
            type="button"
            className={styles.menuBtn}
            onClick={() => setMenuOpen((v) => !v)}
            aria-label="Builder menu"
          >
            ...
          </button>
          {menuOpen && (
            <div className={styles.menu}>
              <button type="button" className={styles.menuItem} onClick={() => { copySave(); closeMenu(); }}>
                {copiedSave ? 'Saved' : 'Save config'}
              </button>
              <button type="button" className={styles.menuItemDanger} onClick={() => { resetAll(); closeMenu(); }}>
                Reset
              </button>
            </div>
          )}
        </div>
      </div>

      {isWindows && (
        <div className={`${styles.row} ${styles.rowNoWrap}`}>
          <span className={styles.rowLabel}>Shell</span>
          <div className={styles.shellPills}>
            {SHELL_ORDER.map((s) => (
              <button
                key={s}
                type="button"
                className={`${styles.shellPill} ${shell === s ? styles.shellPillActive : ''}`}
                onClick={() => setShell(s)}
              >
                {SHELL_LABELS[s]}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className={styles.row}>
        <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.imageLabel}>{LABELS.image}</span>
        <div className={styles.pills}>
          {pill('standard', image, IMAGE_LABELS.standard, () => { setImage('standard'); setGpu('none'); }, false, true, PILL_TIPS['image-standard'])}
          {pill('full', image, IMAGE_LABELS.full, () => setImage('full'), false, false, PILL_TIPS['image-full'])}
        </div>
      </div>

      <div className={styles.row}>
        <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.gpuLabel}>{LABELS.gpu}</span>
        <div className={styles.pills}>
          {pill('none', gpu, GPU_LABELS.none, () => setGpu('none'), false, true, PILL_TIPS['gpu-none'])}
          {pill('nvidia', gpu, GPU_LABELS.nvidia, () => setGpu('nvidia'), gpuDisabled, false, PILL_TIPS['gpu-nvidia'])}
        </div>
      </div>

      <div className={styles.row}>
        <span className={styles.rowLabel}>{LABELS.memory}</span>
        <div className={styles.sliderRow}>
          <input type="range" min={2} max={32} step={1} value={memory} onChange={(e) => setMemory(Number(e.target.value))} className={styles.slider} />
          <span className={`${styles.sliderVal} ${memory < 4 ? styles.sliderWarn : ''}`}>{memory} GB</span>
          <span className={styles.sliderHint}>{memHint}</span>
        </div>
      </div>

      <div className={styles.row}>
        <span className={styles.rowLabel}>{LABELS.options}</span>
        <div className={styles.toggles}>
          {tog(OPTION_LABELS.mountPdfs, mountPdfs, () => setMountPdfs(!mountPdfs), PILL_TIPS.mountPdfs)}
          {tog(OPTION_LABELS.autoRemove, autoRemove, () => setAutoRemove(!autoRemove), PILL_TIPS.autoRemove)}
          {tog(OPTION_LABELS.detach, detach, () => { const next = !detach; setDetach(next); if (next) setAllocTty(false); }, PILL_TIPS.detach)}
          {tog(OPTION_LABELS.allocTty, allocTty, () => { const next = !allocTty; setAllocTty(next); if (next) setDetach(false); }, PILL_TIPS.allocTty)}
          {!isWindows && tog(OPTION_LABELS.nonRoot, nonRoot, () => setNonRoot(!nonRoot), PILL_TIPS.nonRoot)}
        </div>
      </div>

      {mountPdfs && (
        <div className={`${styles.row} ${styles.rowNoWrap}`}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.pdfsPathLabel}>PDFs</span>
          <div className={styles.inlineField}>
            <input type="text" value={pdfsPath} onChange={(e) => setPdfsPath(e.target.value)} onBlur={(e) => keepPathTailVisible(e.currentTarget)} ref={keepPathTailVisible} className={`${styles.textInput} ${styles.pathInput}`}
              placeholder={isWindows ? 'C:\\Users\\YOU\\pdfs' : '~/pdfs'} />
          </div>
        </div>
      )}

      <div className={styles.row}>
        <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.consolLabel}>
          {LABELS.consolLabel}
        </span>
        <div className={styles.pills}>
          {CONSOL_ORDER.map((val) => pill(val, consolService, CONSOL_LABELS[val], () => setConsolService(val), false, false, PILL_TIPS[`consol-${val}`]))}
        </div>
      </div>

      {consolService === 'glutton' && (
        <div className={`${styles.row} ${styles.rowNoWrap}`}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.gluttonUrlTitle}>{LABELS.gluttonUrl}</span>
          <div className={styles.inlineField}>
            <input type="text" value={gluttonUrl} onChange={(e) => setGluttonUrl(e.target.value)} className={styles.textInput} placeholder="http://host.docker.internal:8080" />
          </div>
        </div>
      )}

      {consolService === 'crossref' && (
        <div className={styles.row}>
          <span className={styles.rowLabel}></span>
          <div className={styles.inlineField}>
            <label className={`${styles.fieldLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.emailLabel}>{LABELS.email}</label>
            <input type="email" value={crossrefEmail} onChange={(e) => setCrossrefEmail(e.target.value)} className={styles.textInput} placeholder="you@example.com (for polite API pool)" />
          </div>
        </div>
      )}

      {needsConfigMount && (
        <div className={`${styles.row} ${styles.rowNoWrap}`}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.configDirLabel}>Config</span>
          <div className={styles.inlineField}>
            <input
              type="text"
              value={configPath}
              onChange={(e) => setConfigPath(e.target.value)}
              className={`${styles.textInput} ${styles.pathInput}`}
              onBlur={(e) => keepPathTailVisible(e.currentTarget)}
              ref={keepPathTailVisible}
              placeholder={isWindows ? 'C:\\Users\\YOU\\grobid\\config' : '~/grobid/config'}
            />
          </div>
        </div>
      )}

      <div className={styles.advanced}>
        <div className={styles.advancedHeader}>{LABELS.advanced}</div>
        <div className={styles.row}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.portLabel}>{LABELS.port}</span>
          <input type="number" min={1024} max={65535} value={port} onChange={(e) => setPort(Number(e.target.value))} className={styles.textInput} style={{width: 80}} />
        </div>
        <div className={styles.row}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.adminPortLabel}>Admin</span>
          <div className={styles.toggles}>
            {tog('Expose 8071 (health)', adminPort, () => setAdminPort(!adminPort))}
          </div>
        </div>
        <div className={styles.row}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.nameLabel}>Name</span>
          <input type="text" value={containerName} onChange={(e) => setContainerName(e.target.value.replace(/[^a-zA-Z0-9_.-]/g, ''))} className={styles.textInput} style={{width: 150}} placeholder="grobid" />
        </div>
        {!(!mountPdfs || pdfsPath) && <div className={`${styles.row} ${styles.rowNoWrap}`}>
          <span className={`${styles.rowLabel} ${styles.rowLabelTip}`} data-tip={TOOLTIPS.hostPathLabel}>{LABELS.hostPath}</span>
          <input
            type="text"
            value={hostPath}
            onChange={(e) => setHostPath(e.target.value)}
            className={`${styles.textInput} ${styles.pathInput}`}
            onBlur={(e) => keepPathTailVisible(e.currentTarget)}
            ref={keepPathTailVisible}
            placeholder={isWindows ? LABELS.hostPathPlaceholderWin : LABELS.hostPathPlaceholderUnix}
          />
          {(isWindows && shell === 'wsl-bash') && (
            <span className={styles.sliderHint}>{LABELS.hostPathHintWsl}</span>
          )}
          {(!isWindows) && (
            <span className={styles.sliderHint}>{LABELS.hostPathHintBase}</span>
          )}
        </div>}
      </div>

      {needsConfigMount && (
        <div className={`${styles.alert} ${styles.alertWarn}`}>
          <span className={styles.alertIcon}>{'\u26A0'}</span>
          <p><strong>Step 1 first</strong> — create and save the config file below before running Docker. If the host file does not exist, Docker may mount a directory instead and GROBID will fail to start.</p>
        </div>
      )}

      {needsConfigMount && (
        <div className={styles.commandBlock} style={{marginTop: '0.5rem'}}>
          <div className={styles.commandHeader}>
            <span>Step 1 - Prepare this file: <code className={styles.headerPath}>{volConfig}</code></span>
          </div>
          <div className={styles.flagHints}>
            <span className={styles.flagHintsLabel}>Create file:</span>
            <span className={styles.flagHint} data-tip="Creates the parent directory if needed, then creates an empty file at the exact path the Docker command mounts."><code>{createConfigCommand}</code></span>
            <button className={styles.copyBtn} onClick={copyCreate} type="button">{copiedCreate ? LABELS.copied : LABELS.copy}</button>
          </div>
          <div className={styles.flagHints}>
            <span className={styles.flagHintsLabel}>Open file:</span>
            <span className={styles.flagHint} data-tip="Opens the config file in a simple editor so you can paste or edit the generated YAML."><code>{openConfigCommand}</code></span>
            <button className={styles.copyBtn} onClick={copyOpen} type="button">{copiedOpen ? LABELS.copied : LABELS.copy}</button>
          </div>
          <details className={styles.builderDetails}>
            <summary>
              <span>Show full generated YAML</span>
              <button className={styles.copyBtn} onClick={(e) => { e.preventDefault(); copyYaml(); }} type="button">{copiedYaml ? LABELS.copied : LABELS.copy}</button>
            </summary>
            <pre className={styles.commandPre}><code>{yamlSnippet}</code></pre>
          </details>
        </div>
      )}

      <div className={styles.commandBlock}>
        <div className={styles.commandHeader}>
          <span>{needsConfigMount ? 'Step 2 - ' : ''}{LABELS.yourCommand}</span>
          <div className={styles.commandHeaderRight}>
            <button className={styles.copyBtn} onClick={copy} type="button">{copied ? LABELS.copied : LABELS.copy}</button>
          </div>
        </div>
        <pre className={styles.commandPre}><code>{commandLines.map((line, i) => {
          const sep = i < commandLines.length - 1 ? lineCont + '\n' : '\n';
          const isNew = newLines.has(line);
          return (
            <span key={line} className={`${styles.commandLine} ${isNew ? styles.commandLineNew : ''}`}>{line}{sep}</span>
          );
        })}</code></pre>
        <div className={styles.flagHints}>
          <span className={styles.flagHintsLabel}>{LABELS.flags}</span>
          {activeHints.map(([flag, hint]) => (
            <span key={flag} className={styles.flagHint} data-tip={hint}><code>{flag}</code></span>
          ))}
        </div>
      </div>

      {alerts}
    </div>
  );
}
