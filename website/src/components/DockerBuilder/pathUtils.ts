export function winPathToWsl(raw: string): string {
  let p = raw.trim();
  if (!p) return '';
  if (p.startsWith('/mnt/')) return p.replace(/\/+$/, '');
  if (p.startsWith('~/') || p === '~') return p;
  p = p.replace(/\\/g, '/');
  p = p.replace(/\/+/g, '/');
  p = p.replace(/\/+$/, '');
  const driveMatch = p.match(/^([A-Za-z]):\/?(.*?)$/);
  if (driveMatch) {
    const drive = driveMatch[1].toLowerCase();
    const rest = driveMatch[2].replace(/^\/+/, '').replace(/\/+$/, '');
    return rest ? `/mnt/${drive}/${rest}` : `/mnt/${drive}`;
  }
  return p;
}

export function winPathNative(raw: string): string {
  let p = raw.trim();
  if (!p) return '';
  p = p.replace(/\/+$/, '').replace(/\\+$/, '');
  if (p.startsWith('/mnt/') && p.length >= 6) {
    const drive = p[5].toUpperCase();
    const rest = p.slice(6);
    const cleaned = rest.replace(/\//g, '\\').replace(/^\\+/, '').replace(/\\+/g, '\\');
    return cleaned ? `${drive}:\\${cleaned}` : `${drive}:\\`;
  }
  if (p.match(/^[A-Za-z]:[/\\]/)) {
    const drive = p[0].toUpperCase();
    const rest = p.slice(2).replace(/\//g, '\\').replace(/\\+/g, '\\');
    return `${drive}:${rest}`;
  }
  if (p.match(/^[A-Za-z]:$/)) {
    return `${p[0].toUpperCase()}:\\`;
  }
  return p;
}
