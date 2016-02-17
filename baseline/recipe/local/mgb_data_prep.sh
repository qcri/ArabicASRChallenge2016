#!/bin/bash

# Copyright (C) 2016, Qatar Computing Research Institute, HBKU

set -e

if [ $# -ne 3 ]; then
  echo "Usage: $0 <wav-dir> <xml-dir> <mer-sel>"
  exit 1;
fi

wavdir=$1
xmldir=$2
mer=$3

dir=data/train_mer$mer

dirtest=data/test_mer$mer
mkdir -p $dir
mkdir -p $dirtest

rm -f $dir/{wav.scp,feats.scp,utt2spk,spk2utt,segments,text} $dirtest/{wav.scp,feats.scp,utt2spk,spk2utt,segments,text}

rm -f train.full dev.full train.short

#Creating the train and dev program lists
cut -d '/' train -f2 | head -500 >> train.short
cut -d '/' dev -f2 >> dev.full

cat train.short | while read basename; do     
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    $XMLSTARLET/xmlstarlet sel -t -m '//segments[@annotation_id="transcript_align"]' -m "segment" -n -v  "concat(@who,' ',@starttime,' ',@endtime,' ',@WMER,' ')" -m "element" -v "concat(text(),' ')" $xmldir/$basename.xml | local/add_to_datadir.py $basename $dir $mer
    echo $basename $wavdir/$basename.wav >> $dir/wav.scp
done

cat dev.full | while read basename; do
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    $XMLSTARLET/xmlstarlet sel -t -m '//segments[@annotation_id="transcript_manual"]' -m "segment" -n -v  "concat(@who,' ',@starttime,' ',@endtime,' ',@WMER,' ')" -m "element" -v "concat(text(),' ')" $xmldir/$basename.xml | local/add_to_datadir.py $basename $dirtest $mer
    echo $basename $wavdir/$basename.wav >> $dirtest/wav.scp
done

#Creating a file reco2file_channel which is used by convert_ctm.pl in local/score.sh script
rm -rf reco2file_channel
cat $dirtest/wav.scp >> $dirtest/reco2file_channel
for f in $dirtest/reco2file_channel; do
    sed -i "s/$/ 0/" $f;
done

#stm reference file for scoring
cat dev.full | while read basename; do
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    local/xml2stm.py $xmldir/$basename.xml >> $dirtest/test.stm
done

sort -k 2 $dir/utt2spk | utils/utt2spk_to_spk2utt.pl > $dir/spk2utt

sort -k 2 $dirtest/utt2spk | utils/utt2spk_to_spk2utt.pl > $dirtest/spk2utt

utils/fix_data_dir.sh $dir
utils/validate_data_dir.sh --no-feats $dir

utils/fix_data_dir.sh $dirtest
utils/validate_data_dir.sh --no-feats $dirtest

echo "Training and Test data preparation succeeded"
