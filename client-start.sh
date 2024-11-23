#!/bin/bash

root_dir="client"
src="src"
src_dir="$root_dir/$src"
output_dir="$root_dir/bin"
dependencies="$root_dir/libs"
shared_src="shared/src"
entry="Main"

# make bin if it doesnt exist
mkdir -p "$output_dir"

# compile shared directory into bin
javac -d "$output_dir" "$shared_src"/*.java
if [ $? -ne 0 ]; then
    echo "Shared code compilation failed. Exiting."
    exit 1
fi

# compile client/src code
javac -cp "$output_dir:$dependencies/*" -d "$output_dir" "$src_dir"/*.java
if [ $? -ne 0 ]; then
    echo "Server code compilation failed. Exiting."
    exit 1
fi

# copy .env over to bin
if [ -f "$root_dir/.env" ]; then
    cp "$root_dir/.env" "$output_dir/"
else
    echo ".env file doesn't exist. Skipping."
fi

# go into bin directory
cd "$output_dir" || exit

# run the application
java -cp ".:$dependencies/*" "$entry"
