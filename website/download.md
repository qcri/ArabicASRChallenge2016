Download Instructions
==

*NOTE: Download is not available yet, please check back again on release date*


After your registeration, you will receive download instructions by email. Any use of this data is bound by the MGB-Challenge Arabic Track data license.

### Audio

Unzip the download script and use it like this:

```
sh download.sh <your_username> <your_password> train.full|train.short|dev.full|dev.short|dev.longitudinal <your_local directory> wav
```

Audio format is

```
RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 16000 Hz
```
(~108MB per hour)

Audio downloads should include checksum files. If that's not the case, [here](http://) is a set of checksum files for the different data subsets.


### XML Metadata

You can download the metadata in the same way as audio by replacing wav with xml in the command above. Below is a set of zipped archives that will be faster to download.

* [train.full](ftp://)
* [train.short](ftp://)
* [dev.full](ftp://) 
* [dev.short](ftp://)


### Arabic Pronunciation Dictionary

The dictionary is available through QCRI (Qatar Computing Research Institute) website. You can download through link [here](http://).

### Textual Training Data

* [lm-train.orig](ftp://)
* [lm-train.processed](ftp://)

