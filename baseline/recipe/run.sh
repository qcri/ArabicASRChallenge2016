#!/bin/bash -u

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
# (train.full and dev.full) in this working directory, link to the
# usual steps/ and utils/ directories, and create your copies path.sh
# and cmd.sh in this directory.

# TO DO: specify the directories containing the binaries for
# xmlstarlet

XMLSTARLET=/data/sls/scratch/sameerk/xmlstarlet/usr/bin
#Sameer: Not using SRILM at the moment
#SRILM=alt/work/kaldi-trunk-2015-11-03/tools/srilm/bin/i686-m64
#IRSTLM=/data/sls/scratch/sameerk/kaldi/tools/irstlm/bin

# TO DO: you will need to choose the size of training set you want.
# Here we select according to an upper threshhold on Matching Error
# Rate from the lightly supervised alignment.  When using all the
# training shows, this will give you training data speech segments of
# approximate lengths listed below:

#Need to change this

# MER	duration (hrs)
#
# 10 	  240
# 20      400
# 30 	  530
# 40 	  640
# 50 	  740
# all    1210

mer=100  

# TO DO: set the location of downloaded WAV files, XML, LM text and the Combilex Lexicon

# Location of downloaded WAV files
WAV_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/wav

# Location of downloaded XML files
XML_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/xml

# Location of downloaded LM text
LM_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/lm

# Location of Combilex lexicon files
LEX_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/lex

nj=30  # split training into how many jobs?
nDecodeJobs=40

##########################################################
#
#  Recipe
#
##########################################################


#1) Data preparation

export XMLSTARLET SRILM IRSTLM

#Sameer: Using edinburgh's script with some modification for test data
echo "Preparing training data"
local/mgb_data_prep.sh $WAV_DIR $XML_DIR $mer

#Sameer: Added our dictionary preparation method with some modifications. Modification being: Not downloading the lexicon from QCRI website
#Note to self: Update the lexicon to the new one and run again
echo "Preparing dictionary"
local/graphgeme_mgb_prep_dict.sh $LEX_DIR

#Sameer: Using GALE Script instead of Edinburgh's, using Kaldi_lm toolbox instead of SRILM, use SRILM and see the difference
echo "Training n-gram language model"
local/mgb_train_lms.sh $mer

# Sameer: Changed <unk> to <UNK>
echo "Preparing lang dir"
utils/prepare_lang.sh data/local/dict "<UNK>" data/local/lang data/lang


local/mgb_format_data.sh $mer

#Added a loop to calculate features for both train and test data, just as in the GALE run.sh
echo "Computing features"
for x in train_mer$mer test_mer$mer ; do
    mfccdir=data/mfcc_$x
    steps/make_mfcc.sh --nj $nj --cmd "$train_cmd" data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
    steps/compute_cmvn_stats.sh data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
done

# 2) Building GMM systems
# This is based on the standard Kaldi GMM receipe

utils/subset_data_dir.sh data/train_mer$mer 30000 data/train_mer${mer}_30k || exit 1;


steps/train_mono.sh --nj 80 --cmd "$train_cmd" \
  data/train_mer${mer}_30k data/lang exp/mer$mer/mono 

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

# now train on full data
steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer$mer data/lang exp/mer$mer/tri2 exp/mer$mer/tri2_ali

# Train tri3, which is LDA+MLLT, on full
steps/train_lda_mllt.sh --cmd "$train_cmd" \
  4000 50000 data/train_mer$mer data/lang exp/mer$mer/tri1_ali exp/mer$mer/tri3

utils/mkgraph.sh data/lang_test exp/mer$mer/tri3 exp/mer$mer/tri3/graph
steps/decode.sh --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri3/graph data/test_mer$mer exp/mer$mer/tri3/decode 

steps/align_si.sh --nj $nj --cmd "$train_cmd" --use-graphs true data/train_mer$mer data/lang exp/mer$mer/tri3 exp/mer$mer/tri3_ali

# Train tri4, which is LDA+MLLT+SAT, on train_nodup data
#steps/align_fmllr.sh --nj $nj --cmd "$train_cmd" \
#  data/train_mer$mer data/lang exp/mer$mer/tri3 exp/mer$mer/tri3_ali_nodup 

steps/train_sat.sh  --cmd "$train_cmd" \
  5000 100000 data/train_mer$mer data/lang exp/mer$mer/tri3_ali exp/mer$mer/tri4

utils/mkgraph.sh data/lang_test exp/mer$mer/tri4 exp/mer$mer/tri4/graph

steps/decode_fmllr.sh --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri4/graph data/test_mer$mer exp/mer$mer/tri4/decode

steps/align_fmllr.sh --nj $nj --cmd "$train_cmd" data/train_mer$mer data/lang exp/mer$mer/tri4 exp/mer$mer/tri4_ali

#local/nnet/run_dnn.sh
#local/nnet/run_lstm.sh

#local/score_combine.sh data/test_mer$mer data/lang exp/mer$mer/tri4_dnn_2048x5_smb/decode_test_mer${mer}_it6 exp/lstm4f/decode_test/ exp/mer$mer/seqDNN_lstm_combine

#local/online/run_nnet2.sh

time=$(date +"%Y-%m-%d-%H-%M-%S")

for x in exp/mer$mer/*/decode* exp/mer$mer/nnet2_online*/nnet_a_gpu*/decode* exp/mer$mer/seqDNN_lstm_combine ; do [ -d $x ] && grep WER $x/wer_* | utils/best_wer.sh; \
done | sort -n -r -k2 > RESULTS.$USER.$time # to make sure you keep the results timed and owned

#This scripts needs to be modified
#local/split_wer.sh $ > RESULTS.details.$USER.$time









