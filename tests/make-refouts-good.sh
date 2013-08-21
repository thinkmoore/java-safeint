#!/bin/bash

if [ $# -eq 0 ]
then
    echo "Usage ./make-refout-good.sh <.si file>"
    exit
fi

file=$1

mkdir -p tmp
mkdir -p refout
filename=$(basename "$file")
extension="${filename##*.}"
filename="${filename%.*}"
asjava="${filename}.java"
echo "Generating reference output for $file"
cp $file "tmp/${asjava}"
javac -d tmp "tmp/${asjava}"
java -cp tmp C > "refout/${filename}.refout" 2>&1
