#!/usr/bin/env python
__author__ = 'Yifan Zhang (yzhang@qf.org.qa)'

import os
import sys
import time
import codecs
from xml.dom import minidom

class Element(object):
  def __init__(self, text, startTime, endTime=None):
    self.text = text
    self.startTime = startTime
    self.endTime = endTime

def loadXml(xmlFileName, opts):
  dom = minidom.parse(open(xmlFileName, 'r'))
  trans = dom.getElementsByTagName('transcript')[0]
  segments = trans.getElementsByTagName('segments')[0]
  elements = []
  for segment in segments.getElementsByTagName('segment'):
    sid = segment.attributes['id'].value
    startTime = float(segment.attributes['start'].value)
    endTime = float(segment.attributes['end'].value)
    text = ' '.join((e.childNodes[0].data for e in segment.getElementsByTagName('element')))
    elements.append(Element(text, startTime, endTime))
  return {'id': sid, 'turn': elements}

def stm(data):
  out = codecs.getwriter('utf-8')(sys.stdout)
  for e in data['turn']:
    out.write("{} 0 UNKNOWN {:.02f} {:.02f} ".format(data['id'], e.startTime, e.endTime)) 
    out.write(e.text)
    out.write("\n")

def ctm(data):
  """ generate ctm output for test
  """
  out = codecs.getwriter('utf-8')(sys.stdout)
  for e in data['turn']:
    tokens = e.text.split()
    duration = e.endTime - e.startTime
    interval = duration / len(tokens)
    startTime = e.startTime
    for token in tokens:
      out.write("{} 0 {:.02f} {:.02f} ".format(data['id'], startTime, interval))
      out.write(token)
      out.write("\n")

def main(args):
  data = loadXml(args.xmlFileName, args)
  if args.ctm:
    ctm(data)
  else:
    stm(data)

if __name__ == '__main__':
  import argparse

  parser = argparse.ArgumentParser(description='convert Arabic MGB xml file to MGB xml')
  parser.add_argument("--id", dest="uid",
                      help="utterance id")
  parser.add_argument("--ctm", dest="ctm", default=False, action='store_true',
                      help="output ctm file for testing")
  parser.add_argument("--skip-bad-segments", dest="skip_bs", default=False, action='store_true',
                      help="skip segments with ###, these are either overlapped speech or unintelligible speech")
  parser.add_argument(dest="xmlFileName", metavar="xml", type=str)
  args = parser.parse_args()

  main(args)
