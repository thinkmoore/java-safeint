#!/bin/bash

javafile="${BASH_ARGV}"

filename=$(basename "${javafile}")
extension="${filename##*.}"
filename="${filename%.*}"

mkdir -p tmp
javac -cp ../rt-classes -d tmp "${javafile}"
java -cp ../rt-classes:tmp C > "tmp/${filename}.testout" 2>&1

diff "tmp/${filename}.testout" "refout/${filename}.refout" >/dev/null
if [ $? -ne 0 ]; then
  echo "Output differed from reference" >&2
  diff "tmp/${filename}.testout" "refout/${filename}.refout" >&2
fi
