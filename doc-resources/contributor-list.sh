#!/bin/bash
echo "# Contributors\n" > ../CONTRIBUTORS.md
echo "Contributors in alphabetic order of name.\n ----" >> ../CONTRIBUTORS.md
git shortlog --summary  --email | cut -f 2 | awk ' {print; print ""; print ""}' >> ../CONTRIBUTORS.md
