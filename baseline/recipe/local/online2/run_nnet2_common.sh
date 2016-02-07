#!/bin/bash

# this script is called from scripts like run_nnet2.sh; it does
# the common stages of the build.

. cmd.sh
mfccdir=mfcc

stage=1

. cmd.sh
. ./path.sh
. ./utils/parse_options.sh


if [ $stage -le 1 ]; then
  for datadir in train test; do
    utils/copy_data_dir.sh data/$datadir data/${datadir}_hires
    steps/make_mfcc.sh --nj 40 --mfcc-config conf/mfcc_hires.conf \
      --cmd "$train_cmd" data/${datadir}_hires exp/make_hires/$datadir $mfccdir || exit 1;
    steps/compute_cmvn_stats.sh data/${datadir}_hires exp/make_hires/$datadir $mfccdir || exit 1;
  done
  # Make a subset of about 1/3 of the data.
  utils/subset_data_dir.sh data/train_hires 100000 \
     data/train_hires_100k || exit 1

  # make a corresponding subset of normal-dimensional-MFCC training data.
  utils/subset_data_dir.sh --utt-list <(awk '{print $1}' data/train_hires_100k/utt2spk) \
    data/train data/train_100k || exit 1
fi

if [ $stage -le 2 ]; then
  # We need to build a small system just because we need the LDA+MLLT transform
  # to train the diag-UBM on top of.  We align the si84 data for this purpose.

  steps/align_fmllr.sh --nj 40 --cmd "$train_cmd" \
    data/train_100k data/lang exp/tri3b exp/nnet2_online/tri3b_ali_100k
fi

if [ $stage -le 3 ]; then
  # Train a small system just for its LDA+MLLT transform.  We use --num-iters 13
  # because after we get the transform (12th iter is the last), any further
  # training is pointless.
  steps/train_lda_mllt.sh --cmd "$train_cmd" --num-iters 13 \
    --realign-iters "" \
    --splice-opts "--left-context=3 --right-context=3" \
    5000 10000 data/train_hires_100k data/lang \
     exp/nnet2_online/tri3b_ali_100k exp/nnet2_online/tri4a || exit 1
    
fi

if [ $stage -le 4 ]; then
  steps/online/nnet2/train_diag_ubm.sh --cmd "$train_cmd" --nj 30 \
     --num-frames 400000 data/train_hires_100k 256 exp/nnet2_online/tri4a exp/nnet2_online/diag_ubm
fi

if [ $stage -le 5 ]; then
  # even though $nj is just 10, each job uses multiple processes and threads.
  steps/online/nnet2/train_ivector_extractor.sh --cmd "$train_cmd" --nj 10 \
    data/train_hires exp/nnet2_online/diag_ubm exp/nnet2_online/extractor || exit 1;
fi

if [ $stage -le 6 ]; then
  # We extract iVectors on all the train_si284 data, which will be what we
  # train the system on.

  # having a larger number of speakers is helpful for generalization, and to
  # handle per-utterance decoding well (iVector starts at zero).
  steps/online/nnet2/copy_data_dir.sh --utts-per-spk-max 2 data/train_hires \
    data/train_hires_max2

  steps/online/nnet2/extract_ivectors_online.sh --cmd "$train_cmd" --nj 30 \
    data/train_hires_max2 exp/nnet2_online/extractor exp/nnet2_online/ivectors_train || exit 1;
fi

if [ $stage -le 7 ]; then
  rm exp/nnet2_online/.error 2>/dev/null
  steps/online/nnet2/extract_ivectors_online.sh --cmd "$train_cmd" --nj 8 \
      data/test_hires exp/nnet2_online/extractor exp/nnet2_online/ivectors_test || touch exp/nnet2_online/.error &
fi

exit 0;
