#!/bin/bash

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

cat train.full | while read basename; do     
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    $XMLSTARLET/xmlstarlet sel -t -m '//segments[@annotation_id="transcript_align"]' -m "segment" -n -v  "concat(@who,' ',@starttime,' ',@endtime,' ',@WMER,' ')" -m "element" -v "concat(text(),' ')" $xmldir/$basename.xml | local/add_to_datadir.py $basename $dir $mer
    echo $basename $wavdir/$basename.wav >> $dir/wav.scp
done

cat dev.full | while read basename; do
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    $XMLSTARLET/xmlstarlet sel -t -m '//segments[@annotation_id="transcript_align"]' -m "segment" -n -v  "concat(@who,' ',@starttime,' ',@endtime,' ',@WMER,' ')" -m "element" -v "concat(text(),' ')" $xmldir/$basename.xml | local/add_to_datadir.py $basename $dirtest $mer
    echo $basename $wavdir/$basename.wav >> $dirtest/wav.scp
done

sort -k 2 $dir/utt2spk | utils/utt2spk_to_spk2utt.pl > $dir/spk2utt

sort -k 2 $dirtest/utt2spk | utils/utt2spk_to_spk2utt.pl > $dirtest/spk2utt

utils/fix_data_dir.sh $dir
utils/validate_data_dir.sh --no-feats $dir


utils/fix_data_dir.sh $dirtest
utils/validate_data_dir.sh --no-feats $dirtest

echo "Training and Test data preparation succeeded"
