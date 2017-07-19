#!/bin/bash 

# this script run sclite evaluation. 
# it assumes sctk is installed. If you decide not to install it in sctk
# subdirectory, please export the bin dir in sctk in PATH.

sctk_bin=/alt/projects/mgb-arabic/ArabicASRChallenge2016/evaluation/sctk/bin
export PATH=$sctk_bin:$PATH

EVALSTM=$1    # utf8 reference in stm format
EVALCTM=$2    # utf8 recognition result in ctm format
BUCKWALTER=${3-false}     # true if input ctm is in buckwalter

if [[ -s arabic_utf8.glm ]]; then
  GLM="-g ar20160531mgb.glm"
fi

if $BUCKWALTER; then
  ./toutf8.py $EVALCTM $EVALCTM.utf8
else
  cp $EVALCTM $EVALCTM.utf8
fi

hubscr.pl -d -V -f ctm -F stm -l arabic -h hub5 $GLM -r $EVALSTM $EVALCTM.utf8
