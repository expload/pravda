How to add new opcode to VM
===========================

While Pravda is under heavy development we can fell ourselves free to add new opcodes to VM. However it doesn't mean that individual developer can do by its own decision. Addition of eveny new opcode should be rationale and disscussed with collegues in chat or on Github issue. When team agreed with supplementation of new opcode you can use this checklist to implement it in a code.

1. Add opcode to `pravda.vm.Opcodes` in `vm-api` module. Selection a section carefully. Use hex number next to last opcode in the section.
2. Add implementation of opcode to module of `pravda.vm.operations` package correspondent to the section. Do not forget to add documentation for method.
3. Add selection of implementation to `pravda.vm.impl.VmImpl`.
4. Add mnemonic declaration to `pravda.vm.asm.Operation` to orphans. If declaration of opcode is complex (take arguments) take a look to `pravda.vm.asm.PravdaAssembler`.
5. Write few test cases to `/vm/src/test/resources`
