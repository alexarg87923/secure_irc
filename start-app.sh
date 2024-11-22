#!/bin/bash

src_dir="src"
output_dir="bin"
entry="Main"

mkdir -p $output_dir

javac -d $output_dir $src_dir/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed. Exiting."
    exit 1
fi

java -cp $output_dir $entry
