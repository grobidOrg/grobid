#!/bin/bash

if [ $# -lt 1 ]; then
    echo 'usage: <path to config.yaml>'
    exit 1
fi

config=$1

echo "Starting grobid service with config $config"

ARCH=$(uname -m)
if [ "$ARCH" = "aarch64" ]; then
    LIB_DIR="../grobid-home/lib/lin_arm-64"
else
    LIB_DIR="../grobid-home/lib/lin-64"
fi

java -Djava.library.path="$LIB_DIR" -jar build/libs/grobid-service-*-onejar.jar server $config || exit $?
