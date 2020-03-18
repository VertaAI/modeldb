## Developer Installation

From the root directory of the repository, run these commands:
1. `cd client/verta/`
1. `pip install -e .`
   1. This installs Verta from the local source in "editable" mode, allowing the Client code to be
      changed and re-imported without needing to reinstall the package.
1. (optional) `pip install -r requirements.txt`
   1. This installs packages relating to PyPI publication, unit testing, and documentation.

### Troubleshooting

If you receive an `EnvironmentError: [Errno 13]` regarding permissions during `pip install -e .`, consider these potential solutions:
- Append `--user` to the command: `pip install -e . --user`
- Use Python3 by using the command: `pip3 install -e .`

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

## Package Publication

1. Run the test suite and verify that it meets expectations.
1. Update `__version__` in [`__about__.py`](https://github.com/VertaAI/modeldb/blob/master/client/verta/verta/__about__.py) with a new version number.
   - Increment the minor version for backwards-incompatible changes or a major set of new features.
   - Increment the patch version for minor new features and bug fixes.
1. Update [changelog.rst](https://github.com/VertaAI/modeldb/blob/master/client/verta/docs/reference/changelog.rst).
1. Commit the updated `__about__.py` and `changelog.rst` to a new branch and submit a pull request.
1. Once the pull request is approved and merged, a Verta core team member shall tag the merge commit using e.g. `git tag -a client-v0.0.0 -m '' && git push --follow-tags`, with the appropriate version number.

### Publish to PyPI

1. Launch the Client's PyPI publication pipeline on Jenkins.
1. Verify that the package number has been updated [on PyPI](https://pypi.org/project/verta/).
1. The new version will be `pip install`able shortly.

### Publish to conda-forge

1. Some time after PyPI publication, a conda-forge bot will automatically submit [a Pull Request to the `verta` feedstock repository](https://github.com/conda-forge/verta-feedstock/pulls).
1. Make any necessary additional changes to `recipe/meta.yaml`.
   - The only changes we might need to make are in our package's listed dependencies, since the conda-forge bot simply copies it from the previous version.
   - Other organizations would need to make additional changes if e.g. they have C dependencies, but we do not so there is no need to worry.
   - Verify the checksum if you'd like.
1. Wait for the build tests in the Pull Request to succeed.
1. Merge the Pull Request.
1. Verify that the package number has been updated [on Anaconda Cloud](https://anaconda.org/conda-forge/verta).
1. The new version will be `conda install -c conda-forge`able after an hour or so.
