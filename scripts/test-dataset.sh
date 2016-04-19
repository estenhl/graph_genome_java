#!/bin/bash

DIR=../
CONTAINER_ID=10bf4fbece7b

filename=$1
num=$2
mismatches=$3
prob=$4

# Create local test data
IFS='.' read -r -a array <<< "$1"
read_dir=${array[0]}
mkdir $read_dir
java -jar $DIR/target/read-generator.jar reads file=$filename out=$read_dir.reads num=$num prob=$prob
/bin/bash $DIR/scripts/reads-to-fastas.sh $read_dir.reads $read_dir
cp $filename $read_dir/$filename
mkdir $read_dir-fuzzy-stats
mkdir $read_dir-po_msa-stats

# Copy test data to docker 
docker cp $read_dir $CONTAINER_ID:/sequence-graphs/$read_dir
docker cp test-sg.sh $CONTAINER_ID:/test-sg.sh

# Create docker index
docker start $CONTAINER_ID
before=$(date +%s%N)
docker exec $CONTAINER_ID /sequence-graphs/createIndex/createIndex /sequence-graphs/$read_dir-index /sequence-graphs/$read_dir/$filename
after=$(date +%s%N)
docker_build_time=$(($after - $before))

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
docker exec $CONTAINER_ID mkdir /sequence-graphs/$read_dir-sg-alignments
docker exec $CONTAINER_ID mkdir /sequence-graphs/$read_dir-sg-stats
for i in `seq 1 $num`;
do
    docker exec $CONTAINER_ID /bin/bash /test-sg.sh /sequence-graphs/$read_dir-index /sequence-graphs/$read_dir/$filename /sequence-graphs/$read_dir/$i.txt /sequence-graphs/$read_dir-sg-alignments/$i.alignment $mismatches /sequence-graphs/$read_dir-sg-stats/$i.stats
    before=$(date +%s%N)
    java -Xmx2048m -Xms2048m -jar $DIR/target/graph-genome.jar align -i=$read_dir-fuzzy-index -af=$read_dir/$i.txt -em=$mismatches -t=fuzzy > $read_dir-fuzzy-stats/$i.stats
    after=$(date +%s%N)
    echo "Tool time: $(($after - $before))" >> $read_dir-fuzzy-stats/$i.stats
    before=$(date +%s%N)
    java -jar $DIR/target/graph-genome.jar align -i=$read_dir-fuzzy-index -af=$read_dir/$i.txt -em=$mismatches -t=po_msa > $read_dir-po_msa-stats/$i.stats
    after=$(date +%s%N)
    echo "Tool time: $(($after - $before))" >> $read_dir-po_msa-stats/$i.stats
done 

#Copy alignments locally and clean up docker
docker cp $CONTAINER_ID:/sequence-graphs/$read_dir-sg-alignments .
docker cp $CONTAINER_ID:/sequence-graphs/$read_dir-sg-stats .
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir-index
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir-sg-alignments
docker exec $CONTAINER_ID rm -rf /sequence-graphs/$read_dir-sg-stats
docker exec $CONTAINER_ID rm /test-sg.sh

# Score alignments
echo "Type: vg" > $read_dir-sg-stats/build.stats
echo "Nanoseconds: $docker_build_time" >> $read_dir-sg-stats/build.stats
echo "Number of tests: $num" >> $read_dir-sg-stats/build.stats
echo "Number of allowed $mismatches" >> $read_dir-sg-stats/build.stats
echo "Mutation probability: $prob" >> $read_dir-sg-stats/build.stats
for i in `seq 1 $num`;
do
	/bin/bash score-sg-alignment.sh $read_dir-sg-alignments/$i.alignment $read_dir-sg-stats/$i.stats
done

# Print summaries
/bin/bash summarize-stats.sh $read_dir $num

# Clean up locally
rm -rf $read_dir
#rm -rf $read_dir-sg-alignments
#rm -rf $read_dir-sg-stats
rm -rf $read_dir-fuzzy-stats
#rm -rf $read_dir-po_msa-stats
rm $read_dir-fuzzy-index
rm $read_dir.reads
