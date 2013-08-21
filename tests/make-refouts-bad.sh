#!/bin/bash

if [ $# -eq 0 ]
then
    echo "Usage ./make-refout-bad.sh <.si file>"
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

../bin/safeintc -d tmp -assert -noserial -c $file
javac -cp ../rt-classes -d tmp "tmp/${asjava}"
java -cp ../rt-classes:tmp C > "refout/${filename}.refout" 2>&1
