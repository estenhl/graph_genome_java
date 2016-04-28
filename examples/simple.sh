# First index
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --png=build

# Equal sequence
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=equal.index
java -jar ../target/graph-genome.jar align --index=equal.index --align-sequence=ACGTATTAC --png=equal-align
java -jar ../target/graph-genome.jar align --index=equal.index --align-sequence=ACGTATTAC --merge=true --png=equal-merge

# SNP without error margin
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=snp-no-errors.index
java -jar ../target/graph-genome.jar align --index=snp-no-errors.index --align-sequence=ACGGATTAC --png=snp-no-errors-align
java -jar ../target/graph-genome.jar align --index=snp-no-errors.index --align-sequence=ACGGATTAC --merge=true --png=snp-no-errors-merge

# SNP with error margin
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=snp.index
java -jar ../target/graph-genome.jar align --index=snp.index --align-sequence=ACGGATTAC --error-margin=1 --png=single-snp-align
java -jar ../target/graph-genome.jar align --index=snp.index --align-sequence=ACGGATTAC --error-margin=1 --merge=true --png=single-snp-merge

# Deletion
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=deletion.index
java -jar ../target/graph-genome.jar align --index=deletion.index --align-sequence=ACGTTTAC --error-margin=1 --png=deletion-align
java -jar ../target/graph-genome.jar align --index=deletion.index --align-sequence=ACGTTTAC --error-margin=1 --png=deletion-merge

# Insertion
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=insertion.index
java -jar ../target/graph-genome.jar align --index=insertion.index --align-sequence=ACGTAATTAC --error-margin=1 --png=insertion-align
java -jar ../target/graph-genome.jar align --index=insertion.index --align-sequence=ACGTAATTAC -merge=true --error-margin=1 --png=insertion-merge

# Structural 1
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=structural1.index
java -jar ../target/graph-genome.jar align --index=structural1.index --align-sequence=ACGTGGTTAC --error-margin=2 --merge=true --png=structural1-merge

# Structural 2
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC --index=structural2.index
java -jar ../target/graph-genome.jar align --index=structural2.index --align-sequence=ACGTACCTT --error-margin=4 --merge=true --png=structural2-merge

# Complex
java -jar ../target/graph-genome.jar index --input-sequences=ACGTATTAC,ACGGATTAC,ACGTTTAC,ACGTAATTAC --index=complex.index --error-margin=1 --png=complex
java -jar ../target/graph-genome.jar align --index=complex.index --align-sequence=ACGTGGTTAC --error-margin=2 --merge=true
java -jar ../target/graph-genome.jar align --index=complex.index --align-sequence=ACGTACCTT --error-margin=4 --merge=true --png=complex-merge
java -jar ../target/graph-genome.jar align --index=complex.index --align-sequence=ACGTGGTTAC --error-margin=0 --png=complex-align

