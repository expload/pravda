from gcr.io/time-coin/sbt:latest as builder

workdir /build

# It's necessary for correct work of sbt-git plugin
copy .git .git

copy cli     cli
copy codegen codegen
copy common  common
copy dotnet  dotnet
copy node-db node-db
copy node    node
copy vm-api  vm-api
copy vm-asm  vm-asm
copy vm      vm
copy yopt    yopt

copy project project
copy build.sbt build.sbt

run cd /build && SBT_OPTS="-Xmx2G -Xms2G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss2M" sbt node/stage

from openjdk:8u171

workdir /node

copy --from=builder build/node/target/universal/stage/lib lib
copy --from=builder build/node/target/universal/stage/bin bin

copy docker/images/expload-pravda-node/application.conf /node
copy docker/images/expload-pravda-node/entry.sh /node

entrypoint [ "/node/entry.sh" ]
