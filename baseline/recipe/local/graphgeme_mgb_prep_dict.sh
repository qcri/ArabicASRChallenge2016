#!/bin/bash

# run this from ../
dir=data/local/dict
mkdir -p $dir
lexdir=$1

#(2) Dictionary preparation:

# silence phones, one per line.
echo SIL > $dir/silence_phones.txt
echo SIL > $dir/optional_silence.txt

cp $lexdir/crpx.dct $dir
mv $dir/crpx.dct $dir/lexicon.txt
cat $dir/lexicon.txt | cut -d ' ' -f2- | tr -s ' ' '\n' |\
sort -u >  $dir/nonsilence_phones.txt || exit 1;

sed -i '1i<UNK> SIL' $dir/lexicon.txt
 
echo Dictionary preparation succeeded

