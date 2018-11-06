FROM openjdk:8u171

WORKDIR /xcoins-receiving

COPY ./services/xcoins-receiving/target/universal/stage/lib lib
COPY ./services/xcoins-receiving/target/universal/stage/bin bin

COPY docker/images/xcoins-receiving/entry.sh /xcoins-receiving

ENTRYPOINT [ "/xcoins-receiving/entry.sh" ]
