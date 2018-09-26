# How to add new functionality to VM

While Pravda is under heavy development we can feel ourselves free to add new opcodes and functions to VM. However it doesn't mean that individual developer can do this by their own decision. Addition of every new opcode should be rationale and should be discussed with colleagues in the chat or in the Github issue. When team agree on addition of the new opcode you can use this guide to implement it in the code.

Do not forget to [generate docs](gen-doc.md)!
## Opcodes
1. Add opcode to `pravda.vm.Opcodes` in `vm-api` module. Select a section carefully. Use hex number next to last opcode in the section.
2. Add implementation of the opcode to the module from `pravda.vm.operations` package that corresponds to the selected section. Do not forget to add documentation for the method.
3. Add appropriate pattern-match branch to `pravda.vm.impl.VmImpl`.
4. Add mnemonic declaration to `pravda.vm.asm.Operation` orphans. If opcode is complex (takes arguments, for example) take a look to `pravda.vm.asm.PravdaAssembler`.
5. Write few test cases in `/vm/src/test/resources`

## Standard Library
1. Add implementation to `pravda.vm.std` in `vm` module. For id use hex number next to last added standard library function.
2. Add reference to `pravda.vm.StandardLibrary` object.
3. Write few test cases in `/vm/src/test/resources`
