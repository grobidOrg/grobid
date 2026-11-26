#!/usr/bin/env python3
"""
Write the grobid.yaml used by an end-to-end evaluation run.

    configure_grobid.py --profile crf
    configure_grobid.py --profile delft --delft-install ~/delft
    configure_grobid.py --profile crf   --consolidation glutton --glutton-url http://glutton:8080

The `delft` profile starts from grobid-evaluation.yaml, the preset behind the published
benchmarks. The `crf` profile starts from the default grobid.yaml, so every model keeps
the engine it ships with.

By default (`--consolidation none`), the consolidation settings of the preset are left as
they are: the evaluation is then run with `-PconsolidateHeader=0`, which calls no
consolidation service. With `--consolidation glutton`, consolidation is switched to
biblio-glutton.

The file is rewritten without its comments: it is meant for a throw-away checkout.
"""

import argparse
import os

import yaml

CONFIG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "grobid-home", "config")
PRESETS = {"crf": "grobid.yaml", "delft": "grobid-evaluation.yaml"}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--profile", choices=sorted(PRESETS), required=True)
    parser.add_argument(
        "--consolidation",
        choices=["none", "glutton"],
        default="none",
        help="none: the evaluation runs without consolidation; glutton: header consolidation with biblio-glutton",
    )
    parser.add_argument("--glutton-url", help="biblio-glutton base URL (default: the one of grobid-evaluation.yaml)")
    parser.add_argument("--delft-install", help="DeLFT directory, holding resources-registry.json and data/db")
    parser.add_argument(
        "--concurrency",
        type=int,
        default=(os.cpu_count() or 4) + 1,
        help="GROBID concurrency; the evaluation processes PDFs on concurrency - 1 threads (default: CPUs + 1)",
    )
    parser.add_argument("--config-dir", default=CONFIG_DIR, help="grobid-home/config directory")
    args = parser.parse_args()
    if args.consolidation == "none" and args.glutton_url:
        parser.error("--glutton-url is meaningless with --consolidation none")

    with open(os.path.join(args.config_dir, PRESETS[args.profile])) as f:
        config = yaml.safe_load(f)

    grobid = config["grobid"]
    if args.consolidation == "glutton":
        glutton_url = args.glutton_url
        if not glutton_url:
            # whatever the profile, the reference instance is the one of the evaluation preset
            with open(os.path.join(args.config_dir, PRESETS["delft"])) as f:
                glutton_url = yaml.safe_load(f)["grobid"]["consolidation"]["glutton"]["url"]
        grobid["consolidation"]["service"] = "glutton"
        grobid["consolidation"]["glutton"]["url"] = glutton_url
    if args.delft_install:
        grobid["delft"]["install"] = os.path.abspath(os.path.expanduser(args.delft_install))
    grobid["concurrency"] = args.concurrency

    target = os.path.join(args.config_dir, "grobid.yaml")
    with open(target, "w") as f:
        yaml.safe_dump(config, f, sort_keys=False, default_flow_style=False)

    print(f"Wrote {os.path.normpath(target)} from {PRESETS[args.profile]} (profile: {args.profile})")
    if args.consolidation == "glutton":
        print(f"  consolidation: glutton at {grobid['consolidation']['glutton']['url']}")
    else:
        print("  consolidation: disabled (run the evaluation with -PconsolidateHeader=0)")
    print(f"  concurrency: {grobid['concurrency']}")
    print(f"  delft install: {grobid['delft']['install']}")
    for model in grobid["models"]:
        if model.get("engine") == "delft":
            print(f"  delft model: {model['name']} ({model.get('delft', {}).get('architecture')})")


if __name__ == "__main__":
    main()
