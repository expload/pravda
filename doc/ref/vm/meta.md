# Meta information in Pravda bytecode

Pravda bytecode may contain additional information for disassembler, code generator and other tools that read and interpreter Pravda bytecode.  
This information is called `meta`. 

## Meta

There're 5 kinds of `meta` for the moment:
- `label_def "<string>"` that marks definition of a label;
- `label_use "<string>"` that marks usage of a label;
- `program_name "<string>"` that contains a name of a _program_;
- `method <struct>` (see `struct` definition in [data spec](data.md)).
- `source_mark <struct>` that specifies information about source files.
- `translator_mark "<string"` contains translation specific information.
- `custom "<string>"` that contains arbitrary information. 