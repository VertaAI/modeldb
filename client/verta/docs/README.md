## Local Build Instructions
1. `cd` to this directory
1. `python -m pip install -r requirements.txt` to install Sphinx, the documentation build utility
1. `rm -rf _build/; make html` to clear existing generated files and rebuild pages
1. `open _build/html/index.html` to open the landing page in your web browser

## Deployment Directions
1. (optional) Go to https://readthedocs.org/projects/verta/versions/ to activate a branch to be deployed
1. Go to https://readthedocs.org/projects/verta/builds/ to trigger a build
1. (optional) When done testing, return to https://readthedocs.org/projects/verta/versions/ to deactivate the branch version
