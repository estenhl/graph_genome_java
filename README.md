# The GraphGenome tool
This is a tool developed as a part of a master thesis in Informatics: Programming and networks at the Department for Informatics, University of Oslo. The tool builds graph-based reference genomes from datasets and provides functionality for aligning reads against the graph.
## Demo
There exists an extremely crude in-browser demo of the tool at **somewhere**
## Setup
Clone the repo: `git@github.com:estenpro/graph_genome_java.git`
Build the project with maven: `mvn clean install`
### Users without maven
For users without maven there exist prebuilt binaries in the `target/` folder, reachable through the run-scripts.
## Usage
The tool has two main functions, reachable through the `build_index.sh` and the `align_sequence.sh` scripts. Additionally a third script `build_and_align.sh` combines the two
### build_index.sh
Builds an index from a set of input sequences, fasta files or vcf files.
**Required parameters**
* `--index=<index-name>` Name of the file where the index should be stored
* At least one of the following:
  * `--input-sequences=<seq1>,<seq2>,...,<seqN>` A comma-separated list of sequences used for building the graph
  * `--input-fastas=<file1>,<file2>,...<fileN>` A comma-separated list of fasta files used for building the graph

**Optional parameters**
* `--scoring-system=<type>` The scoring system used for alignment. Possible values `edit-distance` and `lastz`. Defaults to edit distance
* `--error-margin=<margin>` The allowed error margin when aligning and merging sequences. Defaults to 0
* `--suffix-length=<length>` The suffix length to be used. Defaults to an optimal computed value based on the graph
* `--png=<filename>` Filename for the visual output files. Results in a `filename.dot` dot-file, and an additional `filename.png` file if dot is installed on the system. Outputs the finished merged graph.
* `--type=<type>` The type of algorithm to use for alignment. Possible values `fuzzy` and `po-msa`. Defaults to fuzzy, which is the "Fuzzy context-based search" algorithm developed in the thesis
* `--parallellization=<true/false>` Chooses whether to use parallellization in the alignment process. Defaults to false
* `--heuristical=<true/false>` Runs the algorithm heuristically
* `--vcf` Vcf files used to build the graph. (WARNING: Extremely experimental)

### align_sequence.sh
**Required parameters**
* `--index=<index-name>` Name of the file where the index to use is stored
* Either:
  * `align-sequence=<seq>` The sequence to be aligned
  * `align-fasta` A fasta file containing the sequence to be aligned

**Optional parameters**
* `--scoring-system=<type>` The scoring system used for alignment. Possible values `edit-distance` and `lastz`. Defaults to edit distance
* `--error-margin=<margin>` The allowed error margin when aligning and merging sequences. Defaults to 0
* `--suffix-length=<length>` The suffix length to be used. Defaults to an optimal computed value based on the graph
* `--merge=<true/false>` Chooses whether to merge the aligned sequence into the index or not. Defaults to false
* `--png=<filename>` Filename for the visual output files. Results in a `filename.dot` dot-file, and an additional `filename.png` file if dot is installed on the system. Outputs the alignment if `--merge=false`, the merged graph if `--merge=true`
* `--type=<type>` The type of algorithm to use for alignment. Possible values `fuzzy` and `po-msa`. Defaults to fuzzy, which is the "Fuzzy context-based search" algorithm developed in the thesis
* `--parallellization=<true/false>` Chooses whether to use parallellization in the alignment process. Defaults to false

### build_and_align.sh
Passes on parameters to the underlying functions, and is thus dependant on the same arguments. Does not need an `--index` parameter!
## Examples
A set of examples can be found in the `examples/` folder

**`simple.sh`**
Builds indexes and produces visualizations for the examples shown in the thesis

**`hla_a.sh`**
Builds an index and produces a visualization for a set of assemblies of the hla-a gene in the MHC region