## Installation
To install the packages needed for building documentation, refer to the *Python 3* steps of the **Developer Installation** instructions in [the contribution guide](../../CONTRIBUTING.md).

## Adding a New Tutorial
1. create a new `.rst` file in [`tutorials/`](https://github.com/VertaAI/modeldb/tree/master/client/verta/docs/tutorials)
1. add the new page as an entry in the `toctree` of [`tutorials/tutorials.rst`](https://github.com/VertaAI/modeldb/blob/master/client/verta/docs/tutorials/tutorials.rst); the format is  
   `Page Title <relative/path/to/page>`
   - This makes the new page accessible via the navigation sidebar.

## Building Documentation
To clear existing generated files and rebuild pages, run:
```bash
rm -rf _build/; make html
```
To open the landing page in your web browser, run:
```bash
open _build/html/index.html
```

## Deployment and Publication
1. (optional) Go to https://readthedocs.org/projects/verta/versions/ to activate a branch to be deployed
    - If your branch doesn't appear, wait about 10 minutes or so for ReadTheDocs to update its index.
1. Go to https://readthedocs.org/projects/verta/builds/ to trigger and deploy a new build
    - When the build is completed, there will be an easy-to-miss "View docs" link to the right.
    - As of March 19, for some reason unknown to me, ReadTheDocs doesn't seem to update the rendered docs for almost a full day after a build is completed. I recommend sticking with local builds for incremental changes, and only build on ReadTheDocs to make sure the final version looks as intended.
1. (optional) When done testing, return to https://readthedocs.org/projects/verta/versions/ to deactivate the branch version
