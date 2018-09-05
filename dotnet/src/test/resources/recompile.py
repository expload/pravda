#!/usr/bin/python3
import glob
import subprocess
import os
import shutil

tmp_dir = "/tmp/pravda"

if not os.path.exists(tmp_dir):
    os.makedirs(tmp_dir)

def in_tmp_dir(path):
    return os.path.join(tmp_dir, path)

def run_if_changed(name, inputs, script, outputs):
    input_mtime = max(map(os.path.getmtime, inputs))
    output_mtime = min(map(os.path.getmtime, outputs))

    if input_mtime > output_mtime:
        print(f"Executing {name}...")
        for input in inputs:
            shutil.copy2(input, tmp_dir)
        subprocess.call(script(inputs, outputs))
        for output in outputs:
            shutil.copy2(output, ".")


def compile_dll(inputs, outputs):
    return ["csc", in_tmp_dir(inputs(0)),
            "-target:library",
            f"-out:{in_tmp_dir(outputs(0))}"]

def compile_exe(inputs, outputs):
    return ["csc", in_tmp_dir(inputs(0)),
            f"-reference:{in_tmp_dir(inputs(1))}",
            f"-out:{in_tmp_dir(outputs(0))}"]

def compile_exe_pdb(inputs, outputs):
    compile_exe(inputs, outputs[:1]) + ["-debug:portable", f"-pdb:{in_tmp_dir(outputs(1))}"]

run_if_changed("expload.dll compilation", ["expload.cs"], compile_dll, ["expload.dll"])

for filename in ["arithmetics", "array", "closure", "compare", "error", "hello_world",
                 "if", "inheritance", "loop", "loop_nested", "method_calling", "objects",
                 "stdlib", "strings", "system", "zoo_program"]:
    run_if_changed(f"{filename} compilation", [f"{filename}.cs", "expload.dll"], compile_exe, [f"{filename}.exe"])

for filename in ["smart_program"]:
    run_if_changed(f"{filename} compilation", [f"{filename}.cs", "expload.dll"], compile_exe_pdb, [f"{filename}.exe", f"{filename}.pdb"])


for file in glob.glob("*.cs"):
    filename = file[:-3]
    if file in exclude:
        continue

    print("Compiling " + file + "...")

    shutil.copy2(file, tmp_dir)

    args = ["csc", in_tmp_dir(file),
            "-reference:" + in_tmp_dir("expload.dll"),
            "-out:" + in_tmp_dir(filename + ".exe")]
    if file in with_pdb:
        args.append("-debug:portable")
    subprocess.call(args)

    shutil.copy2(in_tmp_dir(filename + ".exe"), ".")