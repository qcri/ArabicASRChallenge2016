#!/usr/bin/env python
# simple utility to convert bulkwater text to utf8
# 

import sys
import codecs
import re

_bw = u"><|Yp&}"
_nbw = u"AAAyh''"


_backwardNorm = {ord(b):a for a,b in zip(_nbw, _bw)}

def fromBuckWalter(s):
  return s.translate(_backwardNorm)
  
def stripTashkeel(text): 
  text = re.sub (u"[FNKauio\~\`]","",text) # buckwalter Character  list
  return text


infile = codecs.open(sys.argv[1], 'r', 'utf-8')
outfile = codecs.open(sys.argv[2], 'w', 'utf-8')

for line in infile:
  tokens = line.split()
  limit = 4 if len(tokens) == 5 else 5
  for i in xrange(len(tokens)):
    if i >= limit:
      tokens[i] = stripTashkeel(fromBuckWalter(tokens[i]))
  outfile.write(" ".join(tokens))
  outfile.write("\n")

  

 
