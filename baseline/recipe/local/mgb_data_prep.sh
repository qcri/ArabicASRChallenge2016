#!/bin/bash

# Copyright (C) 2016, Qatar Computing Research Institute, HBKU


if [ $# -ne 3 ]; then
  echo "Usage: $0 <wav-dir> <xml-dir> <mer-sel>"
  exit 1;
fi

wavDir=$1
xmldir=$2
mer=$3

#wavDir=$WAV_DIR;xmldir=$XML_DIR;mer=80
trainDir=data/train_mer$mer
devDir=data/dev

mkdir -p $trainDir
mkdir -p $devDir

rm -f $trainDir/{wav.scp,feats.scp,utt2spk,spk2utt,segments,text} $devDir/{wav.scp,feats.scp,utt2spk,spk2utt,segments,text}


for file in dev train; do
  if [ ! -f $file ]; then
    echo "$0: no such file $file - copy $file from GitHub repository ArabicASRChallenge2016/download/"
    exit 1;
  fi
done 
#Creating the train and dev program lists
cut -d '/' train -f2 | head -500 >> train.short
cut -d '/' dev -f2 >> dev$$

cat train.short | while read basename; do     
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    $XMLSTARLET/xmlstarlet sel -t -m '//segments[@annotation_id="transcript_align"]' -m "segment" -n -v  "concat(@who,' ',@starttime,' ',@endtime,' ',@WMER,' ')" -m "element" -v "concat(text(),' ')" $xmldir/$basename.xml | local/add_to_datadir.py $basename $trainDir $mer
    echo $basename $wavDir/$basename.wav >> $trainDir/wav.scp
done

cat dev$$ | while read basename; do
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    $XMLSTARLET/xmlstarlet sel -t -m '//segments[@annotation_id="transcript_manual"]' -m "segment" -n -v  "concat(@who,' ',@starttime,' ',@endtime,' ',@WMER,' ')" -m "element" -v "concat(text(),' ')" $xmldir/$basename.xml | local/add_to_datadir.py $basename $devDir $mer
    echo $basename $wavDir/$basename.wav >> $devDir/wav.scp
done

#Creating a file reco2file_channel which is used by convert_ctm.pl in local/score.sh script
rm -rf $devDir/reco2file_channel
cat $devDir/wav.scp >> $devDir/reco2file_channel
for f in $devDir/reco2file_channel; do
    sed -i "s/$/ 0/" $f;
done

#stm reference file for scoring
cat dev$$ | while read basename; do
    [ ! -e $xmldir/$basename.xml ] && echo "Missing $xmldir/$basename.xml" && exit 1
    local/xml2stm.py $xmldir/$basename.xml >> $devDir/test.stm
done


for list in overlap non_overlap; do
 cp -r $devDir ${devDir}_$list
 ( cd ${devDir}_$list
   for x in segments text utt2spk; do
     join ../../local/${list}_speech.lst $x > tmp
	 mv tmp $x
   done
 )
done

for dir in data/*; do
  sort -k 2 $dir/utt2spk | utils/utt2spk_to_spk2utt.pl > $dir/spk2utt
  utils/fix_data_dir.sh $dir
utils/validate_data_dir.sh --no-feats $dir
done

for dir in ${devDir} ${devDir}_overlap ${devDir}_non_overlap; do
  awk '{print $1 " " $1}' $dir/segments > $dir/spk2utt
  cp $dir/spk2utt $dir/utt2spk
  perl -e '
 ($f1,$f2)= split /\s+/, $ARGV[0];
 open(FNAME, "$f1");
 while (<FNAME>){chomp $_;@arr=split /\s+/,$_;shift @arr;$scal = "@arr";$hashExist{$scal}=1;}close (FNAME);
 open(FTR, "$f2"); while (<FTR>){$line=$_;s/ 0 UNKNOWN / /;@arr=split /\s+/,$_;if (defined $hashExist{"$arr[0] $arr[1] $arr[2]"}) {print "$line";}}close (FTR);
 ' "$dir/segments $dir/test.stm" > $dir/test.stm_
 mv $dir/test.stm_ $dir/test.stm
done

rm -fr dev$$

echo "Training and Test data preparation succeeded"
