#!/bin/bash

DIR=~/skole/master/graph-genome
CONTAINER_ID=10bf4fbece7b

filename=$1
IFS='.' read -r -a array <<< "$1"
read_dir=${array[0]}
mkdir $read_dir
java -jar $DIR/target/read-generator.jar reads file=$filename out=$read_dir.reads num=$2
/bin/bash $DIR/scripts/reads-to-fastas.sh $read_dir.reads $read_dir
cp $filename $read_dir/$filename
docker cp $read_dir $CONTAINER_ID:/sequence-graphs/$read_dir
docker start $CONTAINER_ID
docker exec $CONTAINER_ID /sequence-graphs/createIndex/createIndex /sequence-graphs/$read_dir-index /sequence-graphs/$read_dir/$filename
rm -rf $read_dir
rm $read_dir.reads