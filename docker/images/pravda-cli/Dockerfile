FROM openjdk:8u171

WORKDIR /pravda-cli

COPY ./cli/target/universal/stage/lib lib
COPY ./cli/target/universal/stage/bin bin

COPY docker/images/pravda-cli/coin-distr.json /pravda-cli
COPY docker/images/pravda-cli/entry.sh /pravda-cli

ENTRYPOINT [ "/pravda-cli/entry.sh" ]
