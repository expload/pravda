# Meta in Bytecode

Pravda bytecode may contain additional information for the disassembler, code generator and other tools that read and interpret Pravda bytecode. This information is called `meta`.

There are currently 5 types of `meta`:
- `label_def "<string>"` that marks the definition of a label;
- `label_use "<string>"` that marks the usage of a label;
- `program_name "<string>"` that contains the name of a _program_;
- `method <struct>` (see `struct` definition in [data spec](data.md)).
- `source_mark <struct>` that specifies information about source files.
- `translator_mark "<string"` contains translation specific information.
- `custom "<string>"` contains arbitrary information.
