#!/bin/bash

# Download ASRU data set. This is the MediaEval download script
# Usage: sh download.sh <username> <password> <listfile> <destination_dir> <extension>

# where username and password have been given to you in email by an
# ASRU admin; listfile is one of the dataset lists you downloaded with
# this script (train.full; train.short; dev.full; dev.short);
# destination_dir is the place on your local machine where you want
# the files to go; extension is 'wav'

# Example: sh download.sh user pass dev.short myLocalDir wav


user=$1   # Username
pass=$2   # Password
file=$3   # List with file names
folder=$4 # Destination folder
ext=$5    # File extension (wav, xml, ...)

while read v; do
   # For each file in the file list
   echo $v
   BASE=$(basename $v)
   # Destination file
   F=$folder/$BASE.$ext
   T=$folder/.$BASE.$ext
   if [ ! -e "$F" ]; then
     # if file has not been downloaded yet
     rm -f $T
     # Run wget to temp destination and move to final destination
     wget -O $T ftp://${user}:${pass}@mgb-arabic.cloudapp.net/data/$ext/$v.$ext && mv $T $F
   fi
done < $file

# if we've downloaded wavs, download a checksum file too
if [ "$ext" == "wav" ]; then
  set=$(basename "$file")
  F=$folder/$set\_checksums.txt
  T=$folder/.$set\_checksums.txt
  wget -O $T http://${user}:${pass}@data.cstr.ed.ac.uk/asru/data/checksums/$set\_checksums.txt && mv $T $F
fi
