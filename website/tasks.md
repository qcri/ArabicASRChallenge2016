Evaluation tasks
==

Participants are welcomed to enter any of these following tasks:

1. Speech-to-text transcription of broadcast television
2. Dialect detection Task

Tasks are described in more details below. The data for these two tasks will be available in [download page](http://). Support programs such as conversion and scoring tools will be provided in download page as well.

### Rules for all tasks

* Only audio data (arabic.train.full) and language model data (arabic.stripped.lm, arabic.normalised.lm) supplied in [download page](http://) can be used for transcription and dialect detection tasks. Metadata specified in below descriptions can be used in addition to the training data. Everything else can only be used in contrast evaluation. 
* Any lexicon can be used.


### Task 1: Transcription
This is a standard speech transcription task operating on a collection of whole TV shows drawn from diverse genres. Scoring will require ASR output with word-level recognition result. Segments of music and/or overlapped speech will be excluded for scoring purposes. Speaker labels are not required in the hypothesis for scoring. Usual NIST-style mappings will be used to normalise reference/hypothesis.

For the evaluation data, program titles will be supplied. There are 19 programmes, and episodes of these program in training data will not appear in the evaluation data. Metadata such as program title, speakers, broadcast date and time can be used to enhance the training and recognition process. Other meta data available in original XML transcript provided by Al-Jazeera cannot be used.

There may be shared speakers across training and evaluation data. It is possible for participants to automatically identify these themselves and make use of the information. However, each show in the evaluation set should be processed independently, ie. it will not be possible to link speakers across shows. Speaker information may be a speaker name or a generic term such as "speaker from Syria" for example. Participants shall not make assumption that the speakers with the same name are guaranteed to be the exact same person. A list will be provided to be as accurate as possible for terms aren't real names.

Systems for speech/silence segmentation must be trained only on the official training set. A baseline speech/silence segmentation and speaker information for the evaluation data will be supplied for participants who do not wish to build these systems. Any speaker information supplied will not link speakers between training/dev/eval sets.

Scoring - This tarball contains the transcription scoring script along with STM and GLM files as well as example scoring outputs. The download also includes a README with more information.

### Task 2: Dialect Detection Task
In this task, participants will be supplied with 50 hours of Arabic broadcast data, obtained from the entire episode
that has been manually verified by human among the four major regional dialectal arabic groups: Egyptian, North African, Gulf, Levantine, as well as Modern Standard Arabic. The dialectal data will be available in segments with variable durations. The dialectal data will share the transcripts available through the transcription task. Only transcripts can be used. Metadata must not be used for training.

This evaluation task concerns dialect identification of broadcast audio to one of the five dialects; Egyptian, Levantine, Gulf, North African, Modern standard Arabic. The evaluation data will be provides as segments of three to five seconds. Participant will identify the dialect of each segment using of the audio signal from each segment.

Scoring - This zip contains v0.1 of the dialect identifcation scoring script. The working of this script is described in detail in this document.
