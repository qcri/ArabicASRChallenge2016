import markdown2
import os, sys


output = """<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <link rel="stylesheet" type="text/css" href="view-source:http://www.mgb-challenge.org/style.css" media="screen" />
</head>

<body>
"""
mkin = open(sys.argv[1])
output += markdown2.markdown(mkin.read())

output += """</body>

</html>
"""

outfile = open(sys.argv[2], 'w')
outfile.write(output)
outfile.close()
