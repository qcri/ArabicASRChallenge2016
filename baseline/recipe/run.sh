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

mer=100  

# TO DO: set the location of downloaded WAV files, XML, LM text and the Lexicon

# Location of downloaded WAV files
WAV_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/wav

# Location of downloaded XML files
XML_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/xml

# Location of downloaded LM text
LM_DIR=/data/sls/scratch/sameerk/mgb_arabic_data/lm

# Location of lexicon files
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

echo "Preparing training data"
local/mgb_data_prep.sh $WAV_DIR $XML_DIR $mer

echo "Preparing dictionary"
local/graphgeme_mgb_prep_dict.sh $LEX_DIR

echo "Training n-gram language model"
local/mgb_train_lms.sh $mer

echo "Preparing lang dir"
utils/prepare_lang.sh data/local/dict "<UNK>" data/local/lang data/lang

local/mgb_format_data.sh $mer

echo "Computing features"
for x in train_mer$mer test_mer$mer ; do
    mfccdir=data/mfcc_$x
    steps/make_mfcc.sh --nj $nj --cmd "$train_cmd" data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
    steps/compute_cmvn_stats.sh data/$x exp/mer$mer/make_mfcc/$x/log $mfccdir
done

# 2) Building GMM systems
# This is based on the standard Kaldi GMM receipe

#Taking 30k utterances
utils/subset_data_dir.sh data/train_mer$mer 30000 data/train_mer${mer}_30k || exit 1;

steps/train_mono.sh --nj 80 --cmd "$train_cmd" \
  data/train_mer${mer}_30k data/lang exp/mer$mer/mono 

steps/align_si.sh --nj $nj --cmd "$train_cmd" \
  data/train_mer$mer data/lang exp/mer$mer/mono exp/mer$mer/mono_ali 

#Train on full data
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

# Train tri4, which is LDA+MLLT+SAT 

steps/train_sat.sh  --cmd "$train_cmd" \
  5000 100000 data/train_mer$mer data/lang exp/mer$mer/tri3_ali exp/mer$mer/tri4

utils/mkgraph.sh data/lang_test exp/mer$mer/tri4 exp/mer$mer/tri4/graph

steps/decode_fmllr.sh --nj $nDecodeJobs --cmd "$decode_cmd" exp/mer$mer/tri4/graph data/test_mer$mer exp/mer$mer/tri4/decode

steps/align_fmllr.sh --nj $nj --cmd "$train_cmd" data/train_mer$mer data/lang exp/mer$mer/tri4 exp/mer$mer/tri4_ali

time=$(date +"%Y-%m-%d-%H-%M-%S")

for x in exp/mer$mer/*/decode*; do [ -d $x ] && grep WER $x/wer_* | utils/best_wer.sh; \
done | sort -n -r -k2 > RESULTS.$USER.$time # to make sure you keep the results timed and owned





