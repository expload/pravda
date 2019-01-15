#!/bin/sh

if [ -z "$PRAVDA_COIN_HOLDER" ]
then
  echo "Please specify public key in \$PRAVDA_COIN_HOLDER to perform initial coin distribution"
  exit 1
else
  echo "Giving money to $PRAVDA_COIN_HOLDER"
fi

sed -i "s/PRAVDA_COIN_HOLDER/$PRAVDA_COIN_HOLDER/g" /pravda-cli/coin-distr.json

if [ ! -d ${PRAVDA_DATA} ]; then
  echo "${PRAVDA_DATA} does not exist, creating"
  mkdir -p ${PRAVDA_DATA}
fi

if [ ! -f /node-data/initialized ]; then
  echo "Initializing node"
  rm -rf ${PRAVDA_DATA}/*
  bin/pravda node init --data-dir ${PRAVDA_DATA} --coin-distribution coin-distr.json
  echo yes > /node-data/initialized
fi

echo "Starting node"
bin/pravda node run --data-dir ${PRAVDA_DATA}
