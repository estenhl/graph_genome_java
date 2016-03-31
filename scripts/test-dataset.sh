#!/bin/bash

DIR=~/skole/master/graph-genome
CONTAINER_ID=10bf4fbece7b

# Create local test data
filename=$1
num=$2
mismatches=$3
prob=$4

IFS='.' read -r -a array <<< "$1"
read_dir=${array[0]}
mkdir $read_dir
java -jar $DIR/target/read-generator.jar reads file=$filename out=$read_dir.reads num=$num
/bin/bash $DIR/scripts/reads-to-fastas.sh $read_dir.reads $read_dir
cp $filename $read_dir/$filename
mkdir $read_dir-vg-stats
mkdir $read_dir-fuzzy-stats

# Copy test data to docker 
docker cp $read_dir $CONTAINER_ID:/sequence-graphs/$read_dir

# Create docker index
docker start $CONTAINER_ID
before=$(date +%s%N)
docker exec $CONTAINER_ID /sequence-graphs/createIndex/createIndex /sequence-graphs/$read_dir-index /sequence-graphs/$read_dir/$filename
after=$(date +%s%N)
time=$(($after - $before))
echo "Type: vg" > $read_dir-vg-stats/build.stats
echo "Nanoseconds: $time" >> $read_dir-vg-stats/build.stats
echo "Number of tests: $num" >> $read_dir-vg-stats/build.stats
echo "Number of allowed $mismatches" >> $read_dir-vg-stats/build.stats
echo "Mutation probability: $prob" >> $read_dir-vg-stats/build.stats

# Create fuzzy index
before=$(date +%s%N)
java -jar $DIR/target/graph-genome.jar index -i=$read_dir-fuzzy-index -if=$read_dir/$filename 
after=$(date +%s%N)
time=$(($after - $before))
echo "Type: fuzzy search" > $read_dir-fuzzy-stats/build.stats
echo "Nanoseconds: $time" >> $read_dir-fuzzy-stats/build.stats
echo "Number of tests: $num" >> $read_dir-fuzzy-stats/build.stats
echo "Number of allowed $mismatches" >> $read_dir-fuzzy-stats/build.stats
echo "Mutation probability: $prob" >> $read_dir-fuzzy-stats/build.stats

# Run alignment on test files
docker exec $CONTAINER_ID mkdir /sequence-graphs/$read_dir-vg-alignments
for i in `seq 1 $num`;
do
	before=$(date +%s%N)
    docker exec $CONTAINER_ID /sequence-graphs/createIndex/mapReads /sequence-graphs/$read_dir-index --reference /sequence-graphs/$read_dir/$filename --fastas /sequence-graphs/$read_dir/$i.txt --alignment /sequence-graphs/$read_dir-vg-alignments/$i.alignment --mismatches $mismatches
    after=$(date +%s%N)
    time=$(($after - $before))
    echo "Nanoseconds: $time" > $read_dir-vg-stats/$i.stats
    java -jar $DIR/target/graph-genome.jar align -i=$read_dir-fuzzy-index -af=$read_dir/$i.txt -t=$mismatches > $read_dir-fuzzy-stats/$i.stats
done 

#Copy alignments locally and clean up docker
docker cp $CONTAINER_ID:/sequence-graphs/$read_dir-vg-alignments .
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir-index
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir-vg-alignments

# Score alignments
for i in `seq 1 $num`;
do
	/bin/bash score-sg-alignment.sh $read_dir-vg-alignments/$i.alignment $read_dir-vg-stats/$i.stats
done

# Print summaries
/bin/bash summarize-stats.sh $read_dir $num

# Clean up locally
rm -rf $read_dir
rm -rf $read_dir-vg-alignments
rm -rf $read_dir-vg-stats
rm -rf $read_dir-fuzzy-stats
rm $read_dir-fuzzy-index
rm $read_dir.reads