## Installation
To install the packages needed for building documentation, refer to the *Python 3* steps of the **Developer Installation** instructions in [the contribution guide](../../CONTRIBUTING.md).

## Adding New Python APIs
All publicly-importable classes (no module names starting with `_`) will have their docstrings *automatically* documented thanks to Sphinx and the autosummary extension!

- It may be necessary to use `verta._internal_utils.documentation.reassign_module()` to rewrite some classes' paths. See the codebase for examples.
- New top-levels modules need to be manually added to [python.rst](https://github.com/VertaAI/modeldb/blob/master/client/verta/docs/python.rst)

## Building Documentation
To clear existing generated files and rebuild pages, run:
```bash
make html
```
To open the landing page in your web browser, run:
```bash
open _build/html/index.html
```

## Deployment and Publication
1. (optional) Go to https://readthedocs.org/projects/verta/versions/ to activate a branch to be deployed
    - If your branch doesn't appear, activate (then deactivate) any visible branch to force ReadTheDocs to refresh its listing.
1. Go to https://readthedocs.org/projects/verta/builds/ to trigger and deploy a new build
    - When the build is completed, there will be an easy-to-miss "View docs" link to the right.
1. (optional) When done testing, return to https://readthedocs.org/projects/verta/versions/ to deactivate the branch version
