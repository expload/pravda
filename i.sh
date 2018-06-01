#!/bin/bash

set -e

CURRENT_DIR=`pwd`
VERSION=$(curl --silent "https://api.github.com/repos/mytimecoin/pravda/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
OUT=pravda-$VERSION
PACKAGE=PravdaSDK-$VERSION.tgz

rm -rf /tmp/pravda-install
mkdir /tmp/pravda-install
cd /tmp/pravda-install

echo "Downloading $PACKAGE..."
curl -L -# -O --fail https://github.com/mytimecoin/pravda/releases/download/$VERSION/$PACKAGE -o $PACKAGE
tar -xvzf $PACKAGE

# Macos doesn't read .profile on shell login
# so force to use .bash_profile instead.
case $(uname -s) in
    Darwin*)    PROFILE=$HOME/.bash_profile;;
    *)          PROFILE=$HOME/.profile
esac

echo "Installing to $HOME/.pravda..."
rm -rf $HOME/.pravda
mv $OUT $HOME/.pravda
printf '\nexport PATH="$HOME/.pravda/bin/:$PATH"' >> $PROFILE
echo '"PATH=~/.pravda/bin/:..." was added to $PROFILE.'
source $PROFILE

echo "Removing temporary directory..."
rm -rf /tmp/pravda-install
cd $CURRENT_DIR
echo "Done."
