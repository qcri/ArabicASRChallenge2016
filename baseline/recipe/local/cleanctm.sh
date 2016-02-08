#!/bin/bash

# Copyright (C) 2016, Qatar Computing Research Institute, HBKU

java -version

x=$(pwd)
segments=$x/$3/segments
ctmfile=$x/$1
ctmdir=$x/$2/scoring
mkdir -p $ctmdir/tmp
cd $x/local/javautils/bin

# For each ctm generated fix the timing information and sort it for scoring
    java -cp ".:$x/local/javautils/lib/*" mgbmain.CleanCTM $ctmfile $segments $ctmdir/tmp
    # Concatenate individual ctms for scoring
    echo "Concatenating the ctm files"
    ls $ctmdir/tmp | while read cleanctm; do
	cat $ctmdir/tmp/$cleanctm >> $ctmfile.updated
    done
    rm $ctmdir/tmp/*

rm -rf $ctmdir/tmp


