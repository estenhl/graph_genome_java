#!/bin/bash

# Equal sequences
java -jar ../target/graph-genome.jar index --index=equal.index --input-sequences=ACGTATTAC
java -jar ../target/graph-genome.jar align --index=equal.index --align-sequence=ACGTATTAC --png=equal-alignment
java -jar ../target/graph-genome.jar align --index=equal.index --align-sequence=ACGTATTAC --merge=true --png=equal-merge

# SNP without error-margin
java -jar ../target/graph-genome.jar index --index=snp-error.index --input-sequences=ACGTATTAC
java -jar ../target/graph-genome.jar align --index=snp-error.index --align-sequence=ACGGATTAC --png=snp-no-margin-alignment
java -jar ../target/graph-genome.jar align --index=snp-error.index --align-sequence=ACGGATTAC --merge=true --png=snp-no-margin-merge

# SNP with error-margin
java -jar ../target/graph-genome.jar index --index=snp.index --input-sequences=ACGTATTAC
java -jar ../target/graph-genome.jar align --index=snp.index --align-sequence=ACGGATTAC --png=snp-alignment --error-margin=1
java -jar ../target/graph-genome.jar align --index=snp.index --align-sequence=ACGGATTAC --merge=true --png=snp-merge --error-margin=1
java -jar ../target/graph-genome.jar align --index=snp.index --align-sequence=ACGCATTAC --merge=true --png=snp-merge-two --error-margin=1

# Insertion
java -jar ../target/graph-genome.jar index --index=insertion.index --input-sequences=ACGTATTAC
java -jar ../target/graph-genome.jar align --index=insertion.index --align-sequence=ACGTAATTAC --png=insertion-alignment --error-margin=1
java -jar ../target/graph-genome.jar align --index=insertion.index --align-sequence=ACGTAATTAC --merge=true --png=insertion-merge --error-margin=1

# Deletion
java -jar ../target/graph-genome.jar index --index=deletion.index --input-sequences=ACGTATTAC
java -jar ../target/graph-genome.jar align --index=deletion.index --align-sequence=ACGTTTAC --png=deletion-alignment --error-margin=1
java -jar ../target/graph-genome.jar align --index=deletion.index --align-sequence=ACGTTTAC --merge=true --png=deletion-merge --error-margin=1

rm *.dot
rm *.index
