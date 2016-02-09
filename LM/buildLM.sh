#!/bin/bash

# Build Tri-gram LM using SRI
# Usage: sh buildLM.sh <LMtext> <Lexicon> <outputLM>

if [ $# -ne 3 ]; then
  echo "Usage: sh buildLM.sh <LMtext> <Lexicon> <outputLM>"
  exit 1
fi

inputText=$1; shift
inputLexicon=$1; shift
outputLM=$1; shift

#we assume the lexicon and text files are in buckwalter format

grep -v ^# $inputLexicon | awk '{print $1}' > wordList$$
ngram-count -vocab wordList$$ -order 3 -prune 0.000000008 -text $inputText -lm $outputLM

exit 0


