#!/bin/bash

# Download ASRU data set. This is the MediaEval download script
# Usage: sh download.sh <username> <password> <listfile> <destination_dir> <extension>

# where username and password have been given to you in email by an
# ASRU admin; listfile is one of the dataset lists you downloaded with
# this script (train.full; train.short; dev.full; dev.short);
# destination_dir is the place on your local machine where you want
# the files to go; extension is 'wav'

# Example: sh download.sh user pass dev myLocalDir wav

if [ $# -ne 5 ]; then
  head -n 13 $0 | tail -n 12 | sed 's:^#::'
  exit 1
fi

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
