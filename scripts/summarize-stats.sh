#!/bin/bash

filename=$1
num=$2

graph_size=0
sequence_length=0
min_po_msa=$(date +%s%N)
max_po_msa=0
total_po_msa=0
min_fuzzy=$(date +%s%N)
max_fuzzy=0
total_fuzzy=0
errors_fuzzy=0
min_sg=$(date +%s%N)
max_sg=0
total_sg=0
errors_sg=0
for i in `seq 1 $num`;
do
	size=($(tail -4 $filename-fuzzy-stats/$i.stats | head -1))
	length=($(tail -5 $filename-fuzzy-stats/$i.stats | head -1))
	graph_size=${size[2]}
	sequence_length=${length[2]}

	po_msa_time=($(tail -2 $filename-fuzzy-stats/$i.stats | head -1))
	po_msa_score=($(tail -3 $filename-fuzzy-stats/$i.stats | head -1))
	if [ ${po_msa_time[1]} -gt $max_po_msa ]; then
		max_po_msa=${po_msa_time[1]}
	fi
	if [ ${po_msa_time[1]} -lt $min_po_msa ]; then
		min_po_msa=${po_msa_time[1]}
	fi
	total_po_msa=$(($total_po_msa + ${po_msa_time[1]}))

	fuzzy_time=($(tail -8 $filename-fuzzy-stats/$i.stats | head -1))
	fuzzy_score=($(tail -9 $filename-fuzzy-stats/$i.stats | head -1))
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

	sg_time=($(tail -2 $filename-vg-stats/$i.stats | head -1))
	sg_score=($(tail -1 $filename-vg-stats/$i.stats | head -1))
	if [ ${sg_time[1]} -gt $max_sg ]; then
		max_sg=${sg_time[1]}
	fi
	if [ ${sg_time[1]} -lt $min_sg ]; then
		min_sg=${sg_time[1]}
	fi
	if [ ${sg_score[1]} -ne ${po_msa_score[1]} ]; then
		errors_sg=$(($sg_fuzzy + 1))
	fi
	total_sg=$(($total_sg + ${sg_time[1]}))
done 

echo "Graph size: $graph_size" > $filename.summary
echo "Sequence length: $sequence_length" >> $filename.summary
echo "Min. PO-MSA time: $min_po_msa" >> $filename.summary
echo "Max. PO-MSA time: $max_po_msa" >> $filename.summary
echo "Avg. PO-MSA time: $(($total_po_msa / $num))" >> $filename.summary
echo "Min. Fuzzy time: $min_fuzzy" >> $filename.summary
echo "Max. Fuzzy time: $max_fuzzy" >> $filename.summary
echo "Avg. Fuzzy time: $(($total_fuzzy / $num))" >> $filename.summary
echo "Fuzzy errors: $errors_fuzzy" >> $filename.summary
echo "Max. sg time: $max_sg" >> $filename.summary
echo "Min. sg time: $min_sg" >> $filename.summary
echo "Avg. sg time: $(($total_sg / $num))" >> $filename.summary
echo "sg errors: $errors_sg" >> $filename.summary