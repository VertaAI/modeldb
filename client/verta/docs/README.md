## Local Build Instructions
1. `cd` to this directory
1. `python -m pip install -r requirements.txt` to install Sphinx, the documentation build utility
1. `rm -rf _build/; make html` to clear existing generated files and rebuild pages
1. `open _build/html/index.html` to open the landing page in your web browser

## Deployment Directions
1. (optional) Go to https://readthedocs.org/projects/verta/versions/ to activate a branch to be deployed
    - If your branch doesn't appear, wait about 10 minutes or so for ReadTheDocs to update its index.
1. Go to https://readthedocs.org/projects/verta/builds/ to trigger and deploy a new build
    - When the build is completed, there will be an easy-to-miss "View docs" link to the right.
    - As of March 19, for some reason unknown to me, ReadTheDocs doesn't seem to update the rendered docs for almost a full day after a build is completed. I recommend sticking with local builds for incremental changes, and only build on ReadTheDocs to make sure the final version looks as intended.
1. (optional) When done testing, return to https://readthedocs.org/projects/verta/versions/ to deactivate the branch version
