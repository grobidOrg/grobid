# Troubleshooting and Frequently Asked Questions

## What Java version do I need for GROBID?

**For building and running**: OpenJDK 21 or higher, however some dependencies may require an earlier version, so we might increase the runtime backward compatibility to JDK 17+ in the next release, > 0.8.2.


## What is 500 `BAD_INPUT_DATA` or `NO_BLOCKS` error?

In short, this is not a critical error.
For historical reasons, there are certain error 500 responses that are not critical errors; see [here](Grobid-service.md#errors-handling) for a more detailed explanation.
`BAD_INPUT_DATA` is likely a PDF document that cannot be converted because it is damaged or too big. `NO_BLOCKS` means that the PDF does not contain any text, and should be processed with OCR first.


## How to make sure that the GPU is being used

If you are using the full Docker image (e.g. `grobid/grobid:0.8.2-full`), the GPU will be automatically detected on Linux if available; however, to be sure, we recommend using `nvtop` (similar to `htop` for CPU).

## How to avoid DeLFT's Tensorflow (TF) to allocate the full GPU memory

By default, TensorFlow allocates all the GPU memory when it starts.
This can be changed by setting the environment variable `TF_FORCE_GPU_ALLOW_GROWTH` to `true` before starting GROBID.
With docker, for example you can this command:

```shell
docker run --rm --gpus all --init --ulimit core=0 -e TF_FORCE_GPU_ALLOW_GROWTH='true' -p 8070:8070 grobid/grobid:0.8.2-full
```

## JEP/DeLFT fails with `CXXABI` or `libstdc++` version errors

When running GROBID with DeLFT models outside Docker (e.g. locally or via `run_evaluation.sh`), JEP initialization may fail with errors like:

```
ImportError: /home/user/miniconda3/envs/myenv/lib/python3.10/lib-dynload/../../libicui18n.so.78:
  version `CXXABI_1.3.15' not found (required by .../libstdc++.so.6)
```

This happens because the system's `libstdc++.so.6` is older than what the conda/virtualenv libraries expect. Your Python environment likely ships a newer `libstdc++.so.6` in its `lib/` directory, but the linker doesn't know to look there.

**Fix**: Before starting GROBID, prepend your Python environment's `lib/` directory to `LD_LIBRARY_PATH`:

```bash
# For conda environments:
export LD_LIBRARY_PATH="${CONDA_PREFIX}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"

# For virtualenvs:
export LD_LIBRARY_PATH="${VIRTUAL_ENV}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
```

!!! tip
    The `run_evaluation.sh` script detects active conda/virtualenv environments and sets `LD_LIBRARY_PATH` automatically. If you run GROBID through other means (e.g. `./gradlew :grobid-service:run`), you may need to export this variable manually.


## When processing a large quantity of files, I see many `503` errors

The `503` status returned by GROBID is not an error, and does not mean that the server has issues. On the contrary, this is the mechanism to avoid the service from collapsing and to keep it up, alive, and running according to its capacity for days.

As described in the [documentation](Grobid-service.md#apiprocessfulltextdocument), the `503` status indicates that the service is currently using all its available threads, and the client should simply wait a bit before re-sending the query.

Exploiting the `503` mechanism is already implemented in the different GROBID clients listed [here](Grobid-service.md#clients-for-grobid-web-services).

## Timeout and 408 errors when running the client

Occasionally, [people have reported](https://github.com/kermitt2/grobid/issues/1234) timeout issues when using Grobid, especially when processing large quantities of PDF files locally. This can happen if the server takes too long to respond, leading to a 408 Request Timeout error.

To resolve this issue, there are several options:
 - Check that you are running the proper image for your hardware. If you are not sure, use the image `grobid/grobid:0.8.2-crf` which is the most lightweight and fastest image.
 - Make sure you don't send too many requests at the same time, as this can overload the server. If you are using the Grobid Python client, you can set the `n` parameter to a lower value (e.g. 1 or 2) to limit the number of concurrent requests.
 - Increase the timeout value in your client. If you are using the Grobid Python client, you can set the `timeout` parameter to a higher value (e.g. 90 seconds) in the `config.json` to give the server more time to respond. **If consolidation is enabled** (`consolidateHeader` or `consolidateCitations`), you should increase the timeout much further — to **200–600 seconds** — because each document now waits for multiple external API lookups on top of PDF processing.

## The service is slow to process PDFs

If GROBID seems unusually slow when processing documents, the most common cause is **consolidation**. When consolidation is enabled (e.g. `consolidateHeader=1` or `consolidateCitations=1`), GROBID queries an external bibliographical service (CrossRef or biblio-glutton) for each bibliographic item, which adds significant latency — especially for documents with many references.

A few things to check:

1. **Are you using consolidation?** If you don't need DOI resolution or metadata enrichment, you can disable consolidation by setting `consolidateHeader=0` and `consolidateCitations=0` in your API calls. This will make processing much faster.

2. **Have you configured a polite email?** If you are using CrossRef consolidation, you should set a contact email in `grobid-home/config/grobid.yaml`:

    ```yaml
    consolidation:
        crossref:
          mailto: your-email@example.org
    ```

    Without a `mailto`, requests go through the CrossRef **public** pool with minimal concurrency (1 request at a time). Adding an email enables access to the **polite** pool with higher concurrency (up to 3), resulting in noticeably faster consolidation. See the [Consolidation documentation](Consolidation.md) for more details on tiers.

3. **Consider a CrossRef Plus subscription for medium-scale processing.** If you need consolidation but don't want to self-host biblio-glutton, a [CrossRef Metadata Plus](https://www.crossref.org/services/metadata-retrieval/metadata-plus/) subscription can help significantly. Benchmarks on 10,000 documents show the Plus tier is ~3.8x faster and has ~5.5x fewer errors compared to the Polite tier. See the [Consolidation documentation](Consolidation.md#performance-with-crossref-consolidation) for detailed numbers.

4. **Consider biblio-glutton for large-scale processing.** The CrossRef API has rate limits that make it impractical for processing large batches with citation consolidation enabled. If you need to process many PDFs with full citation consolidation, consider using [biblio-glutton](https://github.com/kermitt2/biblio-glutton), which can scale horizontally and is not subject to external rate limits.


## How to override the grobid configuration file when running via docker?

You can override the default configuration file by mounting a custom configuration file from your host machine to the container.
The latest versions of the default configuration files are located [here](https://github.com/kermitt2/grobid/tree/master/grobid-home/config).

!!! warn "Configuration file compatibility"
     While you can use the default `grobid.yaml` in the `-full` image, the `grobid-full.yaml` will not work with the `-crf` image.


Once you have downloaded the configuration file locally, for example located at `/path/to/your/grobid.yaml`, you can run the Docker container with the following command:

```shell
docker run --rm --gpus all --init --ulimit core=0 -v /path/to/your/grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml -p 8070:8070 grobid/grobid:0.8.2-full
```


## Where are the logs?

The logs of Grobid are located in `logs/grobid-service.log`. The console logs from Gradle are usually not very useful to understand the problem.
When opening an issue, remember to attach the logs from `grobid-service.log` to the report.
In case of running Grobid via Docker, you can access the logs using the command `docker logs <container_id>`.


## Guidance for server configuration in production

The exact server configuration will depend on the service you want to call, the models selected in the Grobid configuration file (`grobid-home/config/grobid.yaml`), and the availability of GPU. We consider here the complete full text processing of PDF (`processFulltextDocument`).

1) Using lightweight image (CRF models only) without the needs of GPUs (docker image: `grobid/grobid:{version}-crf`):

- in `grobid/grobid-home/config/grobid.yaml` set the parameter `concurrency` to your number of available threads at server side or slightly higher (e.g. 16 to 20 for a 16 threads-machine)

- keep the concurrency at the client (number of simultaneous calls) slightly higher than the available number of threads at the server side, for instance if the server has 16 threads, use a concurrency between 20 and 24 (it's the option `n` in the above mentioned clients)

These settings will ensure that CPU are fully used when processing a large set of PDF.

For example, with these settings, we processed with `processFulltextDocument` around 10.6 PDF per second (around 915,000 PDF per day, around 20M pages per day) with the node.js client during one week on a 16 CPU machine (16 threads, 32GB RAM, no SDD). It ran without any crash during 7 days at this rate. We processed 11.3M PDF in a bit less than 7 days with two 16-CPU servers in one of our projects.

!!! tip
    If your server has 8-10 threads available, you can use the default settings of the docker image, otherwise you will need to modify the configuration file to tune the parameters, as [documented](Configuration.md).

2) Using the full image (with Deep Learning models, docker image: `grobid/grobid:{version}-full`):

2.1) If the server has a GPU

In case the server has a GPU, which has its own memory, the Deep Learning inferences are automatically parallelized on this GPU, without impacting the CPU and RAM memory. The settings given above in 1) can normally be use similarly.

2.2) If the server has CPU only

When Deep Learning models run as well on CPU as fallback, the CPU are used more intensively (DL models push CPU computations quite a lot), more irregularly (Deep Learning models are called at certain point in the overall process, but not continuously) and the CPU will use additional RAM memory to load those larger models. For the DL inference on CPU, an additional thread is created, allocating its own memory. We can have up to 2 times more CPU used at peaks, and approx. up to 50% more memory.

The settings should thus be considered as follow:

- in `grobid/grobid-home/config/grobid.yaml` set the parameter `concurrency` to your number of available threads at server side divided by 2 (8 threads available, set concurrency to `4`)

- keep the concurrency at the client (number of simultaneous calls) at the same level as the `concurrency` parameter at server side, for instance if the server has 16 threads, use a `concurrency` of `8` and the client concurrency at `8` (it's the option `n` in the clients)

In addition, consider more RAM memory when running Deep Learning model on CPU, e.g. 24-32GB memory with concurrency at `8` instead of 16GB.

3) In general, consider also these settings:

- Set `modelPreload` to `true` in `grobid/grobid-home/config/grobid.yaml`, it will avoid some strange behavior at launch (this is the default setting).

- Regarding the query parameters, `consolidateHeader` can be `1`  or `2` if you are using the biblio-glutton or CrossRef consolidation. It significantly improves the accuracy and add useful metadata.

- If you want to consolidate all the bibliographical references and use `consolidateCitations` as `1` or `2`, the CrossRef query rate limit will make the scaling to more than 1 document per second impossible (so Grobid would typically wait 90% or more of its time waiting for CrossRef API responses)... For scaling the bibliographical reference resolutions, you will need to use a local consolidation service, [biblio-glutton](https://github.com/kermitt2/biblio-glutton). The overall capacity will depend on the biblio-glutton service then, and the number of elasticsearch nodes you can exploit. From experience, it is difficult to go beyond 300K PDF per day when using consolidation for every extracted bibliographical references.

See [full thread](https://github.com/kermitt2/grobid/issues/443).

## Memory constraints and Out of Memory errors

In case of running on limited memory hardware, there are various ways to deal with memory constraints in Grobid:

- reduce the number of parallel threads on server side in grobid/grobid-home/config/grobid.yaml change the parameter `concurrency` (default is `10`, this is the max size of the thread pool that is reported in the logs)

- reduce the number of parallel processing at client side: this is the parameter `n` in the grobid python client command line

- reduce the maximum amount of memory dedicated to the PDF parsing by pdfalto: in `grobid/grobid-home/config/grobid.yaml` change the parameter `memory_limit_mb` a lower amount (note: not too low, PDF parsing remains memory-hungry).


## I would also like to extract images from PDFs

You will get the embedded images converted into `.png` by using the normal batch command. For instance:

```console
java -Xmx4G -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-core/build/libs/grobid-core-0.8.2-onejar.jar -gH grobid-home -dIn ~/test/in/ -dOut ~/test/out -exe processFullText
```

There is a web service doing the same, returning everything in a big zip file, `processFulltextAssetDocument`, still usable but deprecated.

A simpler option, if you are only interested in raw text and images, is to use directly [pdfalto](https://github.com/kermitt2/pdfalto).


## pdfalto is GPL, it is used by and shipped with GROBID which is Apache 2, is it okay in term of licensing?

We think there is no issue. First because GROBID calls the pdfalto binary as external command line - so there is similar to chaintools/scripts (which can be Apache/MIT) with external command line calls (calling most of the time also GPL stuff on Linux). More precisely:

- GROBID and pdfalto are two different programs in the sense of FSF, see last paragraph [here](https://www.gnu.org/licenses/gpl-faq.en.html#MereAggregation):

```text
By contrast, pipes, sockets and command-line arguments are communication mechanisms normally
used between two separate programs. So when they are used for communication, the modules
normally are separate programs.
```

Linking libraries, for instance with a JNI, would have required a LGPL, but here we don't link libraries, share address space, and so on.

- pdfalto can be aggregated in the same "grobid" distribution, see GPL faq [Mere Aggregation](https://www.gnu.org/licenses/gpl-faq.en.html#MereAggregation)

```text
The GPL permits you to create and distribute an aggregate, even when the licenses of the
other software are nonfree or GPL-incompatible. The only condition is that you cannot
release the aggregate under a license that prohibits users from exercising rights that
each program's individual license would grant them.
```

For convenience, it is no problem to ship the pdfalto executables with GROBID - same as a docker image which ships typically a mixture of GPL and Apache/MIT stuff calling each others like crazy and much more "deeply" than in our case.

Finally, as the two source codes are shipped in different repo with clear licensing information, exercising the rights that each program's individual license grants them is fully respected.

The only possible restriction would be:

```text
But if the semantics of the communication are intimate enough, exchanging complex
internal data structures, that too could be a basis to consider the two parts as
combined into a larger program.
```

pdfalto produces ALTO files, which is a standard format. pdfalto can be used for many other purposes than GROBID. In return GROBID itself can support other inputs, like text or the old pdf2xml files, and could support ALTO files produced by other tools. So this restriction does not apply.

## API & Usage

### How do I call the GROBID API from a web application?

([#827](https://github.com/kermitt2/grobid/issues/827))

POST to `/api/processFulltextDocument` with the PDF as multipart form data. See the [service documentation](Grobid-service.md) and available [client libraries](Grobid-service.md#clients-for-grobid-web-services) (Python, Java, Node.js).

### Can I extract only the body text, skipping references?

([#674](https://github.com/kermitt2/grobid/issues/674))

There is no built-in parameter for this. GROBID processes the full document structure as a unit. Post-process the TEI XML to extract only the `<body>` element.

### Why does `processHeaderDocument` give different results than `processFulltextDocument`?

([#766](https://github.com/kermitt2/grobid/issues/766))

`processHeaderDocument` only analyzes the first ~2 pages and uses a simplified segmentation. It is ~15x faster but may extract different fields (e.g., `titleStmt`). Use `processFulltextDocument` for complete extraction.

### Can I control which elements GROBID processes (e.g., skip in-text citations)?

([#830](https://github.com/kermitt2/grobid/issues/830))

No. GROBID extracts the complete logical structure. In-text citation markers are part of the document structure. For raw text without structural annotations, use `createTrainingBlank`.

### Why does GROBID use the network during processing?

([#882](https://github.com/kermitt2/grobid/issues/882))

If consolidation is enabled (default), GROBID queries external services (CrossRef or biblio-glutton) to enrich metadata. Set `consolidateHeader=0` and `consolidateCitations=0` to run fully offline.

### Where do DOIs and metadata not present in the PDF come from?

([#799](https://github.com/kermitt2/grobid/issues/799))

From the consolidation service (CrossRef or biblio-glutton). When enabled, GROBID matches extracted references against external databases and enriches the output with DOI, volume, issue, etc. Note: consolidation may replace preprint metadata with the published version's metadata.

### How to use custom trained models without Git conflicts?

([#884](https://github.com/kermitt2/grobid/issues/884))

Either (1) point to an external `grobid-home` directory via configuration, or (2) maintain a fork with your custom models.

### Can I filter out captions, footers, or other elements from the output?

([#989](https://github.com/kermitt2/grobid/issues/989))

Not built-in. Post-process the TEI XML using XPath expressions to select/exclude elements. Tools like `xmltodict` (Python), or converters like `s2orc-doc2json` can help.


## Output & Structure

### Why does GROBID extract only one email per author?

([#263](https://github.com/kermitt2/grobid/issues/263))

By design: GROBID uses Levenshtein distance to match the "best" email to each author. Multiple emails per author are not currently supported.

### Why don't page numbers appear in the TEI output?

([#278](https://github.com/kermitt2/grobid/issues/278))

GROBID produces logical structure, not presentational layout. Pagination is a presentational concept. Page information is available through token coordinates (see [Coordinates in PDF](Coordinates-in-PDF.md)).

### Does GROBID preserve the original text layout?

([#304](https://github.com/kermitt2/grobid/issues/304))

No. GROBID extracts logical document structure (title, sections, references), not visual layout. The original formatting, inline punctuation placement, and whitespace are normalized during extraction.

### Does GROBID detect section hierarchy (section vs. subsection)?

([#526](https://github.com/kermitt2/grobid/issues/526))

Currently, GROBID detects section headings but does not reliably distinguish hierarchy levels. This is tracked as a known enhancement request ([#377](https://github.com/kermitt2/grobid/issues/377)).

### Does GROBID handle inline formulas, superscripts, and subscripts?

([#829](https://github.com/kermitt2/grobid/issues/829))

GROBID detects formulas and retains some formatting data internally, but does not currently serialize superscript/subscript information to the TEI XML output.

### What is the difference between `<bibl>` and `<biblStruct>` in the output?

([#874](https://github.com/kermitt2/grobid/issues/874))

`<bibl>` is used in training data and raw text annotations. `<biblStruct>` (with `<analytic>` and `<monogr>` sub-elements) is used in the normalized output, following TEI conventions for structured bibliographic records.

### Can GROBID produce an interactive PDF viewer with annotations?

([#502](https://github.com/kermitt2/grobid/issues/502))

Not directly, but the recommended approach is to use PDF.js with GROBID's coordinate output as an overlay. Display the original PDF and position annotation layers using the bounding box coordinates. The GROBID console demonstrates this approach.

### How to extract table content from PDFs?

([#514](https://github.com/kermitt2/grobid/issues/514))

GROBID detects table locations and provides bounding box coordinates. For structured table content extraction (rows/cells), use specialized tools like Tabula or Camelot on the detected table regions.


## Scope & Compatibility

### Can GROBID process books or long documents?

([#673](https://github.com/kermitt2/grobid/issues/673))

GROBID is designed for scholarly articles. For books, you can use the `start` and `end` page parameters to process individual chapters. The token limit exists to prevent out-of-memory errors and can be adjusted in configuration, but very long documents may produce unreliable results.

### What languages does GROBID support?

([#958](https://github.com/kermitt2/grobid/issues/958))

GROBID's PDF processing is language-independent, and it includes a 53-language detector. However, the models are primarily trained on English-language articles, with some German and French. Quality for other languages is unpredictable. Custom training data can extend support.

### Best practices for creating PDF documents for GROBID processing

([#839](https://github.com/kermitt2/grobid/issues/839))

Export from Word to PDF rather than using direct `.docx` conversion. Use distinct font sizes for title/headings, standard fonts, and generous spacing. GROBID works best with documents that follow typical scholarly article conventions.

### Why are footnote references poorly extracted?

([#929](https://github.com/kermitt2/grobid/issues/929))

The training data contains very few examples of footnote-style references. Extraction quality for footnote references is significantly lower than for standard bibliography sections. This is an active area of improvement.

### Do I need to retrain to improve affiliation extraction?

([#794](https://github.com/kermitt2/grobid/issues/794))

Yes. If GROBID fails to parse specific affiliation formats (e.g., with country/city information), add failing examples to the `affiliation-address` training corpus and retrain. Note that GROBID expects PDF-extracted text blocks (with formatting artifacts), not clean text.

### How does GROBID use pdfalto internally?

([#553](https://github.com/kermitt2/grobid/issues/553))

pdfalto converts PDF to ALTO XML. GROBID then converts ALTO tokens into `LayoutToken` objects. Different models operate at different granularities: segmentation works at block level, fulltext at line level, and name parsing at token level.


### Windows related issues

Grobid is developed and tested on Linux. Mac is also supported, although some components might behave slightly different due to the natural incompatibility of Apple with the rest of the world, and the availability on some proprietary fonts on this platform.

Grobid running natively on Windows is not supported. We recommend Windows users to use the docker image (documented [here]()) and call the system via the REST API using one of the various [grobid clients](Grobid-service.md#clients-for-grobid-web-services).

Before opening a new issue which might be related to Windows, please check that it is not redundant [here](https://github.com/kermitt2/grobid/issues?q=is%3Aissue+is%3Aopen+label%3AWindows-specific)

!!! warn "Windows Subsystem for Linux (WSL)"
    Please be aware that users have reported issues using the WSL mode in Windows to run Grobid. More details [here](https://github.com/kermitt2/grobid/issues/954).

**NOTE**: If you are a developer using Windows and you like to help, please let us know.


## Invalid model format

Using macOS 10.12 Sierra or higher with a non-default locales (e.g. French), you might get the following error:
```
error: invalid model format
```

This is because Wapiti is not reading the binary files correctly (most likely due to some incompatibles introduced from OSX to macOS).

As workaround, before launching GROBID, type in the console:
```
export LC_ALL=C
```

See [here](https://github.com/kermitt2/grobid/issues/142#issuecomment-253497513) the open issue.

## Missing libxml2 library

**libxml2**: GROBID is currenly shipped with all the needed libraries (Mac and Linux 32/64 bit).

libxml2 is required by pdfalto, and is normally shipped by default on all standard installation (Ubuntu, Mac OSX, etc).

For minimal or cloud based / container system like Linode, AWS, Docker, etc. _libxml2_ might not be installed by default and should thus be installed as prerequisite.

See [here](https://github.com/kermitt2/grobid/issues/101) the open issue. 

