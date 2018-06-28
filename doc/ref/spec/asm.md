# Pravda Assembler

Pravda assembler (pasm) is text representation of Pravda VM bytecode.  This is basic program. Let's consider simple program written with pasm.

```
/* My program */
jump @main
@ok:
push "good"
jump @end
@main:
push 2
mul
push 8
gt
jumpi @ok
push "bad"
@end:
```

This will pops item from stack multiplies it with 2. If it less that 8 push "good" to stack, else push "bad".

Pasm is very simple. 

1. Define labels: `@my_label:`.
2. Jump to label: `jump @my_label`, `jumi @my_label`, `call @my_label`.
3. Push primitive to stack:`push [primitive]`. Or put item to heap: `new [data]` (in this case reference to data will be pushed to stack).
4. Write comments: `/* a comment */`.
5. Work with structs: `struct_mut [primitive]`, `struct_get [primitive]`. This will produce `STRUCT_MUT_STATIC` and `STRUCT_GET_STATIC` opecodes which are taking key from bytecode. You can write `struct_mut` or `struct_get` without literal. In this case `STRUCT_MUT` and `STRUCT_GET` opcodes will be used and key will be taken from stack.
6. Write other [opcodes](opcodes.md).

See also [string data encoding](data.md).