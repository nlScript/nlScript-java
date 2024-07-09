#!/bin/bash

# Prerequisites:
# Install Node.js
# npm install -G showdown

echo '<article class="markdown-body">' > index.html
showdown makehtml -i index.md -e showdown-highlight -a -o index.html -p github
echo '</article>' >> index.html


# do the same for simplified.md
echo '<article class="markdown-body">' > simplified.html
showdown makehtml -i simplified.md -e showdown-highlight -a -o simplified.html -p github
echo '</article>' >> simplified.html

# do the same for variables.md
echo '<article class="markdown-body">' > variables.html
showdown makehtml -i variables.md -e showdown-highlight -a -o variables.html -p github
echo '</article>' >> variables.html

# do the same for custom-types.md
echo '<article class="markdown-body">' > custom-types.html
showdown makehtml -i custom-types.md -e showdown-highlight -a -o custom-types.html -p github
echo '</article>' >> custom-types.html

# do the same for type-hierarchy.md
echo '<article class="markdown-body">' > type-hierarchy.html
showdown makehtml -i type-hierarchy.md -e showdown-highlight -a -o type-hierarchy.html -p github
echo '</article>' >> type-hierarchy.html

