import React, { useEffect, useMemo, useRef, useState } from 'react';
import sidebars from '@site/sidebars';
import { generatedLlmDocIndex, type GeneratedLlmDocRecord } from '@site/src/generated/llmDocIndex';
import styles from './styles.module.css';

type SidebarCategoryItem = {
  type: 'category';
  label: string;
  items: SidebarTreeItem[];
};

type SidebarDocItem = {
  type: 'doc';
  id: string;
  doc: GeneratedLlmDocRecord;
};

type SidebarTreeItem = SidebarCategoryItem | SidebarDocItem;

type CopyDoc = GeneratedLlmDocRecord & {
  location: string[];
};

type Props = {
  currentDocId?: string;
};

type SidebarConfig = Record<string, unknown>;

const sidebarLabelOverrides: Record<string, string> = {
  docsSidebar: 'Documentation',
  referenceSidebar: 'Reference',
};

export default function LlmCopySelector({ currentDocId }: Props): React.JSX.Element {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(() => new Set());
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [floatingPosition, setFloatingPosition] = useState<{ right: number; bottom: number } | null>(null);
  const closeTimeoutRef = useRef<number | undefined>(undefined);
  const panelRef = useRef<HTMLElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);

  const tree = useMemo(() => buildSidebarTree(sidebars as SidebarConfig, generatedLlmDocIndex), []);
  const orderedDocs = useMemo(() => flattenDocs(tree), [tree]);
  const rootStyle = useMemo(() => {
    if (floatingPosition === null) {
      return undefined;
    }

    return {
      '--llm-copy-selector-right': `${floatingPosition.right}px`,
      '--llm-copy-selector-bottom': `${floatingPosition.bottom}px`,
    } as React.CSSProperties;
  }, [floatingPosition]);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        window.clearTimeout(closeTimeoutRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    const handlePointerDown = (event: MouseEvent | TouchEvent) => {
      const target = event.target;
      if (!(target instanceof Node)) {
        return;
      }

      if (panelRef.current?.contains(target) || triggerRef.current?.contains(target)) {
        return;
      }

      setIsOpen(false);
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('touchstart', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('touchstart', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  useEffect(() => {
    const syncWithAskAiButton = () => {
      const askAiButton = findAskAiButton();

      if (!askAiButton) {
        setFloatingPosition(null);
        return;
      }

      const rect = askAiButton.getBoundingClientRect();
      const gap = -10;
      const right = Math.max(window.innerWidth - rect.left + gap, 8);
      const bottom = Math.max(window.innerHeight - rect.bottom, 8);

      setFloatingPosition((previous) => {
        if (
          previous !== null &&
          Math.abs(previous.right - right) < 1 &&
          Math.abs(previous.bottom - bottom) < 1
        ) {
          return previous;
        }

        return { right, bottom };
      });
    };

    syncWithAskAiButton();

    const intervalId = window.setInterval(syncWithAskAiButton, 1000);
    window.addEventListener('resize', syncWithAskAiButton);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('resize', syncWithAskAiButton);
    };
  }, []);

  const selectedDocs = useMemo(
    () => orderedDocs.filter((doc) => selectedIds.has(doc.id)),
    [orderedDocs, selectedIds],
  );

  const compiledText = useMemo(() => buildCopyPayload(selectedDocs), [selectedDocs]);
  const characterCount = compiledText.length;
  const tokenEstimate = Math.ceil(characterCount / 4);

  const setSelection = (docIds: string[], checked: boolean) => {
    setSelectedIds((previous) => {
      const next = new Set(previous);

      docIds.forEach((docId) => {
        if (checked) {
          next.add(docId);
        } else {
          next.delete(docId);
        }
      });

      return next;
    });
  };

  const handleCopy = async () => {
    if (!compiledText) {
      setStatusMessage('Select at least one page to copy.');
      return;
    }

    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(compiledText);
      } else {
        copyWithFallback(compiledText);
      }

      setStatusMessage(`Copied ${selectedDocs.length} page${selectedDocs.length === 1 ? '' : 's'} for LLM use.`);
      if (closeTimeoutRef.current) {
        window.clearTimeout(closeTimeoutRef.current);
      }
      closeTimeoutRef.current = window.setTimeout(() => setStatusMessage(''), 2400);
    } catch {
      setStatusMessage('Copy failed. Try again in a secure browser context.');
    }
  };

  return (
    <div className={styles.root} style={rootStyle}>
      {isOpen ? (
        <section ref={panelRef} className={styles.panel} aria-label="Copy docs for LLM panel">
          <div className={styles.header}>
            <p className={styles.subtitle}>Pick docs or whole categories, then copy a clean markdown-like bundle.</p>
            <button
              type="button"
              className={styles.closeButton}
              aria-label="Close copy panel"
              onClick={() => setIsOpen(false)}>
              x
            </button>
          </div>

          <div className={styles.actions}>
            <button
              type="button"
              className={styles.actionButton}
              onClick={() => setSelectedIds(new Set(orderedDocs.map((doc) => doc.id)))}>
              Select all
            </button>
            <button
              type="button"
              className={styles.actionButton}
              onClick={() => setSelectedIds(new Set())}>
              Clear
            </button>
            <button
              type="button"
              className={styles.copyButton}
              onClick={() => void handleCopy()}
              disabled={selectedDocs.length === 0}>
              Copy selected
            </button>
          </div>

          <div className={styles.tree}>
            {tree.map((item, index) => (
              <TreeNode
                key={`${item.type}-${index}`}
                item={item}
                selectedIds={selectedIds}
                onToggle={setSelection}
              />
            ))}
          </div>

          <div className={styles.summary}>
            <div className={styles.summaryStats}>
              <span>
                <strong>{selectedDocs.length}</strong> page{selectedDocs.length === 1 ? '' : 's'}
              </span>
              <span>
                <strong>{characterCount.toLocaleString()}</strong> chars
              </span>
              <span>
                <strong>{tokenEstimate.toLocaleString()}</strong> tokens
              </span>
            </div>
            {statusMessage ? <div className={styles.status}>{statusMessage}</div> : null}
          </div>
        </section>
      ) : null}

      {!isOpen ? (
        <button
          ref={triggerRef}
          type="button"
          className={styles.trigger}
          aria-label="Copy docs for LLM"
          aria-expanded={isOpen}
          onClick={() => setIsOpen((open) => !open)}>
          <span className={styles.triggerIcon} aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false">
              <path d="M9 3h8a2 2 0 0 1 2 2v10h-2V5H9V3Zm-4 4h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2Zm0 2v10h8V9H5Z" fill="currentColor" />
            </svg>
          </span>
          <span className={styles.triggerLabel}>Copy for LLM</span>
        </button>
      ) : null}
    </div>
  );
}

function TreeNode({
  item,
  selectedIds,
  onToggle,
}: {
  item: SidebarTreeItem;
  selectedIds: Set<string>;
  onToggle: (docIds: string[], checked: boolean) => void;
}): React.JSX.Element {
  if (item.type === 'doc') {
    const checked = selectedIds.has(item.id);

    return (
      <label className={styles.item}>
        <span className={styles.labelRow}>
          <input
            type="checkbox"
            className={styles.checkbox}
            checked={checked}
            onChange={(event) => onToggle([item.id], event.target.checked)}
          />
          <span className={styles.labelText}>
            <span className={styles.labelTitle}>{item.doc.title}</span>
          </span>
        </span>
      </label>
    );
  }

  const docIds = collectDocIds(item.items);
  const selectedCount = docIds.filter((docId) => selectedIds.has(docId)).length;
  const checked = docIds.length > 0 && selectedCount === docIds.length;
  const indeterminate = selectedCount > 0 && selectedCount < docIds.length;

  return (
    <div className={styles.group}>
      <CategoryCheckbox
        label={item.label}
        checked={checked}
        indeterminate={indeterminate}
        meta={`${selectedCount}/${docIds.length} pages`}
        onChange={(nextChecked) => onToggle(docIds, nextChecked)}
      />
      <div className={styles.children}>
        {item.items.map((child, index) => (
          <TreeNode
            key={`${child.type}-${index}`}
            item={child}
            selectedIds={selectedIds}
            onToggle={onToggle}
          />
        ))}
      </div>
    </div>
  );
}

function CategoryCheckbox({
  label,
  checked,
  indeterminate,
  meta,
  onChange,
}: {
  label: string;
  checked: boolean;
  indeterminate: boolean;
  meta: string;
  onChange: (checked: boolean) => void;
}): React.JSX.Element {
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.indeterminate = indeterminate;
    }
  }, [indeterminate]);

  return (
    <label className={styles.labelRow}>
      <input
        ref={inputRef}
        type="checkbox"
        className={styles.checkbox}
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
      />
      <span className={styles.labelText}>
        <span className={styles.labelTitle}>{label}</span>
        <span className={styles.labelMeta}>{meta}</span>
      </span>
    </label>
  );
}

function buildSidebarTree(
  sidebarConfig: SidebarConfig,
  docIndex: Record<string, GeneratedLlmDocRecord>,
): SidebarTreeItem[] {
  const roots: SidebarTreeItem[] = [];

  Object.entries(sidebarConfig).forEach(([sidebarId, sidebarItems]) => {
    const items = normalizeSidebarItems(sidebarItems, docIndex);
    if (items.length === 0) {
      return;
    }

    roots.push({
      type: 'category',
      label: sidebarLabelOverrides[sidebarId] ?? humanizeSidebarLabel(sidebarId),
      items,
    });
  });

  return roots;
}

function normalizeSidebarItems(
  items: unknown,
  docIndex: Record<string, GeneratedLlmDocRecord>,
): SidebarTreeItem[] {
  if (!Array.isArray(items)) {
    return [];
  }

  return items.flatMap((item) => normalizeSidebarItem(item, docIndex));
}

function normalizeSidebarItem(
  item: unknown,
  docIndex: Record<string, GeneratedLlmDocRecord>,
): SidebarTreeItem[] {
  if (typeof item === 'string') {
    const doc = docIndex[item];
    return doc ? [{ type: 'doc', id: item, doc }] : [];
  }

  if (!item || typeof item !== 'object') {
    return [];
  }

  const sidebarItem = item as { type?: string; label?: string; items?: unknown; id?: string };

  if ((sidebarItem.type === 'doc' || sidebarItem.type === 'ref') && typeof sidebarItem.id === 'string') {
    const doc = docIndex[sidebarItem.id];
    return doc ? [{ type: 'doc', id: sidebarItem.id, doc }] : [];
  }

  if (sidebarItem.type === 'category' && typeof sidebarItem.label === 'string') {
    const childItems = normalizeSidebarItems(sidebarItem.items, docIndex);
    return childItems.length > 0 ? [{ type: 'category', label: sidebarItem.label, items: childItems }] : [];
  }

  return [];
}

function flattenDocs(tree: SidebarTreeItem[]): CopyDoc[] {
  const docs: CopyDoc[] = [];
  const seen = new Set<string>();

  const visit = (item: SidebarTreeItem, parents: string[]) => {
    if (item.type === 'doc') {
      if (!seen.has(item.id)) {
        seen.add(item.id);
        docs.push({ ...item.doc, location: parents });
      }
      return;
    }

    item.items.forEach((child) => visit(child, [...parents, item.label]));
  };

  tree.forEach((item) => visit(item, []));
  return docs;
}

function collectDocIds(items: SidebarTreeItem[]): string[] {
  const ids: string[] = [];

  items.forEach((item) => {
    if (item.type === 'doc') {
      ids.push(item.id);
      return;
    }

    ids.push(...collectDocIds(item.items));
  });

  return Array.from(new Set(ids));
}

function buildCopyPayload(docs: CopyDoc[]): string {
  if (docs.length === 0) {
    return '';
  }

  const lines: string[] = [
    '# GROBID docs export for LLM context',
    '',
    `Selected pages: ${docs.length}`,
    '',
  ];

  docs.forEach((doc) => {
    lines.push('---');
    lines.push(`## ${doc.title}`);
    lines.push(`Path: ${doc.path}`);

    if (doc.location.length > 0) {
      lines.push(`Location: ${doc.location.join(' > ')}`);
    }

    if (doc.description) {
      lines.push(`Description: ${doc.description}`);
    }

    lines.push(`Source: ${doc.source}`);
    lines.push('');
    lines.push(doc.content);
    lines.push('');
  });

  return lines.join('\n').trim();
}

function humanizeSidebarLabel(value: string): string {
  return value
    .replace(/Sidebar$/u, '')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/^./u, (match) => match.toUpperCase());
}

function copyWithFallback(value: string) {
  const textarea = document.createElement('textarea');
  textarea.value = value;
  textarea.setAttribute('readonly', 'true');
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

function findAskAiButton(): HTMLButtonElement | null {
  const roots: ParentNode[] = [document];

  while (roots.length > 0) {
    const root = roots.shift();
    if (!root) {
      continue;
    }

    const directMatch = root.querySelector?.('button.tluma-button');
    if (directMatch instanceof HTMLButtonElement) {
      return directMatch;
    }

    const buttons = Array.from(root.querySelectorAll?.('button') ?? []);
    for (const button of buttons) {
      if ((button.textContent || '').trim() === 'Ask AI' && button instanceof HTMLButtonElement) {
        return button;
      }
    }

    const elements = Array.from(root.querySelectorAll?.('*') ?? []);
    for (const element of elements) {
      const shadowRoot = (element as Element & { shadowRoot?: ShadowRoot | null }).shadowRoot;
      if (shadowRoot) {
        roots.push(shadowRoot);
      }
    }
  }

  return null;
}
