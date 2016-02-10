#!/bin/bash


# This runs on the full training set (with duplicates removed), with p-norm
# units, on top of fMLLR features, on GPU.

mer=$1
temp_dir=
dir=exp/mer$mer/nnet2_5d
stage=-2
parallel_opts="-l gpu=0"
. ./cmd.sh
. ./path.sh

. utils/parse_options.sh

#what to do here?

                  

( 
   if [ ! -f $dir/final.mdl ]; then
    steps/nnet2/train_pnorm_accel2.sh --parallel-opts "$parallel_opts" \
      --cmd "$decode_cmd" --stage $stage \
      --num-threads 1 --minibatch-size 512 \
      --mix-up 20000 --samples-per-iter 300000 \
      --num-epochs 15 \
      --initial-effective-lrate 0.005 --final-effective-lrate 0.0005 \
      --num-jobs-initial 1 --num-jobs-final 1 --num-hidden-layers 4 --splice-width 5 \
      --pnorm-input-dim 4000 --pnorm-output-dim 400 --p 2 \
      data/train_mer$mer data/lang exp/mer$mer/tri4_ali $dir || exit 1;
  fi
 
  steps/nnet2/decode.sh --cmd "$decode_cmd" --nj 60 \
    --config conf/decode.config \
    --transform-dir exp/mer$mer/tri4/decode \
    exp/mer$mer/tri4/graph data/test_mer$mer \
    $dir/decode || exit 1;
)
