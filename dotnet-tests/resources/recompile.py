#!/usr/bin/python3
import glob
import subprocess
import os
import shutil

tmp_dir = "/tmp/pravda"
dll_dir = "../../PravdaDotNet"

if not os.path.exists(tmp_dir):
    os.makedirs(tmp_dir)

def in_tmp_dir(path):
    return os.path.join(tmp_dir, os.path.basename(path))
def in_dll_dir(path):
    return os.path.join(dll_dir, os.path.basename(path))

def run_if_changed(name, inputs, script, outputs):

    def output_getmtime(f):
        if os.path.exists(f):
            return os.path.getmtime(f)
        else:
            return 0

    input_mtime = max(map(os.path.getmtime, inputs))
    output_mtime = min(map(output_getmtime, outputs))

    if input_mtime > output_mtime:
        print(f"Executing {name}...")
        for input in inputs:
            shutil.copy2(input, tmp_dir)
        print(script(inputs, outputs))
        subprocess.call(script(inputs, outputs))
        for output in outputs:
            shutil.copy2(in_tmp_dir(output), output)

def compile(inputs, outputs, flags):
    res = ["csc", in_tmp_dir(inputs[0]),
           f"-out:{in_tmp_dir(outputs[0])}"]

    if len(inputs) > 1:
        for ref in inputs[1:]:
            res += [f"-reference:{in_tmp_dir(ref)}"]

    return res + flags

def compile_dll(inputs, outputs):
    return compile(inputs, outputs, ["-target:library"])

def compile_exe(inputs, outputs):
    return compile(inputs, outputs, [])

def compile_exe_pdb(inputs, outputs):
    return compile_exe(inputs, outputs[:1]) + \
           ["-debug:portable", f"-pdb:{in_tmp_dir(outputs[1].rpartition('.')[0])}"]

run_if_changed("Pravda.dll compilation",
               [in_dll_dir("Pravda.cs")], compile_dll, [in_dll_dir("Pravda.dll")])

for filename in ["arithmetics",
                 "arrays",
                 "closure",
                 "compare",
                 "error",
                 "event",
                 "if",
                 "inheritance",
                 "loop",
                 "loop_nested",
                 "method_calling",
                 "objects",
                 "stdlib",
                 "strings",
                 "system",
                 "zoo_program"]:
    run_if_changed(f"{filename} compilation",
                   [f"{filename}.cs", in_dll_dir("Pravda.dll")], compile_exe, [f"{filename}.exe"])

for filename in ["smart_program", "hello_world"]:
    run_if_changed(f"{filename} compilation",
                   [f"{filename}.cs", in_dll_dir("Pravda.dll")], compile_exe_pdb, [f"{filename}.exe", f"{filename}.pdb"])

run_if_changed("pcall_program compilation",
               ["pcall_program.cs", in_dll_dir("Pravda.dll")], compile_dll, ["pcall_program.dll"])
run_if_changed("pcall compilation",
               ["pcall.cs", in_dll_dir("Pravda.dll"), "pcall_program.dll"], compile_exe, ["pcall.exe"])
