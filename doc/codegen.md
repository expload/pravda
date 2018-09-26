# Code generation for Unity3D

Pravda project is able to generate auxiliary code for [Unity](https://unity3d.com/) that provides convenient way to call _program's_ methods.  
It uses [`meta`](ref/vm/meta.md) information from the given bytecode to detect and analyse methods of the _program_. 

## How to generate code

```
pravda gen unity --input input.pravda --dir output-dir/
```

