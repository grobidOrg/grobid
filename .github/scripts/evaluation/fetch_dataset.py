#!/usr/bin/env python3
"""
Download one end-to-end evaluation corpus, or one shard of it, from the Hugging Face dataset.

The dataset holds one directory per corpus and, under it, one directory per article (see
doc/End-to-end-evaluation.md). Articles are sorted by name and dealt round-robin, so the
shards of a corpus are disjoint, cover it entirely and are the same on every run.

    fetch_dataset.py --dataset PMC_sample_1943 --output data --shard 0 --shards 4
    fetch_dataset.py --dataset PMC_sample_1943 --output data --content gold

The files land under <output>/<dataset>/<article>/, the layout `jatsEval` expects.
"""

import argparse
import os
import random
import re
import sys
import time
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor

from huggingface_hub import HfApi, hf_hub_download

DEFAULT_REPO = "sciencialab/grobid-evaluation"
# how long a download waits, in total, for the Hugging Face rate limits to reset
RATE_LIMIT_WAIT = 60 * 60


def is_pdf(name):
    return name.lower().endswith(".pdf")


def is_gold(name):
    """Mirror of the gold file selection in EndToEndEvaluation."""
    if name.endswith(".nxml") or name.endswith(".pub2tei.tei.xml"):
        return True
    # bioRxiv, PLOS and eLife JATS files have a plain .xml extension; the output of
    # the tools under evaluation (GROBID TEI, pdfx) must not be taken for gold data
    return name.endswith(".xml") and not name.endswith(".tei.xml") and not name.endswith(".pdfx.xml")


def list_articles(api, repo, revision, dataset):
    """Return {article directory: [file path in repo, ...]} for a corpus."""
    articles = defaultdict(list)
    for entry in api.list_repo_tree(
        repo, path_in_repo=dataset, recursive=True, repo_type="dataset", revision=revision
    ):
        if not hasattr(entry, "size"):
            # a directory
            continue
        parts = entry.path.split("/")
        if len(parts) < 3:
            # a file directly under the corpus directory (e.g. a readme), not an article
            continue
        articles[parts[1]].append(entry.path)
    return articles


def rate_limit_reset(error):
    """Seconds until the rate limit of a 429 response resets, None for any other error."""
    # hf_hub_download raises a 429 on the metadata request as the cause of a LocalEntryNotFoundError
    while error is not None and getattr(error, "response", None) is None:
        error = error.__cause__
    response = getattr(error, "response", None)
    if response is None or response.status_code != 429:
        return None
    # e.g. RateLimit: "resolvers";r=0;t=55
    match = re.search(r"\bt=(\d+)", response.headers.get("RateLimit", ""))
    if match:
        return int(match.group(1))
    retry_after = response.headers.get("Retry-After", "")
    return int(retry_after) if retry_after.isdigit() else 60


def with_retries(what, call, deadline, attempts=5):
    """Return call(), waiting for the rate limits to reset until `deadline` (time.monotonic())."""
    attempt = 0
    while True:
        try:
            return call()
        except Exception as e:
            reset = rate_limit_reset(e)
            if reset is not None:
                # the limit is shared with the other requests: they all wait for the same reset,
                # spread over a few seconds so as not to hit it again together
                delay = reset + random.uniform(1, 10)
                if time.monotonic() + delay > deadline:
                    raise
                print(f"Rate limited on {what}, retrying in {delay:.0f}s", file=sys.stderr)
            else:
                # network errors
                attempt += 1
                if attempt == attempts:
                    raise
                delay = 5 * 2 ** (attempt - 1)
                print(f"{what} failed ({e}), retrying in {delay}s", file=sys.stderr)
            time.sleep(delay)


def download(repo, revision, path, output, deadline):
    return with_retries(
        f"download of {path}",
        lambda: hf_hub_download(repo, path, repo_type="dataset", revision=revision, local_dir=output),
        deadline,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dataset", required=True, help="corpus directory in the dataset, e.g. PMC_sample_1943")
    parser.add_argument("--output", required=True, help="directory under which <dataset>/ is created")
    parser.add_argument("--repo", default=DEFAULT_REPO, help=f"Hugging Face dataset (default: {DEFAULT_REPO})")
    parser.add_argument("--revision", default="main", help="branch, tag or commit of the dataset (default: main)")
    parser.add_argument("--shard", type=int, default=0, help="index of the shard to download (default: 0)")
    parser.add_argument("--shards", type=int, default=1, help="number of shards the corpus is split into (default: 1)")
    parser.add_argument(
        "--content",
        choices=["all", "gold"],
        default="all",
        help="'all' downloads the PDF and the gold XML, 'gold' only the gold XML (default: all)",
    )
    parser.add_argument("--workers", type=int, default=8, help="parallel downloads (default: 8)")
    args = parser.parse_args()

    if args.shards < 1 or not 0 <= args.shard < args.shards:
        parser.error(f"--shard must be in [0, {args.shards}), got {args.shard}")

    api = HfApi()
    deadline = time.monotonic() + RATE_LIMIT_WAIT
    articles = with_retries(
        f"listing of {args.dataset}", lambda: list_articles(api, args.repo, args.revision, args.dataset), deadline
    )
    if not articles:
        sys.exit(f"No article found under {args.dataset} in {args.repo}@{args.revision}")

    selected = [name for i, name in enumerate(sorted(articles)) if i % args.shards == args.shard]

    files = []
    for name in selected:
        for path in articles[name]:
            basename = os.path.basename(path)
            if is_gold(basename) or (args.content == "all" and is_pdf(basename)):
                files.append(path)

    print(
        f"{args.dataset}: shard {args.shard + 1}/{args.shards}, "
        f"{len(selected)} of {len(articles)} articles, {len(files)} files"
    )

    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        list(executor.map(lambda path: download(args.repo, args.revision, path, args.output, deadline), files))

    print(f"Downloaded under {os.path.join(args.output, args.dataset)}")


if __name__ == "__main__":
    main()
