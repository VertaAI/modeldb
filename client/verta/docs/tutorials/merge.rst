Handling merge conflicts
========================

At the moment, the client does not yet fully support resolving merge conflicts. To handle them, it
is recommended to pick out the desired items from the conflicting commits and add them to your
currently-active branch.

Resolution
----------

The Client will let you know when ModelDB can't perform the merge on its own.

.. code-block:: python

    master.merge(development)
    # RuntimeError: merge conflict
    #     resolution is not currently supported through the Client
    #     please create a new Commit with the updated blobs
    #     see https://docs.verta.ai/en/master/examples/tutorials/merge.html for instructions

We can refer to the Web App and check what items are in conflict: in this case, it's the version of
the ``verta`` Python package.

.. image:: /_static/gifs/tutorial-merge-1.gif
    :width: 400px
    :align: center

Knowing that, we can select the version that we want and manually update our commit.

.. code-block:: python

    path = "env/verta"
    master.update(path, development.get(path))

If the commits we're trying to merge in have other changes we'd want, remember to add those as well
before saving.

.. code-block:: python

    for another_path in paths_we_want:
        master.update(another_path, development.get(another_path))

    master.save(message="Merge development")
