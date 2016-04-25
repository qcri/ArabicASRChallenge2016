#!/bin/bash -aux

# Copyright (C) 2016, Qatar Computing Research Institute, HBKU

. ./cmd.sh
. ./path.sh

##########################################################
#
#  Initial notes
#
##########################################################

# To this recipe you'll need
# 1) An installation of Kaldi
# 2) xmlstarlet http://xmlstar.sourceforge.net/


# This script assumes that you are already familiar with Kaldi recipes.


##########################################################
#
#  Actions required from users
#
##########################################################

# TO DO: You will need to place the lists of training and dev data
# (train and dev) in this working directory, link to the
# usual steps/ and utils/ directories, and create your copies path.sh
# and cmd.sh in this directory.

# TO DO: specify the directories containing the binaries for
# xmlstarlet

XMLSTARLET=/data/sls/scratch/sameerk/xmlstarlet/usr/bin


# TO DO: you will need to choose the size of training set you want.
# Here we select according to an upper threshhold on Matching Error
# Rate from the lightly supervised alignment.  When using all the
# training shows, this will give you training data speech segments of
# approximate lengths listed below:

 
#FILTER OUT SEGMENTS BASED ON MER (Match Error Rate)

mer=80  

# TO DO: set the location of downloaded WAV files, XML, LM text and the LEXICON

# Location of downloaded WAV files
WAV_DIR=/data/scratch/mgb/data/audio

# Location of downloaded XML files
XML_DIR=/data/scratch/mgb/data/bw_xml.2/

# Using the training data transcript for building the language model
# Location of downloaded LM text
LM_DIR=/data/scratch/mgb/data/mgb.arabic.lm.text.14.02.2016

# Location of lexicon
LEX_DIR=ArabicASRChallenge2016/lexicon/

nj=100  # split training into how many jobs?
nDecodeJobs=80

##########################################################
#
#  Recipe
#
##########################################################


#1) Data preparation

export XMLSTARLET SRILM IRSTLM

#DATA PREPARATION
echo "Preparing training data"
bash -x local/mgb_data_prep.sh $WAV_DIR $XML_DIR $mer

#LEXICON PREPARATION: The lexicon is also provided
echo "Preparing dictionary"
local/graphgeme_mgb_prep_dict.sh $LEX_DIR

#LM TRAINING: Using the training set transcript text for language modelling
echo "Training n-gram language model"
local/mgb_train_lms.sh $mer

#L Compilation
echo "Preparing lang dir"
utils/prepare_lang.sh data/local/dict "<UNK>" data/local/lang data/lang

#G compilation
local/mgb_format_data.sh

#Calculating mfcc features

echo "Computing features"
for x in train_mer$mer dev_non_overlap dev_overlap ; do
    mfccdir=data/mfcc_$x
    steps/make_mfcc.sh --nj $nj --cmd "$train_cmd" data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
    steps/compute_cmvn_stats.sh data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
done


#Taking 10k segments for faster training
utils/subset_data_dir.sh data/train_mer${mer} 10000 data/train_mer${mer}_10k 

#Monophone training
steps/train_mono.sh --nj 80 --cmd "$train_cmd" \
  data/train_mer${mer}_10k data/lang exp/mer$mer/mono 

#Monophone alignment
steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer$mer data/lang exp/mer$mer/mono exp/mer$mer/mono_ali 

#tri1 [First triphone pass]
steps/train_deltas.sh --cmd "$train_cmd" \
  2500 30000 data/train_mer${mer} data/lang exp/mer$mer/mono_ali exp/mer$mer/tri1 

#tri1 decoding
utils/mkgraph.sh data/lang_test exp/mer$mer/tri1 exp/mer$mer/tri1/graph

for dev in overlap non_overlap; do
  steps/decode.sh --nj $nDecodeJobs --cmd "$decode_cmd" --config conf/decode.config \
    exp/mer$mer/tri1/graph data/dev_$dev exp/mer$mer/tri1/decode_$dev &
done

#tri1 alignment
steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer${mer} data/lang exp/mer$mer/tri1 exp/mer$mer/tri1_ali 

#tri2 [a larger model than tri1]
steps/train_deltas.sh --cmd "$train_cmd" \
  3000 40000 data/train_mer${mer} data/lang exp/mer$mer/tri1_ali exp/mer$mer/tri2

#tri2 decoding
utils/mkgraph.sh data/lang_test exp/mer$mer/tri2 exp/mer$mer/tri2/graph

for dev in overlap non_overlap; do
 steps/decode.sh --nj $nDecodeJobs --cmd "$decode_cmd" --config conf/decode.config \
 exp/mer$mer/tri2/graph data/dev_$dev exp/mer$mer/tri2/decode_$dev
done

#tri2 alignment
steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer$mer data/lang exp/mer$mer/tri2 exp/mer$mer/tri2_ali

# tri3 training [LDA+MLLT]
steps/train_lda_mllt.sh --cmd "$train_cmd" \
  4000 50000 data/train_mer$mer data/lang exp/mer$mer/tri1_ali exp/mer$mer/tri3

#tri3 decoding
utils/mkgraph.sh data/lang_test exp/mer$mer/tri3 exp/mer$mer/tri3/graph

for dev in overlap non_overlap; do
 steps/decode.sh --nj $nDecodeJobs --cmd "$decode_cmd" --config conf/decode.config \
 exp/mer$mer/tri3/graph data/dev_$dev exp/mer$mer/tri3/decode_$dev
done


#tri3 alignment
steps/align_si.sh --nj $nj --cmd "$train_cmd" --use-graphs true data/train_mer$mer data/lang exp/mer$mer/tri3 exp/mer$mer/tri3_ali


#now we start building model with speaker adaptation SAT [fmllr]
steps/train_sat.sh  --cmd "$train_cmd" \
  5000 100000 data/train_mer$mer data/lang exp/mer$mer/tri3_ali exp/mer$mer/tri4

#sat decoding
utils/mkgraph.sh data/lang_test exp/mer$mer/tri4 exp/mer$mer/tri4/graph

for dev in overlap non_overlapdo
  steps/decode_fmllr.sh --nj $nDecodeJobs --cmd "$decode_cmd" --config conf/decode.config \
  exp/mer$mer/tri4/graph data/dev_$dev exp/mer$mer/tri4/decode_$dev
done

#sat alignment
steps/align_fmllr.sh --nj $nj --cmd "$train_cmd" data/train_mer$mer data/lang exp/mer$mer/tri4 exp/mer$mer/tri4_ali

# nnet1 dnn                                                                                                                                
local/nnet/run_dnn.sh $mer


time=$(date +"%Y-%m-%d-%H-%M-%S")
results=baseline.$time
#SCORING IS DONE USING SCLITE
for x in exp/*/*/decode*; do [ -d $x ] && grep Sum $x/score_*/*.sys | utils/best_wer.sh; done | sort -n -k2 > tmp$$

echo "non_overlap_speech_WER:" > $results
grep decode_non_overlap tmp$$ >> $results
echo "" >> $results
echo "" >> $results
echo "overlap_speech_WER:" >> $results
grep decode_overlap tmp$$ >> $results
echo "" >> $results
rm -fr tmp$$

