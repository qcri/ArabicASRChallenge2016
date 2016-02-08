#!/bin/bash
# Copyright 2012  Johns Hopkins University (Author: Daniel Povey)
# Apache 2.0

[ -f ./path.sh ] && . ./path.sh

# begin configuration section.
cmd=run.pl
stage=0
decode_mbr=true
reverse=false
word_ins_penalty=0.0
min_lmwt=9
max_lmwt=20
#end configuration section.

[ -f ./path.sh ] && . ./path.sh
. parse_options.sh || exit 1;

if [ $# -ne 3 ]; then
  echo "Usage: local/score.sh [--cmd (run.pl|queue.pl...)] <data-dir> <lang-dir|graph-dir> <decode-dir>"
  echo " Options:"
  echo "    --cmd (run.pl|queue.pl...)      # specify how to run the sub-processes."
  echo "    --stage (0|1|2)                 # start scoring script from part-way through."
  echo "    --decode_mbr (true/false)       # maximum bayes risk decoding (confusion network)."
  echo "    --min_lmwt <int>                # minumum LM-weight for lattice rescoring "
  echo "    --max_lmwt <int>                # maximum LM-weight for lattice rescoring "
  echo "    --reverse (true/false)          # score with time reversed features "
  exit 1;
fi

data=$1
lang_or_graph=$2
dir=$3
srcdir=`dirname $dir`;
symtab=$lang_or_graph/words.txt

for f in $symtab $dir/lat.1.gz $data/text; do
  [ ! -f $f ] && echo "score.sh: no such file $f" && exit 1;
done

name=`basename $data`

mkdir -p $dir/scoring/log

#cat $data/text | sed 's:<NOISE>::g' | sed 's:<SPOKEN_NOISE>::g' > $dir/scoring/test_filt.txt
if [ $stage -le 0 ]; then
  $cmd LMWT=$min_lmwt:$max_lmwt $dir/scoring/log/best_path.LMWT.log \
    mkdir -p $dir/score_LMWT/ '&&' \
    lattice-1best --lm-scale=LMWT "ark:gunzip -c $dir/lat.*.gz|" ark:- \| \
    lattice-align-words $lang_or_graph/phones/word_boundary.int $srcdir/final.mdl ark:- ark:- \| \
    nbest-to-ctm ark:- - \| \
    utils/int2sym.pl -f 5 $symtab  \| \
    utils/convert_ctm.pl $data/segments $data/wav.scp \
    '>' $dir/score_LMWT/${name}.ctm || exit 1;
fi    

for x in $dir/score_*/$name.ctm; do
 local/cleanctm.sh $x $dir $data
done

# Remove some stuff we don't want to score, from the ctm.                                                                                                   

if [ $stage -le 1 ]; then 
  for x in $dir/score_*/$name.ctm.updated; do
    cp $x $dir/tmpf;
    cat $dir/tmpf | grep -v -E '\[NOISE|LAUGHTER|VOCALIZED-NOISE\]' | \
      grep -v -E '<UNK>|%HESITATION' > $x;
  done
fi  


if [ $stage -le 2 ]; then
  $cmd LMWT=$min_lmwt:$max_lmwt $dir/scoring/log/score.LMWT.log \
    cp $data/test.stm $dir/score_LMWT/ '&&' \
    sclite -O $dir/score_LMWT -o all -h $dir/score_LMWT/${name}.ctm.updated ctm -r $data/test.stm stm || exit 1;  
fi  
#$cmd LMWT=$min_lmwt:$max_lmwt $dir/scoring/log/best_path.LMWT.log \
 #  lattice-align-words $lang_or_graph/phones/word_boundary.int $srcdir/final.mdl "ark:gunzip -c $dir/lat.*.gz|" ark:- \| \
 #  lattice-to-ctm-conf --lm-scale=LMWT ark:- \
 #    ark:- ark,t:$dir/scoring/LMWT.ctm || exit 1; 

if $reverse; then
  for lmwt in `seq $min_lmwt $max_lmwt`; do
    mv $dir/scoring/$lmwt.tra $dir/scoring/$lmwt.tra.orig
    awk '{ printf("%s ",$1); for(i=NF; i>1; i--){ printf("%s ",$i); } printf("\n"); }' \
       <$dir/scoring/$lmwt.tra.orig >$dir/scoring/$lmwt.tra
  done
fi




#find $dir/scoring -name '*.ctm.updated' | while read file; do
#    cat $file | int2sym.pl -f 5 $symtab > $file.new 
#done

#rm $dir/scoring/*.ctm.updated

#find $dir/scoring -name '*.updated.new' | while read file; do
#    filename=$(basename "$file")
#    filename="${filename%.*}"
#    mv $file $dir/scoring/$filename
#done

# Note: the double level of quoting for the sed command
#$cmd LMWT=$min_lmwt:$max_lmwt $dir/scoring/log/score.LMWT.log \
#   mkdir -p $dir/scoring/results/LMWT \| \
#   sclite -O $dir/scoring/results/LMWT -o all -h $dir/scoring/LMWT.ctm.updated ctm -r $data/test.stm stm 
   #cat $dir/scoring/LMWT.ctm \| \
   # utils/int2sym.pl -f 5 $symtab \| sed 's:\<UNK\>::g' \| \
    #compute-wer --text --mode=present \
    # ark:$dir/scoring/test_filt.txt  ark,p:- ">&" $dir/wer_LMWT || exit 1;

exit 0;
