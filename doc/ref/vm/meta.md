# Meta information in Pravda bytecode

Pravda bytecode may contain additional information for disassembler, code generator and other tools that read and interpreter Pravda bytecode.  
This information is called `meta`. 

## Meta

There're 5 kinds of `meta` for the moment:
- `label_def "<string>"` that marks definition of a label;
- `label_use "<string>"` that marks usage of a label;
- `program_name "<string>"` that contains a name of a _program_;
- `method <struct>` (see `struct` definition in [data spec](data.md)). More about this in the next chapter.
- `custom "<string>"` that contains arbitrary information. 

### Method meta
`<struct>` in `method` must contain two pairs `int8(-1): <method_name>` and 
`int8(-2): int8(<type_of_return_value>)` (See definition of `type` in [data spec](data.md)). 


All other pairs in `<struct>` describe arguments of the method. 
They packed in the following way: `int8(2 * i): int8(<type_of_ith_argument>)`, `int8(2 * i + 1): utf8(<name_of_ith_argument>)`.
Name of the argument is optional, pair with `int(2 * i + 1)` key may not exist even if there's `int(2 * i)` key. 