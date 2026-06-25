package org.grobid.service.main;

import java.util.ArrayList;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.dropwizard.DropwizardExports;

/**
 * {@link DropwizardExports} variant that makes the bridged Dropwizard metrics idiomatic for
 * Prometheus, so that {@code curl /metrics/prometheus | promtool check metrics} comes back clean
 * (issue #920).
 *
 * <p>On top of the snake_case naming provided by {@link SnakeCaseSampleBuilder}, the
 * {@link #collect()} override fixes two convention violations that stem from the mismatch between
 * the Dropwizard and Prometheus metric models:
 *
 * <ul>
 *   <li><b>Duplicate JVM metrics.</b> Dropwizard registers its own JVM gauge sets
 *       ({@code jvm.threads.*.count}, {@code jvm.gc.*.count}, {@code jvm.buffers.*.count},
 *       {@code jvm.memory.pools.*}). These are redundant with the hotspot metrics exported by
 *       {@code DefaultExports} and carry {@code _count} suffixes promtool rejects on gauges, so we
 *       drop every {@code jvm}-prefixed family and keep the clean hotspot copies.</li>
 *   <li><b>{@code _total} on non-counters.</b> Jersey's per-method {@code total} timer is bridged
 *       to a Prometheus SUMMARY whose base series ends in the {@code _total} suffix Prometheus
 *       reserves for counters. We rename those families (and all their samples) off {@code _total}.
 *       Genuine Dropwizard meters (already typed COUNTER, e.g. {@code ..._created_total}) keep
 *       their {@code _total} suffix.</li>
 * </ul>
 */
public class GrobidDropwizardExports extends DropwizardExports {

    private static final String JVM_PREFIX = "jvm";
    private static final String RESERVED_SUFFIX = "_total";
    /** Jersey "total" timers measure request time; simpleclient exports timer values in seconds. */
    private static final String REPLACEMENT_SUFFIX = "_seconds";

    public GrobidDropwizardExports(MetricRegistry registry) {
        super(registry, new SnakeCaseSampleBuilder());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> out = new ArrayList<>();
        for (MetricFamilySamples mfs : super.collect()) {
            // Names are already snake_cased by SnakeCaseSampleBuilder at this point.
            if (mfs.name.startsWith(JVM_PREFIX)) {
                continue; // redundant with hotspot DefaultExports; drops the _count gauges too
            }
            if (mfs.type == Type.COUNTER || !mfs.name.endsWith(RESERVED_SUFFIX)) {
                out.add(mfs); // genuine counters keep _total; everything else is already clean
                continue;
            }
            out.add(renameOffReservedSuffix(mfs));
        }
        return out;
    }

    /**
     * Rebuild a non-counter family whose name ends in the reserved {@code _total} suffix, renaming
     * the family and every sample to use {@link #REPLACEMENT_SUFFIX} instead. Every SUMMARY sample
     * name (base, {@code _count}, {@code _sum}, quantiles) starts with the family name, so the tail
     * after the family name is preserved verbatim.
     */
    private static MetricFamilySamples renameOffReservedSuffix(MetricFamilySamples mfs) {
        String newName = mfs.name.substring(0, mfs.name.length() - RESERVED_SUFFIX.length())
                + REPLACEMENT_SUFFIX;
        List<Sample> renamed = new ArrayList<>(mfs.samples.size());
        for (Sample s : mfs.samples) {
            String sampleName = newName + s.name.substring(mfs.name.length());
            renamed.add(
                    new Sample(
                            sampleName, s.labelNames, s.labelValues, s.value, s.timestampMs));
        }
        return new MetricFamilySamples(newName, mfs.type, mfs.help, renamed);
    }
}
