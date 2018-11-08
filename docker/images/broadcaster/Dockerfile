FROM openjdk:8u171

WORKDIR /broadcaster

COPY ./services/broadcaster/target/universal/stage/lib lib
COPY ./services/broadcaster/target/universal/stage/bin bin

COPY docker/images/broadcaster/entry.sh /broadcaster

EXPOSE 5000

ENTRYPOINT [ "/broadcaster/entry.sh" ]
