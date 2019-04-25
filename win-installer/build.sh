#!/bin/sh

export NORMALIZED_PRAVDA_VERSION=$(echo $PRAVDA_VERSION | sed 's/[a-z-]//g')

echo "Packing windows application..."

zip -r "PravdaSDK-v${NORMALIZED_PRAVDA_VERSION}.zip" stage pravda.wxs set-platform.xslt

echo "Done packing"