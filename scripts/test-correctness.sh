#!/bin/bash

for i in `seq 0 5`;
do
    bash test-po_msa-fuzzy.sh 35k.fasta 100 $i 0.0
    cp 35k.summary stats/correctness/35k-0-$i.stats
    bash test-po_msa-fuzzy.sh 35k.fasta 100 $i 0.01
    cp 35k.summary stats/correctness/35k-1-$i.stats
    bash test-po_msa-fuzzy.sh 35k.fasta 100 $i 0.02
    cp 35k.summary stats/correctness/35k-2-$i.stats
    bash test-po_msa-fuzzy.sh 35k.fasta 100 $i 0.03
    cp 35k.summary stats/correctness/35k-3-$i.stats
    bash test-po_msa-fuzzy.sh 35k.fasta 100 $i 0.04
    cp 35k.summary stats/correctness/35k-4-$i.stats
    bash test-po_msa-fuzzy.sh 35k.fasta 100 $i 0.05
    cp 35k.summary stats/correctness/35k-5-$i.stats
done 
