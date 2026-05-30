# Annotating training data with the PDF-TEI Editor

[pdf-tei-editor](https://github.com/mpilhlt/pdf-tei-editor/) is a web-based, open-source tool for editing and correcting GROBID TEI training data side-by-side with the source PDF. It provides a graphical alternative to editing the `*.training.*.tei.xml` files by hand in a text editor, which makes the [correction of pre-annotated training data](training/General-principles.md#correcting-pre-annotated-files) considerably faster and less error-prone.

The tool is developed as part of the *Legal Theory Knowledge Graph* project at the Max Planck Institute of Legal History and Legal Theory.

!!! note
    The PDF-TEI Editor is a third-party project and is **not** maintained by the GROBID team. Refer to its [repository](https://github.com/mpilhlt/pdf-tei-editor/) and documentation for support, issues, and the most up-to-date instructions.

## Why use it

When you [generate pre-annotated training data](Training-the-models-of-Grobid.md#generation-of-training-data) with GROBID's `createTraining` batch command, the output is a set of TEI XML files that **must be reviewed and corrected** before they can be added to the gold-standard corpus. Doing this in a raw text editor is tedious: you constantly switch between the XML and the original PDF to check whether a label is on the right span of text, and it is easy to accidentally alter the text stream — which [must be kept untouched](training/General-principles.md#correcting-pre-annotated-files).

The PDF-TEI Editor addresses this by:

- **Synchronized dual-pane interface** — the rendered PDF and the editable TEI XML are shown next to each other, so you can verify annotations against the source layout at a glance.
- **Schema validation** — the TEI is validated for compliance as you edit, catching malformed markup before it reaches the training corpus.
- **Version control** — branching, merging, comparison (diff) between versions, and detailed revision tracking, which is useful for collaborative, multi-annotator gold-standard creation.
- **Role-based access control and collection management** — for organizing documents and contributors across a shared dataset.
- **Multiple extraction engines** — GROBID is supported as one of the AI extraction backends, so documents can be pre-annotated and then corrected within the same interface.

## Typical workflow with GROBID

The editor fits into the standard GROBID training-data preparation loop described in the [annotation guidelines](training/General-principles.md):

1. Run GROBID's `createTraining` (see [Generation of training data](Training-the-models-of-Grobid.md#generation-of-training-data)) to pre-annotate your PDFs, producing the `*.training.*.tei.xml` files — or use the editor's built-in GROBID extraction.
2. Open the PDF together with its generated TEI XML in the PDF-TEI Editor.
3. Visually correct the annotations against the PDF, **moving tags without altering the text stream** (the `<lb/>` line-break markers and the order of the text must be preserved — see the [correction principles](training/General-principles.md#correcting-pre-annotated-files)).
4. Validate the TEI and save a clean, gold-standard version.
5. Move the corrected file into the corresponding model's corpus directory (`grobid-trainer/resources/dataset/<MODEL>/corpus/`) and [retrain the model](Training-the-models-of-Grobid.md).

!!! tip
    Remember that GROBID training data is curated by **editing or deleting** the pre-annotated files — you should not create new `*.training.*.tei.xml` files from scratch. The editor is there to make the *correction* of GROBID's output efficient, not to author TEI independently of GROBID's extraction.

## Getting started

The fastest way to try it is the Docker-based deployment. From the project's documentation:

```bash
git clone https://github.com/mpilhlt/pdf-tei-editor.git
cd pdf-tei-editor
npm run deploy .env.deploy.demo.localhost
```

The application is then available at `http://localhost:8080` (default demo credentials `admin/admin` or `demo/demo` — change these for any non-local use).

A development setup is also available:

```bash
git clone https://github.com/mpilhlt/pdf-tei-editor.git
cd pdf-tei-editor
cp .env.development .env
npm install
npm run start:dev
```

For the authoritative and most current installation, configuration, and usage instructions, see the [pdf-tei-editor repository](https://github.com/mpilhlt/pdf-tei-editor/) and its documentation.

## Technical notes

- **Backend:** FastAPI (Python 3.13+), SQLite, lxml
- **Frontend:** ES6 modules, CodeMirror 6, PDF.js, Shoelace
- **Synchronization:** WebDAV support for connecting to external systems

These details may change over time; consult the upstream repository for the current stack and requirements.
