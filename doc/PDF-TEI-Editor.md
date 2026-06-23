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

### Connecting to a GROBID server

The editor does **not** bundle GROBID — it talks to a running GROBID server over its REST API to pre-annotate documents. You tell the editor which server to use with the `GROBID_SERVER_URL` environment variable.

Pre-annotation quality directly determines how much manual correction you have to do, so always point the editor at a **full** GROBID server (one running the Deep Learning models): it gives noticeably better reference and citation extraction than the CRF-only *light* instances. You have two options:

- **Use the public full instance on Hugging Face** — `https://grobidOrg-grobid-full.hf.space` (mirror `https://grobidOrg-grobid-full2.hf.space`). It runs the Deep Learning models and requires no installation, so it is the quickest way to start annotating. This is a good fit for occasional work or a first pass. (The *light* instances `https://grobidOrg-grobid.hf.space` and its mirror `https://grobidOrg-grobid2.hf.space`, documented on the [Quick start](getting_started.md) page, are CRF-only and not recommended for building a gold-standard corpus.)

- **Run your own full GROBID instance** — recommended once you settle into iterative sessions of correction and retraining. Hosting it yourself removes the rate/availability limits of the public space and, more importantly, lets you point the editor at *your own* freshly retrained models so each correction cycle pre-annotates with the improvements from the previous one. Use the *full* image (`grobid/grobid:{version}-full`, or the mirror `lfoppiano/grobid:{version}-full`); see [Run with Docker](Grobid-docker.md) for the full instructions. GROBID's REST API listens on port `8070`, so the URL is typically `http://localhost:8070`:

```bash
docker run --rm --gpus all --init --ulimit core=0 -p 8070:8070 grobid/grobid:0.9.0-full
```

### Running the editor (Docker)

The fastest way to run the editor itself is its Docker image, passing the GROBID endpoint via `GROBID_SERVER_URL`:

```bash
docker run -p 8000:8000 \
  -e APP_ADMIN_PASSWORD=secure_password \
  -e GROBID_SERVER_URL=https://grobidOrg-grobid-full.hf.space \
  cboulanger/pdf-tei-editor:latest
```

The application is then available at `http://localhost:8000` (user `admin`, with the password set above). The equivalent `docker-compose.yml`:

```yaml
services:
  pdf-tei-editor:
    image: cboulanger/pdf-tei-editor:latest
    ports:
      - "8000:8000"
    environment:
      - APP_ADMIN_PASSWORD=secure_password
      - GROBID_SERVER_URL=https://grobidOrg-grobid-full.hf.space
```

!!! note
    The example above uses the public Hugging Face instance so it works without any further setup. If you instead run your own GROBID (see above), remember that `GROBID_SERVER_URL` must be reachable **from inside the editor's container**: when GROBID runs on the same host, use `http://host.docker.internal:8070` (Docker Desktop) or put both containers on the same Docker network and use the GROBID container name. A bare `http://localhost:8070` refers to the editor container itself and will not reach GROBID.

### Trying the bundled demo

The repository also ships a one-command demo deployment:

```bash
git clone https://github.com/mpilhlt/pdf-tei-editor.git
cd pdf-tei-editor
npm run deploy .env.deploy.demo.localhost
```

It becomes available at `http://localhost:8080` with demo credentials `admin/admin` or `demo/demo` — change these for any non-local use.

For the authoritative and most current installation, configuration, and usage instructions, see the [pdf-tei-editor repository](https://github.com/mpilhlt/pdf-tei-editor/) and its documentation.
