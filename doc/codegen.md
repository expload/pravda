#Code generation for Unity3D

Pravda project is able to generate auxiliary code for Unity3D that provides convenient way to call _program's_ methods.  
It uses [`meta`](ref/vm/meta.md) information from the given bytecode to detect and analyse methods of the _program_. 

## How to generate code

```
pravda gen unity --input input.pravda --dir output/
```

### BigInteger

Unity3D doesn't have built-in BigInteger class, but Pravda _programs_ use BigInteger to work with blockchain addresses, 
so Pravda Code Generator includes BigInteger implementation as separate file by default. 

To disable this behaviour and exclude BigInteger file from generated sources you should specify it by `--exclude-big-integer` flag. 