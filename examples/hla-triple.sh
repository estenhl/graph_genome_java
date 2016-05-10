java -jar ../target/graph-genome.jar index --index=hla.index -if=../data/mhc_A3105/primary.fasta --png=hla
java -jar ../target/graph-genome.jar align --index=hla.index -af=../data/mhc_A3105/alt_loci_1.fasta --merge=true -em=3 -heur=true --png=hla
java -jar ../target/graph-genome.jar align --index=hla.index -af=../data/mhc_A3105/alt_loci_2.fasta --merge=true -em=4 -heur=true --png=hla