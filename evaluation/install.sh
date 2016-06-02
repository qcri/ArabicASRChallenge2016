#!/bin/bash

wget ftp://jaguar.ncsl.nist.gov/pub/sctk-2.4.10-20151007-1312Z.tar.bz2

tar xf sctk-2.4.10-20151007-1312Z.tar.bz2
ln -s sctk-2.4.10
cd sctk
make config
make all
make install
