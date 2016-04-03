#!/bin/bash

index=$1
reference=$2
fastas=$3
alignment=$4
mismatches=$5
output=$6

before=$(date +%s%N)
/sequence-graphs/createIndex/mapReads $index --reference $reference --fastas $fastas --alignment $alignment --mismatches $mismatches
after=$(date +%s%N)
echo "Nanoseconds: $(($after - $before))" > $output