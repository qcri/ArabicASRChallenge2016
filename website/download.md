Download Instructions
==

*NOTE: Download is not available yet, please check back again on release date*


After your registeration, you will receive download instructions by email. Any use of this data is bound by the MGB-Challenge Arabic Track data license.

### Audio

Unzip the download script and use it like this:

```
sh download.sh <your_username> <your_password> train|dev <your_local directory> wav
```

Audio format is

```
RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 16000 Hz
```
(~108MB per hour)

Audio downloads should include checksum files. If that's not the case, [here](ftp://mgb-arabic.cloudapp.net/data/checksum.tgz) is a set of checksum files for the different data subsets.

The total size for data is 129G for training, 1.4G for dev and 1.4G eval data.

### XML Metadata

You can download the metadata in the same way as audio by replacing wav with xml in the command above. Below is a set of zipped archives that will be faster to download.

* [train](ftp://mgb-arabic.cloudapp.net/data/train.xml.tgz)
* [dev](ftp://mgb-arabic.cloudapp.net/data/dev.xml.tgz) 


### Arabic Pronunciation Dictionary

The dictionary is available through QCRI (Qatar Computing Research Institute) website. You can download through link [here](http://).

### Textual Training Data

Textual training data can be downloaded the same way as audio data by replacing wav with tgz:
```
sh download.sh <your_username> <your_password> lm|lm.orig|lm.processed <your_local directory> tgz
```

* [lm.orig](ftp://mgb-arabic.cloudapp.net/data/lm/mgb.arabic.lm.text.original.tgz)
* [lm.processed](ftp://mgb-arabic.cloudapp.net/data/lm/mgb.arabic.lm.text.processed.tgz)

*lm.orig* has the textual training data in UTF-8 encoding as on Al-Jazeera website. No normalization has been done.
*lm.processed* has the normalized textual training data in buckwalter with non-Arabic characters removed.



