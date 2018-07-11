#!/bin/sh

set -e

if [ -z "$PRAVDA_COIN_HOLDER" ]
then
  echo "Please specify public key in \$PRAVDA_COIN_HOLDER to perform initial coin distribution"
  exit 1
else
  echo "Giving money to $PRAVDA_COIN_HOLDER"
fi

sed -i "s/PRAVDA_COIN_HOLDER/$PRAVDA_COIN_HOLDER/g" /pravda-cli/coin-distr.json

if [ -z "$(ls -A /node-data)" ]; then
  echo "/node-data does not exist, creating"
  mkdir -p /node-data
fi

if [ ! -f /node-data/initialized ]; then
  echo "Initializing node"
  rm -rf /node-data/*
  /pravda-cli/bin/pravda node init --data-dir /node-data --coin-distribution coin-distr.json
  echo yes > /node-data/initialized
fi

echo "Starting node"
export TC_CONFIG_FILE=/pravda-cli/application.conf
/pravda-cli/bin/pravda node run --data-dir /node-data
