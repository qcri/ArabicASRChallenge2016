#!/bin/bash 

export PATH=sctk/bin:$PATH

EVALSTM=$1    # utf8 reference in stm format
EVALCTM=$2    # utf8 recognition result in ctm format

if [[ -s arabic_utf8.glm ]]; then
  GLM="-g arabic_utf8.glm"
fi

../baseline/recipe/local/toutf8.py $EVALCTM $EVALCTM.utf8

hubscr.pl -d -V -f ctm -F stm -l arabic -h hub5 $GLM -r $EVALSTM $EVALCTM.utf8
