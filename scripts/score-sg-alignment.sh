#!/bin/bash

alignment_file=$1
output_file=$2

prev=-1
score=0

cat $alignment_file | ( while read line
do
	IFS=$'\t' read -r -a array <<< "$line"
	if [ ${#array[@]} -eq "5" ]; then
		if [ $prev -eq "-1" ]; then
			prev=${array[1]}
			if [ ${array[4]} -eq "1" ]; then
				let score=$(($score + 1))
			fi
			continue
		fi
		if [ ${array[4]} -eq "1" ]; then
			let score=$(($score + 1))
		fi
		distance=$((${array[1]} - $prev))
		distance=$(($distance - 1))
		prev=${array[1]}
		if [ $distance -lt "0" ]; then
			let score=$(($score - $distance))
			continue
		fi
		let score=$(($score + $distance))
		continue
	fi
	let score=$(($score + 1))
done

echo "Score: $score" >> $2)