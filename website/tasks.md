Evaluation tasks
==

Particpants to the challenge can enter any of the four tasks:

1. Speech-to-text transcription of broadcast television
2. Dialect Detection Task

Tasks are described in more detail below. Each task has one or more primary evaluation conditions and possibly a number of contrastive conditions. To enter a task, participants must submit at least one system which fulfils the primary evaluation conditions. Note that signing the MGB challenge data license requires you to participate in at least one task.
Scoring tools for all tasks will be available shortly.

### Rules for all tasks

* Only audio data (train.full) and language model data (mgb.stripped.lm, mgb.normalised.lm) supplied by the organisers can be used for transcription and alignment tasks. All metadata supplied with training data can be used.
* Any lexicon can be used.


### Task 1: Transcription
This is a standard speech transcription task operating on a collection of whole TV shows drawn from diverse genres. Scoring will require ASR output with word-level timings. Segments with overlap will be ignored for scoring purposes (where overlap is defined to minimise the regions removed - at word level where possible). Speaker labels are not required in the hypothesis for scoring. Usual NIST-style mappings will be used to normalise reference/hypothesis.

For the evaluation data, show titles and genre labels will be supplied. Some titles will have appeared in the training data, some will be new. All genre labels will have been seen in the training data. The supplied title and genre information can be used as much as desired. Other metadata present in the development data will not be supplied for the evaluation data, but this does not preclude, for example, use the use of metadata for the development set to infer properties of shows with the same title in the evaluation data.

There may be shared speakers across training and evaluation data. It is possible for participants to automatically identify these themselves and make use of the information. However, each show in the evaluation set should be processed independently, ie. it will not be possible to link speakers across shows.

Systems for speech/silence segmentation must be trained only on the official training set. A baseline speech/silence segmentation and speaker clustering for the evaluation data will be supplied for participants who do not wish to build these systems. Any speaker clutering supplied will not link speakers between training/dev/eval sets.

Scoring - This tarball contains the transcription scoring script along with STM and GLM files as well as example scoring outputs. The download also includes a README with more information.

### Task 2: Dialect Detection Task
In this task, participants will be supplied with not less than 50 hours of Arabic broadcast data, obtained from the entire episode
that has been manually verified by human among the four major regional Dialectal Arabic groups; Egyptian, North African, Gulf, Levantine, as well as Modern Standard Arabic. This will summarize in total five classes. The dialectal data will be shared with the same transcription as mentioned
in the previous task.

This evaluation task concerns dialect identification of broadcast audio to one of the five dialects; Egyptian, Levantine, Gulf, North African, Modern standard Arabic. The core task is to detect the dialect of the speech using only audio signal. The test set will be segmented
on an average from three-to-five seconds per segment.

As in the transcription task, it will be possible to make use of the show title and genre labels, and any automatic speaker labelling across shows that participants choose to generate. Speaker change information will be supplied as in the original captions.

Scoring - This zip contains v0.1 of the dialect identifcation scoring script. The working of this script is described in detail in this document.
