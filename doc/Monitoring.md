# Monitoring GROBID with Prometheus and Grafana

GROBID exposes its runtime metrics in the [Prometheus](https://prometheus.io/) exposition format, so you
can scrape them with a Prometheus server and build dashboards and alerts in [Grafana](https://grafana.com/).
This page describes a simple, self-contained monitoring setup suitable for a single GROBID instance.

GROBID supports two complementary delivery models, which can be used independently or together:

* **Pull** (sections 1–4 below) — a Prometheus server scrapes the `/metrics/prometheus` endpoint on an
  interval. This is the default and needs no extra configuration in GROBID.
* **Push** ([OTLP](#push-based-export-otlp), the last section) — GROBID periodically exports metrics over
  OTLP to a collector or a hosted backend such as Grafana Cloud. Useful when the backend cannot reach the
  service to scrape it (containers behind NAT, ephemeral/batch runs).

## What is exposed

The metrics endpoint is served on the **admin connector** (port `8071` by default, *not* the main API port
`8070`):

```
http://yourhost:8071/metrics/prometheus
```

The output contains two families of metrics:

* **Application metrics** — derived from the `@Timed` REST entry points of GROBID. Each endpoint produces a
  Prometheus *summary*: a `_count` series (number of requests, i.e. throughput) and latency quantiles. The
  metric names are the fully-qualified Java names with dots replaced by underscores, e.g.
  `org_grobid_service_GrobidRestService_processFulltextDocument_post`.
* **JVM / process metrics** — heap and non-heap memory, garbage collection, threads, CPU and file
  descriptors. These come from the standard Prometheus JVM collectors and use the conventional names, e.g.
  `jvm_memory_bytes_used`, `jvm_gc_collection_seconds_count`, `jvm_threads_current`,
  `process_cpu_seconds_total`.

You can verify the endpoint manually before wiring up Prometheus:

```bash
curl http://localhost:8071/metrics/prometheus
```

You should see plain-text lines such as:

```
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap",} 1.34217728E8
...
```

!!! note "Exposing the admin port"
    When running GROBID in Docker, the admin connector must be published. The Docker examples in this
    documentation map it to host port `8081` (`-p 8081:8071`), so the scrape target becomes
    `host:8081/metrics/prometheus`. Adjust the targets below accordingly.

## 1. Configure Prometheus

Create a `prometheus.yml` with a scrape job pointing at the GROBID admin connector:

```yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: grobid
    metrics_path: /metrics/prometheus
    static_configs:
      - targets: ['grobid:8071']
        labels:
          instance: grobid-prod
```

Replace `grobid:8071` with the host and admin port reachable from your Prometheus server (for a local
Docker GROBID with the mapping above, use `host.docker.internal:8081` or the host IP).

## 2. Run the monitoring stack

The quickest way to get Prometheus and Grafana running is Docker Compose. The following
`docker-compose.yml` brings up GROBID together with a Prometheus and a Grafana instance:

```yml
services:
  grobid:
    image: grobid/grobid:0.9.0
    ports:
      - "8070:8070"   # REST API
      - "8071:8071"   # admin connector (metrics)

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
```

Start it with:

```bash
docker compose up -d
```

Then:

* Prometheus UI: <http://localhost:9090> — check **Status → Targets**, the `grobid` target should be `UP`.
* Grafana UI: <http://localhost:3000> — default login `admin` / `admin`.

## 3. Visualise in Grafana

1. In Grafana, add a data source: **Connections → Data sources → Prometheus**, URL `http://prometheus:9090`.
2. Create a dashboard and add panels using PromQL queries. A few useful starting points:

| What you want to see | PromQL |
|----------------------|--------|
| Heap memory used | `jvm_memory_bytes_used{area="heap"}` |
| Request throughput (per second, 5 min window) | `rate(org_grobid_service_GrobidRestService_processFulltextDocument_post_count[5m])` |
| GC time rate | `rate(jvm_gc_collection_seconds_sum[5m])` |
| Live threads | `jvm_threads_current` |
| Process CPU usage | `rate(process_cpu_seconds_total[5m])` |

!!! tip
    Use the metric autocomplete in the Prometheus UI (<http://localhost:9090>) to discover the exact
    timer names for the endpoints you care about — they depend on the GROBID REST methods being called.

## 4. Alerting (optional)

Prometheus can fire alerts based on the same metrics. Add a rules file and reference it from
`prometheus.yml` (`rule_files: ['alerts.yml']`). For example, to be notified when GROBID stops being
scrapeable, or when heap usage stays high:

```yml
groups:
  - name: grobid
    rules:
      - alert: GrobidDown
        expr: up{job="grobid"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "GROBID instance {{ $labels.instance }} is not reachable"

      - alert: GrobidHighHeapUsage
        expr: jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "GROBID heap usage above 90% on {{ $labels.instance }}"
```

Tune the thresholds (`0.9` heap ratio, the `for:` durations) to your deployment: a batch-heavy GROBID
running close to its `Xmx` is normal and may warrant a higher threshold, whereas an interactive service
should stay well below saturation.

## Push-based export (OTLP)

The sections above cover the **pull** model. As an alternative (or in addition), GROBID can **push** metrics
over [OTLP](https://opentelemetry.io/docs/specs/otlp/) to an OpenTelemetry Collector,
[Grafana Alloy](https://grafana.com/docs/alloy/latest/)/Agent, or a hosted backend such as Grafana Cloud.
This is the right choice when a Prometheus server cannot reach the service to scrape it — for example a
container behind NAT, a short-lived batch run, or a managed backend you only have outbound access to.

Note that Grafana itself does not ingest metrics — it queries a time-series store (Prometheus, Mimir,
Grafana Cloud, …). OTLP pushes into that store, which Grafana then dashboards. The pull and push paths are
independent: you can enable either, both, or neither.

When enabled, the service periodically exports JVM/process runtime metrics (heap, GC, threads, CPU,
classes). It is **disabled by default**; configure it under `grobid.otlp` in
`grobid-home/config/grobid.yaml`:

```yaml
grobid:
  otlp:
    enabled: true
    endpoint: "http://localhost:4318"   # base URL; 4318 for http/protobuf (the "/v1/metrics" path is added automatically), 4317 for grpc
    protocol: "http/protobuf"           # or "grpc"
    intervalSeconds: 60
    timeoutSeconds: 10                  # per-export timeout; bounds how long one push waits on a slow/unreachable receiver
    serviceName: "grobid-service"
    # extra headers for backend auth, e.g. Grafana Cloud:
    #headers:
    #  Authorization: "Basic <base64 of instanceID:apiToken>"
```

> **Scope:** the OTLP exporter currently pushes **JVM/process runtime** metrics only (the `jvm_*` /
> `process_*` families and an OTel `target_info` resource series). The application timers per REST entry
> point (the `org_grobid_service_*` summaries) are exposed on the pull endpoint but are *not* yet bridged
> into the OTLP export.

### Grafana Cloud — worked example

Grafana Cloud exposes a managed OTLP gateway, so no Collector/Alloy is needed in between. In your Grafana
Cloud stack, open **Connections → OTLP** (the "OpenTelemetry" setup) and note three values:

* **Endpoint** — `https://otlp-gateway-<zone>.grafana.net/otlp` (e.g.
  `https://otlp-gateway-prod-eu-central-0.grafana.net/otlp`). Use this **base** URL; GROBID appends
  `/v1/metrics` itself.
* **Instance ID** — the numeric stack id (this is the OTLP username).
* **API token** — a token with the `metrics:write` scope (a `glc_…` Cloud Access Policy token).

Grafana Cloud authenticates with HTTP Basic auth where the username is the instance ID and the password is
the token. Build the header value yourself:

```bash
# Authorization header = "Basic " + base64("<instanceID>:<apiToken>")
printf '%s' '<instanceID>:<apiToken>' | base64
```

Then configure (keep the real token out of version control — see the security note below):

```yaml
grobid:
  otlp:
    enabled: true
    endpoint: "https://otlp-gateway-prod-eu-central-0.grafana.net/otlp"
    protocol: "http/protobuf"
    intervalSeconds: 60
    timeoutSeconds: 10
    serviceName: "grobid-service"
    headers:
      Authorization: "Basic <base64 of instanceID:apiToken>"
```

On startup the service logs a single confirmation line:

```
OtlpMetricsReporter: OTLP metrics push enabled -> https://otlp-gateway-prod-eu-central-0.grafana.net/otlp (http/protobuf), every 60s, service.name=grobid-service
```

The exporter only logs on **failure** (a non-2xx response, auth error, or timeout produces a `WARN`/
`SEVERE`), so a quiet log after that line means the pushes are succeeding. To confirm the data has landed,
open **Explore** in Grafana Cloud, select the stack's Prometheus/Mimir data source, and query over the last
15 minutes:

```promql
target_info{service_name="grobid-service"}        # the resource/identity series — easiest heartbeat
jvm_memory_used_bytes{service_name="grobid-service"}
```

!!! warning "Keep the token out of version control"
    The `Authorization` header embeds a live credential. Do not commit it to `grobid.yaml`. Prefer a
    deployment-time secret — keep the token in an untracked config file mounted at runtime, or template the
    value in from an environment variable / secrets manager in your container orchestration. Rotate the
    token if it is ever exposed.

## See also

* [Using the REST API](Grobid-service.md) — service checks and the admin console
* [Configuration](Configuration.md) — the `adminConnectors` port setting
* [Run with Docker](Grobid-docker.md) — port mappings for the admin connector
