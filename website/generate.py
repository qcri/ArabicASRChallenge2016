import markdown2
import os, sys


header = open('header').read()
footer = open('footer').read()

mkin = open(sys.argv[1])
title = mkin.readline()
if mkin.readline().strip() != '==':
  raise 'md format error, first line is not title'

title_section = """<div class="section-title">
<h2>%s</h2>
</div>""" % title

output = markdown2.markdown(mkin.read())

outfile = open(sys.argv[2], 'w')
outfile.write(header)
outfile.write(title_section)
outfile.write(output)
outfile.write(footer)
outfile.close()
