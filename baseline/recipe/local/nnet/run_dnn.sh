#!/bin/bash 

# Copyright 2014 QCRI (author: Ahmed Ali)
# Apache 2.0

. ./path.sh
. ./cmd.sh ## You'll want to change cmd.sh to something that will work on your system.
           ## This relates to the queue.
nDecodeJobs=40
nDecodeJobs=120

#train DNN
mfcc_fmllr_dir=mfcc_fmllr
baseDir=exp/tri4
alignDir=exp/tri4_ali
dnnDir=exp/tri4_dnn_2048x5
align_dnnDir=exp/tri4_dnn_2048x5_ali
dnnLatDir=exp/tri4_dnn_2048x5_denlats
dnnMPEDir=exp/tri4_dnn_2048x5_smb

trainTr90=data/train_mer100_tr90
trainCV=data/train_mer100_cv10 

steps/nnet/make_fmllr_feats.sh --nj 10 --cmd "$cuda_cmd" \
  --transform-dir $baseDir/decode data/test_mer100_fmllr data/test_mer100 \
  $baseDir $mfcc_fmllr_dir/log_test_mer100 $mfcc_fmllr_dir || exit 1;

steps/nnet/make_fmllr_feats.sh --nj 10 --cmd "$cuda_cmd" \
  --transform-dir $alignDir data/train_mer100_fmllr data/train_mer100 \
  $baseDir $mfcc_fmllr_dir/log_train_mer100 $mfcc_fmllr_dir || exit 1;
                            
utils/subset_data_dir_tr_cv.sh  data/train_mer100_fmllr $trainTr90 $trainCV || exit 1;

(tail --pid=$$ -F $dnnDir/train_mer100_nnet.log 2>/dev/null)& 
$cuda_cmd $dnnDir/train_mer100_nnet.log \
steps/train_nnet.sh  --hid-dim 2048 --hid-layers 5 --learn-rate 0.008 \
  $trainTr90 $trainCV data/lang $alignDir $alignDir $dnnDir || exit 1;

steps/decode_nnet.sh --nj $nDecodeJobs --cmd "$decode_cmd" \
  --config conf/decode_dnn.config --nnet $dnnDir/final.nnet \
  --acwt 0.08 $baseDir/graph data/test_mer100_fmllr $dnnDir/decode

#
steps/nnet/align.sh --nj $nDecodeJobs --cmd "$train_cmd" data/train_mer100_fmllr data/lang \
  $dnnDir $align_dnnDir || exit 1;

steps/nnet/make_denlats.sh --nj $nDecodeJobs --cmd "$train_cmd" --config conf/decode_dnn.config --acwt 0.1 \
  data/train_mer100_fmllr data/lang $dnnDir $dnnLatDir || exit 1;

steps/nnet/train_mpe.sh --cmd "$cuda_cmd" --num-iters 6 --acwt 0.1 --do-smbr true \
  data/train_mer100_fmllr data/lang $dnnDir $align_dnnDir $dnnLatDir $dnnMPEDir || exit 1;

#decode
for n in 1 2 3 4 5 6; do
  steps/decode_nnet.sh --nj $nDecodeJobs --cmd "$train_cmd" --config conf/decode_dnn.config \
  --nnet $dnnMPEDir/$n.nnet --acwt 0.08 \
  $baseDir/graph data/test_mer100_fmllr $dnnMPEDir/decode_test_mer100_it$n || exit 1;
done

echo DNN success
# End of DNN

