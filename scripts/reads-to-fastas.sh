#!/bin/bash

counter=1
cat $1 | while read line
do
	echo ">Test fasta $num" > $2/$counter.txt
	echo $line >> $2/$counter.txt
	counter=$((counter+1))
done
