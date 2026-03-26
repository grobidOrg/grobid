import React, {type ReactNode} from 'react';
import Layout from '@theme-original/DocItem/Layout';
import type LayoutType from '@theme/DocItem/Layout';
import type {WrapperProps} from '@docusaurus/types';
import {useDoc} from '@docusaurus/plugin-content-docs/client';
import {useColorMode} from '@docusaurus/theme-common';
import Giscus from '@giscus/react';

type Props = WrapperProps<typeof LayoutType>;

export default function LayoutWrapper(props: Props): ReactNode {
  const {metadata} = useDoc();
  const {colorMode} = useColorMode();

  const enableComments = metadata.frontMatter.comments !== false;

  return (
    <>
      <Layout {...props} />
      {enableComments && (
        <div style={{marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid var(--ifm-color-emphasis-200)'}}>
          <Giscus
            repo="grobidOrg/grobid"
            repoId=""
            category="Documentation Feedback"
            categoryId=""
            mapping="pathname"
            strict="0"
            reactionsEnabled="1"
            emitMetadata="0"
            inputPosition="top"
            theme={colorMode === 'dark' ? 'dark' : 'light'}
            lang="en"
            loading="lazy"
          />
        </div>
      )}
    </>
  );
}
