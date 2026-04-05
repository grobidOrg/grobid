# Consolidation

In GROBID, we call __consolidation__ the usage of an external bibliographical service to correct and complement the results extracted by the tool. GROBID extracts usually in a relatively reliable manner a core of bibliographical information, which can be used to match complete bibliographical records made available by these services.

Consolidation has two main interests:

* The consolidation service improves very significantly the retrieval of header information (+.12 to .13 in F1-score, e.g. from 74.59 F1-score in average for all fields with Ratcliff/Obershelp similarity at 0.95, to 88.89 F1-score, using biblio-glutton and GROBID version `0.5.6` for the PMC 1943 dataset, see more recent [benchmarking documentation](https://grobid.readthedocs.io/en/latest/End-to-end-evaluation/) and [reports](https://github.com/kermitt2/grobid/tree/master/grobid-trainer/doc)).

* The consolidation service matches the extracted bibliographical references with known publications, and complement the parsed bibliographical references with various metadata, in particular DOI, making possible the creation of a citation graph and to link the extracted references to external services.

The consolidation includes the CrossRef Funder Registry for enriching the extracted funder information.

GROBID supports two consolidation services:

* [CrossRef REST API](https://github.com/CrossRef/rest-api-doc) (default)

* [biblio-glutton](https://github.com/kermitt2/biblio-glutton)

## CrossRef REST API

The advantage of __CrossRef__ is that it is available without any further installation. It has however a limited query rate (in practice around 25 queries per second), which make scaling impossible when processing bibliographical references for several documents processed in parallel. In addition, it provides metadata limited by what is available at CrossRef.

For using [reliably and politely the CrossRef REST API](https://github.com/CrossRef/rest-api-doc#good-manners--more-reliable-service), it is highly recommended to add a contact email to the queries. This is done in GROBID by modifying the config file under `grobid-home/config/grobid.yaml`:

```yaml
consolidation:
    crossref:
      mailto: toto@titi.tutu
      timeoutSec: 10
```

Without indicating this email, the service might be unreliable with some query failures over time. The usage of the CrossRef REST API by GROBID respects the query rate indicated by the service dynamically by each response. Therefore, there should not be any issues reported by CrossRef via this email.

In case you are a lucky Crossref Metadata Plus subscriber, you can set your authorization token in the config file under `grobid-home/config/grobid.yaml` as follow:

```yaml
consolidation:
    crossref:
      token: YOUR_CROSSREF_PLUS_TOKEN
      timeoutSec: 10
```

According to Crossref, the token will ensure that said requests get directed to a pool of machines that are reserved for "Plus" SLA users (note: of course the above token is fake).

### Concurrency

GROBID automatically adjusts concurrency based on the CrossRef API tier detected from the configuration:

| Tier | Condition | Initial Concurrent Requests |
|------|-----------|----------------------------|
| Public | No `mailto`, no `token` | 1 |
| Polite | `mailto` set | 3 |
| Plus | `token` set | 50 |

These initial values are further tuned at runtime using the `x-concurrency-limit` header returned by CrossRef API responses.

When a Plus tier token is configured, GROBID validates it at startup by making a lightweight request (`/works?rows=0`) to CrossRef. If the token is not recognized as Plus tier (e.g. expired or invalid), GROBID automatically falls back to Polite concurrency (3) if `mailto` is set, or Public (1) otherwise, and logs a warning. If CrossRef is unreachable at startup, the Plus tier default is kept since the token cannot be proven invalid.

### Performance with CrossRef Consolidation

When citation consolidation is enabled, the CrossRef API becomes the dominant factor in processing time. Below are benchmarks from processing 10,000 PDF documents with `processFulltextDocument` and `consolidateCitations=1`:

| Metric | Polite Tier | Plus Tier |
|--------|------------|-----------|
| Total runtime | ~162,277 sec (~45 hours) | ~42,755 sec (~12 hours) |
| Speed | 0.06 docs/sec | 0.23 docs/sec |
| Throughput per document | 17.02 sec/doc | 4.31 sec/doc |
| Failed documents | 467/10,000 (4.7%) | 85/10,000 (0.85%) |

The Plus tier is approximately **3.8x faster** and produces **~5.5x fewer errors** compared to the Polite tier. For any batch processing beyond a few hundred documents with citation consolidation, the Plus tier is strongly recommended.

!!! warning "Increase client timeout when using consolidation"
    With consolidation enabled, GROBID takes significantly longer to process each document. The default client timeout (e.g. 60 seconds in the Python client) is far too low — individual documents with many references can take well over a minute. **Increase the client timeout to 200–600 seconds** to avoid unnecessary timeout errors. For example, in the Python client's `config.json`, set `"timeout": 300`.

### Rate Limiting and Backoff

When CrossRef returns HTTP 429 (rate limit exceeded), GROBID applies exponential backoff with jitter ("full jitter" strategy):

- Base delay: 1 second, exponentially increased (`base * 2^attempt`), capped at 60 seconds
- Each retry sleeps for a random duration in `[0, cap]`, spreading retries across time and avoiding synchronized retry bursts (thundering herd)
- During backoff, concurrency is reduced to 1 (serialized requests)
- On the next successful response, backoff resets and concurrency is restored

GROBID also reads the `x-api-pool` header from responses to identify which CrossRef pool is being used.

### Post-Validation

By default, GROBID post-validates CrossRef results against the source metadata to filter false positives (the CrossRef API is a search API and may return inexact matches). This validation compares the first author surname using fuzzy matching.

For biblio-glutton, post-validation is always skipped because glutton handles validation internally.

For CrossRef, post-validation can be disabled via configuration:

```yaml
consolidation:
    crossref:
      postValidation: false
```

When post-validation is disabled, all CrossRef results are accepted. This can be useful for testing or when the results are post-processed by another system.

### Timeouts

Both consolidation services support configurable timeouts to control how long GROBID waits for external API responses:

```yaml
consolidation:
    crossref:
      timeoutSec: 100    # timeout in seconds for CrossRef API calls
    glutton:
      timeoutSec: 60    # timeout in seconds for biblio-glutton API calls
```

!!! warning "Be careful when setting low timeout values"
    Setting too low timeout values may cause request failures. For CrossRef, be particularly careful as aggressive querying with short timeouts and high volume may result in being banned from the service.

## TEI Output

When consolidation is performed, the resulting TEI output includes attributes on `<biblStruct>` elements to indicate the consolidation status and which service was used:

- `status="consolidated"` — the bibliographic item was matched and enriched by the consolidation service
- `status="extracted"` — the bibliographic item was extracted by GROBID only (no consolidation match)
- `source="crossref"` or `source="glutton"` — which consolidation service provided the match

These attributes appear on both header `<biblStruct>` (in `<sourceDesc>`) and citation `<biblStruct>` elements.

Example:
```xml
<biblStruct status="consolidated" source="crossref">
    ...
</biblStruct>
```

## biblio-glutton

This service presents several advantages as compared to the CrossRef service. biblio-glutton can scale as required by adding more Elasticsearch nodes, allowing the processing of several PDF per second. The metadata provided by the service are richer: in addition to the CrossRef metadata, biblio-glutton also returns the PubMed and PubMed Central identifiers, ISTEX identifiers, PII, and the URL of the Open Access version of the full text following the Unpaywall dataset. Finally, the bibliographical reference matching is [slighty more reliable](https://github.com/kermitt2/biblio-glutton#matching-accuracy).

Unfortunately, you need to install the service yourself, including loading and indexing the bibliographical resources, as documented [here](https://github.com/kermitt2/biblio-glutton#building-the-bibliographical-data-look-up-and-matching-databases). Note that a [docker container](https://github.com/kermitt2/biblio-glutton#running-with-docker) is available.

After installing biblio-glutton, you need to select the glutton matching service in the `grobid-home/config/grobid.yaml` file, with its url, for instance:

```yaml
consolidation:
    service: "glutton"
    glutton:
      url: "http://localhost:8080"
      timeoutSec: 60
```
