Download Instructions
==

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


### Arabic Pronunciation Dictionary

The dictionary is available through QCRI(Qatar Computing Research Institute) website. You can download through link [here](http://).

### XML Metadata

You can download the metadata in the same way as audio by replacing wav with xml in the command above. Below is a set of zipped archives that will be faster to download.

* [train.full](ftp://)
* [train.short](ftp://)
* [dev.full](ftp://) (dev.full baseline segmentation and clustering ; dev.full baseline segmentation only)
* [dev.short](ftp://)
dev.longitudinal (dev.longitudinal baseline segmentation and clustering ; dev.longitudinal baseline segmentation only)

Evaluation metadata

* [Task 1 / 2 evaluation set](ftp://) - baseline segmentation ;
* [Task 1 /2 evaluation set](ftp://) - baseline segmentation and clustering
* [Task 3 / 4 evaluation set](ftp://) - baseline segmentation ;
* [Task 3 / 4 evaluation set](ftp://) - baseline segmentation and clustering
* [Task 2 captions for alignment](ftp://)
To extract the speaker colours and captions for alignment in Task 2, you can use this XMLStarlet example command:

```
xml sel -t -m "//segments[@annotation_id='transcript_orig']" -m "segment"
-n -v "concat(//speaker[@id=current()/@who]/@name,'|',text())" 20080520_200000_bbcone_holby_city.xml
```
