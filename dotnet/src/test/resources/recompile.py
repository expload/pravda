#!/usr/bin/python3
import glob
import subprocess
import os
import shutil

exclude = ["expload.cs"]
with_pdb = ["smart_program.cs"]
tmp_dir = "/tmp/pravda"


def in_tmp_dir(path):
    return os.path.join(tmp_dir, path)


if not os.path.exists(tmp_dir):
    os.makedirs(tmp_dir)

print("Compiling  expload.cs...")
shutil.copy2("expload.cs", tmp_dir)
subprocess.call(["csc", in_tmp_dir("expload.cs"),
                 "-target:library",
                 "-out:" + in_tmp_dir("expload.dll")])

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