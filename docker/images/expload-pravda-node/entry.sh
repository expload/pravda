#!/bin/sh

set -e

if [ -z "$PRAVDA_COIN_HOLDER" ]
then
  echo "Please specify public key in \$PRAVDA_COIN_HOLDER to perform initial coin distribution"
  exit 1
else
  echo "Giving money to $PRAVDA_COIN_HOLDER"
fi

if [ -z "$PRAVDA_COIN_HOLDER_2" ]
then
  echo "Please specify public key in \$PRAVDA_COIN_HOLDER_2 to perform initial coin distribution"
  exit 1
else
  echo "Giving money to $PRAVDA_COIN_HOLDER_2"
fi

# More specific regular expressions should follow before
sed -i "s/PRAVDA_COIN_HOLDER_2/$PRAVDA_COIN_HOLDER_2/g" /pravda-cli/coin-distr.json
sed -i "s/PRAVDA_COIN_HOLDER/$PRAVDA_COIN_HOLDER/g" /pravda-cli/coin-distr.json

if [ -z "$(ls -A ${PRAVDA_DATA})" ]; then
  echo "${PRAVDA_DATA} does not exist, creating"
  mkdir -p ${PRAVDA_DATA}
fi

if [ ! -f /node-data/initialized ]; then
  echo "Initializing node"
  rm -rf ${PRAVDA_DATA}/*
  /pravda-cli/bin/pravda node init --data-dir ${PRAVDA_DATA} --coin-distribution coin-distr.json
  echo yes > ${PRAVDA_DATA}/initialized
fi

echo "Starting node"
export TC_CONFIG_FILE=/pravda-cli/application.conf

exec java -classpath /pravda-cli/lib/"*" -Dconfig.file=/pravda-cli/application.conf pravda.node.launcher $@
