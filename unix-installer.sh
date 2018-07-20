#!/bin/bash

set -e

# PS1='$ \w\[\033[0;32m\] [$(git branch 2>/dev/null | grep "^*" | colrm 1 2)\[\033[0;32m\]]\[\033[0m\033[0;32m\] \$\[\033[0m\033[0;32m\]\[\033[0m\] '
RED='\033[0;31m'
GREEN='\033[1;32m'
DARK_GRAY='\033[1;30m'
NC='\033[0m' # No Color

VERSION=$(curl --silent "https://api.github.com/repos/expload/pravda/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
OUT=pravda-$VERSION
PACKAGE=PravdaSDK-$VERSION.tgz

mkdir -p /tmp/pravda-install
cd /tmp/pravda-install

echo "Installing to $HOME/.pravda..."
echo " * Downloading $PACKAGE"
curl -L -# --fail https://github.com/expload/pravda/releases/download/$VERSION/$PACKAGE -o $PACKAGE
echo -en "\033[2A\033[K\r" # Erase cURL progress bar
echo " * Extracting $PACKAGE"
tar -xvzf $PACKAGE 2>&1 |
  while read line; do
    x=$((x+1))
    echo -en "$x files extracted...\r"
  done

# Macos doesn't read .bashrc on shell login
# so force to use .bash_profile instead.
case $(uname -s) in
    Darwin*)    PROFILE=$HOME/.bash_profile;;
    *)          PROFILE=$HOME/.bashrc
esac

echo " * Moving data to $HOME/.pravda"
rm -rf $HOME/.pravda
mv $OUT $HOME/.pravda

if ! grep -q .pravda/bin "$PROFILE";
then
    echo ' * Adding "PATH=~/.pravda/bin/:$PATH" to $PROFILE.'
    printf '\nexport PATH="$HOME/.pravda/bin/:$PATH"\n' >> $PROFILE
else
    echo -e "$DARK_GRAY * ~/.pravda/bin already added to PATH$NC"
fi

echo " * Removing temporary directory"
rm -rf /tmp/pravda-install
printf "$GREEN * Done! Use 'pravda' to access CLI. Type 'source $PROFILE' or restart shell to apply changes.$NC\n"
