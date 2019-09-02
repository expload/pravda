# Building from Sources

Ensure that JDK 1.8 or higher is installed in your system. We use the SBT native packager to produce runnable distros for each tool packed in the compressed archives.

```
$ ./sbt cli/universal:stage
$ ./cli/target/universal/stage/bin/pravda
```

To build an archive, just run `sbt cli/universal:packageZipTarball` in the root of the project. This will create the necessary tgz-archive of the Pravda CLI in the `cli/target`.
