#!/bin/sh

set -e

echo "Starting node"

exec java -classpath /node/lib/"*" -Dconfig.file=/node/application.conf pravda.node.launcher $@
