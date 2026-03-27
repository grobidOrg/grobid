import React, {type ReactNode} from 'react';
import Layout from '@theme-original/DocItem/Layout';
import type LayoutType from '@theme/DocItem/Layout';
import type {WrapperProps} from '@docusaurus/types';
import {useDoc} from '@docusaurus/plugin-content-docs/client';
import {useColorMode} from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Giscus from '@giscus/react';
import type {Mapping} from '@giscus/react';
import LlmCopySelector from '@site/src/components/LlmCopySelector';

type Props = WrapperProps<typeof LayoutType>;

export default function LayoutWrapper(props: Props): ReactNode {
  const {metadata} = useDoc();
  const {colorMode} = useColorMode();
  const {siteConfig} = useDocusaurusContext();
  const giscus = (siteConfig.customFields?.giscus as {
    repo?: string;
    repoId?: string;
    category?: string;
    categoryId?: string;
    mapping?: string;
    themeDark?: string;
    themeLight?: string;
  } | undefined) ?? {};

  const enableComments = metadata.frontMatter.comments !== false;

  return (
    <>
      <LlmCopySelector currentDocId={metadata.id} />
      <Layout {...props} />
      {enableComments && (
        <div style={{marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid var(--ifm-color-emphasis-200)'}}>
          <Giscus
            repo={(giscus.repo ?? 'VooDisss/grobid') as `${string}/${string}`}
            repoId={(giscus.repoId ?? 'R_kgDORx5luw') as `${string}/${string}`}
            category={giscus.category ?? 'Announcements'}
            categoryId={giscus.categoryId ?? 'DIC_kwDORx5lu84C5Ytr'}
            mapping={(giscus.mapping ?? 'og:title') as Mapping}
            strict="0"
            reactionsEnabled="0"
            emitMetadata="0"
            inputPosition="top"
            theme={colorMode === 'dark' ? (giscus.themeDark ?? 'noborder_dark') : (giscus.themeLight ?? 'light')}
            lang="en"
            loading="lazy"
          />
        </div>
      )}
    </>
  );
}
