---
title: "Contributing"
description: "How to improve the docs and try changes in a fork"
sidebar_position: 1
---

# Contributing

The documentation is intentionally designed so contributors can fork the repository, change docs locally, preview the site, and open a pull request without needing to rewrite a large set of hardcoded site URLs.

## What kinds of documentation this site aims to include

The docs are being shaped around the four Diataxis documentation types:

- tutorials
- how-to guides
- reference
- explanation

That means some parts of the site are deliberately practical and task-oriented, while others are intentionally denser and descriptive.

## Current state of the docs

The docs are already usable and structured, but there are still content gaps and areas that can be improved.

Typical high-value contributions include:

- clarifying setup steps
- improving troubleshooting guidance
- filling thin reference pages
- tightening language for clarity and consistency
- improving explanation pages where concepts are still underexplained

## Fork-and-edit workflow

The simplest contribution flow is:

1. fork the repository
2. create a branch for your documentation changes
3. edit files under `website/docs/`
4. preview the site locally
5. open a pull request

## Why forks are easier now

The Docusaurus site configuration has been parameterized so a fork does not require widespread manual URL rewriting just to preview or deploy documentation changes.

Site-specific values such as repository owner, repository name, branch, site URL, and base URL are centralized in `website/docusaurus.config.ts` and can be overridden through environment variables.

## Local preview

From the `website/` directory:

```bash
npm ci
npm run start
```

To produce a production build locally:

```bash
npm run build
```

## Useful environment variables for forks

If you want to test docs in a forked GitHub Pages setup, these values can be overridden:

- `DOCS_GITHUB_OWNER`
- `DOCS_GITHUB_REPO`
- `DOCS_BRANCH`
- `DOCS_SITE_URL`
- `DOCS_BASE_URL`

In many forks, you do not need to set all of them. The defaults are designed to work with a standard GitHub Pages fork layout.

## What to edit

Most documentation changes live in:

- `website/docs/` for content
- `website/sidebars.ts` for navigation
- `website/docusaurus.config.ts` for site-level behavior

Archive material lives under `website/docs/reference/archive/`. That content is preserved for historical context, but it is not the best place for new user-facing guidance.

## Contribution style

Prefer:

- fewer, better docs
- task-oriented writing for normal users
- dense reference only when density is actually useful
- explicit archive labeling for historical material

## Where to discuss changes

- use your fork or the upstream repository issues for concrete change requests
- use GitHub Discussions for broader documentation questions when available

If you are unsure where new material belongs, describe whether it is a tutorial, how-to, reference, or explanation page.
