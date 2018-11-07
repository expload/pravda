FROM openjdk:8u171

WORKDIR /faucet

COPY ./faucet/target/universal/stage/lib lib
COPY ./faucet/target/universal/stage/bin bin

COPY docker/images/pravda-faucet/entry.sh /faucet

ENTRYPOINT [ "/faucet/entry.sh" ]
