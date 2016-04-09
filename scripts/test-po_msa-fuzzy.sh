#!/bin/bash

filename=$1
num=$2
mismatches=$3
prob=$4

# Create local test data
IFS='.' read -r -a array <<< "$1"
read_dir=${array[0]}
mkdir $read_dir
java -jar ../target/read-generator.jar reads file=$filename out=$read_dir.reads num=$num prob=$prob
/bin/bash reads-to-fastas.sh $read_dir.reads $read_dir
cp $filename $read_dir/$filename
mkdir $read_dir-fuzzy-stats
mkdir $read_dir-po_msa-stats

for i in `seq 1 $num`;
do
    echo "Run: $i"
    before=$(date +%s%N)
    java -Xmx4096m -Xms4096m -jar ../target/graph-genome.jar build-and-align -if=$filename -af=$read_dir/$i.txt -em=$mismatches -t=fuzzy -pa=true > $read_dir-fuzzy-stats/$i.stats
    after=$(date +%s%N)
    echo "Tool time: $(($after - $before))" >> $read_dir-fuzzy-stats/$i.stats
    before=$(date +%s%N)
    java -Xmx4096m -Xms4096m -jar ../target/graph-genome.jar build-and-align -if=$filename -af=$read_dir/$i.txt -em=$mismatches -t=po_msa > $read_dir-po_msa-stats/$i.stats
    after=$(date +%s%N)
    echo "Tool time: $(($after - $before))" >> $read_dir-po_msa-stats/$i.stats
done 

min_po_msa=$(date +%s%N)
max_po_msa=0
total_po_msa=0
min_fuzzy=$(date +%s%N)
max_fuzzy=0
total_fuzzy=0
errors_fuzzy=0

for i in `seq 1 $num`;
do
	po_msa_time=($(tail -3 $read_dir-po_msa-stats/$i.stats | head -1))
	po_msa_score=($(tail -4 $read_dir-po_msa-stats/$i.stats | head -1))

	if [ ${po_msa_time[1]} -gt $max_po_msa ]; then
		max_po_msa=${po_msa_time[1]}
	fi
	if [ ${po_msa_time[1]} -lt $min_po_msa ]; then
		min_po_msa=${po_msa_time[1]}
	fi
	total_po_msa=$(($total_po_msa + ${po_msa_time[1]}))

	fuzzy_time=($(tail -3 $read_dir-fuzzy-stats/$i.stats | head -1))
	echo "Found fuzzy time: ${fuzzy_time[1]}"
	fuzzy_score=($(tail -4 $read_dir-fuzzy-stats/$i.stats | head -1))
	if [ ${fuzzy_time[1]} -gt $max_fuzzy ]; then
		max_fuzzy=${fuzzy_time[1]}
	fi
	if [ ${fuzzy_time[1]} -lt $min_fuzzy ]; then
		min_fuzzy=${fuzzy_time[1]}
	fi
	if [ ${fuzzy_score[1]} -ne ${po_msa_score[1]} ]; then
		errors_fuzzy=$(($errors_fuzzy + 1))
	fi
	total_fuzzy=$(($total_fuzzy + ${fuzzy_time[1]}))

done 
echo "Input file: $filename" >> $read_dir.summary
echo "Number of tests: $num" >> $read_dir.summary
echo "Allowed mismatches: $mismatches" >> $read_dir.summary
echo "Mutation probability: $prob" >> $read_dir.summary
echo "" >> $read_dir.summary
echo "Fuzzy index time: ${fuzzy_build_time[1]}" >> $read_dir.summary
echo "Min. PO-MSA time: $min_po_msa" >> $read_dir.summary
echo "Max. PO-MSA time: $max_po_msa" >> $read_dir.summary
echo "Avg. PO-MSA time: $(($total_po_msa / $num))" >> $read_dir.summary
echo "Min. Fuzzy time: $min_fuzzy" >> $read_dir.summary
echo "Max. Fuzzy time: $max_fuzzy" >> $read_dir.summary
echo "Avg. Fuzzy time: $(($total_fuzzy / $num))" >> $read_dir.summary
echo "Fuzzy errors: $errors_fuzzy" >> $read_dir.summary

rm -rf $read_dir
rm -rf $read_dir-fuzzy-stats
rm -rf $read_dir-po_msa-stats
rm $read_dir.reads

