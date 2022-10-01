## Developer Installation
From the root directory of the repository, run these commands:
1. `cd client/verta/`
1. `python2 -m pip install virtualenv && virtualenv venv/` to create a Python 2 virtual environment
   - developing in Python 2 is recommended to help ensure compatibility
   - if you wish to work in Python 3 (which is necessary for building docs locally), instead run `python3 -m venv venv/`
   - for both Python 2 and Python 3, you can replace the `venv/` argument with any unique name you'd like
1. `source venv/bin/activate` to activate the virtual environment
1. `python -m pip install -e .` to install the client editably from source, which means `import`ing the client will load it from whichever commit of the modeldb repo you currently have checked out.
   - alternatively, `python -m pip install -r requirements-dev.txt` to install a full suite of packages for development, testing, and documentation
   - if you see `error: [Errno 13] Permission denied: 'verta.egg-info/requires.txt'`, delete the `verta.egg-info/` directory and try again

## Linting
Python doesn't have complete static nor compile-time checking, so linting is important for catching potential bugs.

ModelDB uses [`pylint`](https://pylint.org/) as its linter, which was installed during **Developer Installation**. Many Python IDEs can be configured to run `pylint` on file save (see instructions/plugins for [VS Code](https://code.visualstudio.com/docs/python/linting) and [PyCharm](https://plugins.jetbrains.com/plugin/11084-pylint)).

Alternatively, `pylint` can be run manually through the Terminal using
```bash
pylint --disable=R,C,W verta
```
from inside `client/verta/`

## Test Writing
ModelDB uses `pytest` as its testing framework. See [the testing README](verta/tests) for more details.

# Documentation Writing
ModelDB uses `sphinx` as its documentation building framework. See [the docs README](verta/docs) for more details.

## Example and Demo Writing

### Jupyter Notebook Formatting

Before committing a Jupyter notebook we have to:
- remove code cell outputs to prevent merge conflicts later, since outputs may change from run to run
- preserve code cell execution numbers to enable easy reference in walkthroughs

This can be done automatically with a pre-save hook:
1. create a Jupyter config file using `jupyter notebook --generate-config`
1. open the resulting `~/.jupyter/jupyter_notebook_config.py` in a code editor
1. replace the line
   ```python
   #c.ContentsManager.pre_save_hook = None
   ```
   with
   ```python
   def number_cells(model, **kwargs):
       if model['type'] != 'notebook':
           return  # only run on notebooks
       if model['content']['nbformat'] != 4:
           return  # only run on nbformat v4
       code_cells = (cell
                     for cell in model['content']['cells']
                     if cell['cell_type'] == 'code')
       for i, cell in enumerate(code_cells, start=1):
           cell['outputs'] = []
           cell['execution_count'] = i
   c.ContentsManager.pre_save_hook = number_cells
   ```
