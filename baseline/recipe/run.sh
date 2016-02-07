#!/bin/bash -u

# Copyright (C) 2016, Qatar Computing Research Institute, HBKU

set -e

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
# (train.full and test.full) in this working directory, link to the
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

# MER	duration (hrs)
#
# 10 	  
# 20      
# 30 	  
# 40 	  
# 50 	 
# all    

mer=80  

# TO DO: set the location of downloaded WAV files, XML, LM text and the LEXICON

# Location of downloaded WAV files
WAV_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/wav

# Location of downloaded XML files
XML_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/xml

# Using the training data transcript for building the language model
# Location of downloaded LM text
LM_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/lm

# Location of lexicon
LEX_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/lex

nj=100  # split training into how many jobs?
nDecodeJobs=60

##########################################################
#
#  Recipe
#
##########################################################


#1) Data preparation

export XMLSTARLET SRILM IRSTLM

echo "Preparing training data"
local/mgb_data_prep.sh $WAV_DIR $XML_DIR $mer

echo "Preparing dictionary"
local/graphgeme_mgb_prep_dict.sh $LEX_DIR

echo "Training n-gram language model"
local/mgb_train_lm.sh $LM_DIR

echo "Preparing lang dir"
utils/prepare_lang.sh data/local/dict "<UNK>" data/local/lang data/lang

local/mgb_format_data.sh

echo "Computing features"
for x in train_mer$mer test_mer$mer ; do
    mfccdir=data/mfcc_$x
    steps/make_mfcc.sh --nj $nj --cmd "$train_cmd" data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
    steps/compute_cmvn_stats.sh data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
done


# take a random 10k from this
utils/subset_data_dir.sh data/train_mer${mer} 10000 data/train_mer${mer}_10k 

steps/train_mono.sh --nj 80 --cmd "$train_cmd" \
  data/train_mer${mer}_10k data/lang exp/mer$mer/mono 

steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer$mer data/lang exp/mer$mer/mono exp/mer$mer/mono_ali 

steps/train_deltas.sh --cmd "$train_cmd" \
  2500 30000 data/train_mer${mer} data/lang exp/mer$mer/mono_ali exp/mer$mer/tri1 

utils/mkgraph.sh data/lang_test exp/mer$mer/tri1 exp/mer$mer/tri1/graph

steps/decode.sh  --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri1/graph data/test_mer$mer exp/mer$mer/tri1/decode

steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer${mer} data/lang exp/mer$mer/tri1 exp/mer$mer/tri1_ali 

steps/train_deltas.sh --cmd "$train_cmd" \
  3000 40000 data/train_mer${mer} data/lang exp/mer$mer/tri1_ali exp/mer$mer/tri2

utils/mkgraph.sh data/lang_test exp/mer$mer/tri2 exp/mer$mer/tri2/graph
steps/decode.sh --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri2/graph data/test_mer$mer exp/mer$mer/tri2/decode

steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer$mer data/lang exp/mer$mer/tri2 exp/mer$mer/tri2_ali

# Train tri3, which is LDA+MLLT, on full
steps/train_lda_mllt.sh --cmd "$train_cmd" \
  4000 50000 data/train_mer$mer data/lang exp/mer$mer/tri1_ali exp/mer$mer/tri3

utils/mkgraph.sh data/lang_test exp/mer$mer/tri3 exp/mer$mer/tri3/graph
steps/decode.sh --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri3/graph data/test_mer$mer exp/mer$mer/tri3/decode 

steps/align_si.sh --nj $nj --cmd "$train_cmd" --use-graphs true data/train_mer$mer data/lang exp/mer$mer/tri3 exp/mer$mer/tri3_ali


#training with Speaker Adaptation
steps/train_sat.sh  --cmd "$train_cmd" \
  5000 100000 data/train_mer$mer data/lang exp/mer$mer/tri3_ali exp/mer$mer/tri4

utils/mkgraph.sh data/lang_test exp/mer$mer/tri4 exp/mer$mer/tri4/graph
steps/decode_fmllr.sh --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri4/graph data/test_mer$mer exp/mer$mer/tri4/decode

steps/align_fmllr.sh --nj $nj --cmd "$train_cmd" data/train_mer$mer data/lang exp/mer$mer/tri4 exp/mer$mer/tri4_ali

steps/make_denlats.sh --cmd "$train_cmd" --nj $nj --transform-dir exp/mer$mer/tri4_ali \
--config conf/decode.config \
data/train_mer$mer data/lang exp/mer$mer/tri4 exp/mer$mer/tri4_denlats || exit 1;

steps/decode.sh --cmd "$decode_cmd" --nj $nDecodeJobs --config conf/decode.config \
 --transform-dir exp/mer$mer/tri4/decode \
exp/mer$mer/tri4/graph data/test_mer$mer exp/mer$mer/tri4_mmi_b0.1/decode || exit 1 ;

# Do MPE.                                                                                                                                                   
steps/train_mpe.sh  --cmd "$train_cmd" data/train_mer$mer data/lang exp/mer$mer/tri4_ali exp/mer$mer/tri4_denlats exp/mer$mer/tri4_mpe || exit 1;

steps/decode.sh --cmd "$decode_cmd" --nj $nDecodeJobs --config conf/decode.config \
  --transform-dir exp/mer$mer/tri4/decode \
  exp/mer$mer/tri4/graph data/test_mer$mer exp/mer$mer/tri4_mpe/decode || exit 1 ;


# SGMM system [sgmm5a]                                                                                                                                       
steps/train_ubm.sh --cmd "$train_cmd" \
  900 data/train_mer$mer data/lang exp/mer$mer/tri4_ali exp/mer$mer/ubm5a || exit 1;

steps/train_sgmm2.sh --cmd "$train_cmd" \
  14000 35000 data/train_mer$mer data/lang exp/mer$mer/tri4_ali \
  exp/mer$mer/ubm4/final.ubm exp/mer$mer/sgmm2_4 || exit 1;

utils/mkgraph.sh data/lang_test exp/mer$mer/sgmm2_4 exp/mer$mer/sgmm2_4/graph || exit 1;
steps/decode_sgmm2.sh --nj $nDecodeJobs --cmd "$decode_cmd" --config conf/decode.config \
  --transform-dir exp/mer$mer/tri4/decode \
  exp/mer$mer/sgmm2_4/graph data/test_mer$mer exp/mer$mer/sgmm2_4/decode || exit 1;


time=$(date +"%Y-%m-%d-%H-%M-%S")

for x in exp/mer$mer/*/decode*; do [ -d $x ] && grep Sum $x/score_*/*.sys | utils/best_wer.sh; done | sort -n -r -k2 > RESULTS.$USER.$time

#for x in exp/mer$mer/*/decode* exp/mer$mer/nnet2_online*/nnet_a_gpu*/decode* exp/mer$mer/seqDNN_lstm_combine ; do [ -d $x ] && grep Sum $x/score_*/*.sys | utils/best_wer.sh; \
#done | sort -n -r -k2 > RESULTS.$USER.$time # to make sure you keep the results timed and owned

#local/split_wer.sh $ > RESULTS.details.$USER.$time
