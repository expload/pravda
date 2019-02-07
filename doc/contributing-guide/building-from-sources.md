# Building from sources

Ensure that JDK 1.8 or higher is installed in your system. We use SBT native packager to produce runnable distros for each tool packed in
compressed archives.

```
$ ./sbt cli/universal:stage
$ ./cli/target/universal/stage/bin/pravda
```

To build archive just run `sbt cli/universal:packageZipTarball` in
the root of project. This will create necessary tgz-archive of
the Pravda CLI in the `cli/target`.
