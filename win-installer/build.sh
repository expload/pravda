#!/bin/sh

set -e
set -v

export WINEPREFIX=/root/.wine

wine /wix/heat.exe dir stage -t set-platform.xslt -gg -cg PravdaCLIComponent -ke -srd -dr PravdaCLI -sfrag -o PravdaCLI.wxs
wine /wix/candle.exe PravdaCLI.wxs
wine /wix/candle.exe pravda.wxs
wine /wix/light.exe -b stage PravdaCLI.wixobj pravda.wixobj -sval -o PravdaSDK.msi

mv PravdaSDK.msi PravdaSDK-$PRAVDA_VERSION.msi
