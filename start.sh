#!/bin/bash

files="Main.java"
entry="Main"

javac $files
if [ $? -ne 0 ]; then
    echo "Compilation failed. Exiting."
    exit 1
fi

java $entry
