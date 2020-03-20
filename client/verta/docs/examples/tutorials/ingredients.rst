Versioning model ingredients
============================

A model is composed of four ingredients: code, config, dataset and environment. If you change any of
them, you might end up with a different model. On the other hand, if you re-use all correctly, you
have everything you need to reproduce the model. In this tutorial, we'll cover manipulation of
repositories and all the different entities necessary to ensure your environment is reproducible.
If you're already familiar with Git, a lot of these concepts will be known to you.

Repositories
^^^^^^^^^^^^

Repositories are containers of source information, just like in Git. You can create a repository
like this:

.. code-block:: python

    from verta import Client
    client = Client(host, email, dev_key)
    repo = client.get_or_create_repository("My awesome repo")

This repository works similarly to a Git repo. You can access it by checking the current commit:

.. code-block:: python

    master = repo.get_commit()
    print(master)
    # Commit b56524a708a45537d7c7a50c6b002eee75cc32a25ca5c88a957f6d1c3e9ffaa1 (Branch: master)
    # Date: 2020-03-19 18:03:45
    #
    #     Initial commit
    #

Every repository starts with a base commit and a branch named `master`, which are automatically
created for you. The `master` branch is also the default branch.

Commit operations
^^^^^^^^^^^^^^^^^

As a first example of versioning the ingredients, let's add some local files as a dataset:

.. code-block:: python

    from verta.dataset import Path
    master.update("datasets/strata", Path("./demos/03-20-strata/dataset"))

    print(master)
    # unsaved Commit (was Branch: master)
    # Date: 2020-03-19 18:15:13
    #
    #     Initial commit
    #

    print(master.show())
    # unsaved Commit (was Branch: master)
    # Date: 2020-03-19 18:15:13
    #
    #     Initial commit
    #
    # Contents:
    # datasets/strata (dataset.Path)

As we can see, the dataset was added to the commit but it has not been saved yet. This allows you to
experiment and adjust the commit in a staging area until you're ready to save to the server. Once you
are, we can use:

.. code-block:: python

    master.save("Adding demo dataset")
    print(master)
    # Commit 643d8873e74f7fc3a73ce4404e4a80b56df43ad24365a6e0819d17ffab57e620 (Branch: master)
    # Date: 2020-03-19 18:15:15
    #
    #     Adding demo dataset
    #

The commit is now saved in the server and the branch has been updated. You can also check the line of
commits up to the current:

.. code-block:: python

    for commit in master.log():
        print(commit)

    # Commit 643d8873e74f7fc3a73ce4404e4a80b56df43ad24365a6e0819d17ffab57e620 (Branch: master)
    # Date: 2020-03-19 18:15:15
    #
    #     Adding demo dataset
    #
    # Commit b56524a708a45537d7c7a50c6b002eee75cc32a25ca5c88a957f6d1c3e9ffaa1
    # Date: 2020-03-19 18:15:13
    #
    #     Initial commit
    #

We can also check the content of the repository and its history using the web interface:

.. image:: /_static/gifs/tutorial-ingredients-1.gif

Branch operations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Next, let's version the environment that we want to use to train models. We'll use the previous commit
as base and build on a branch:

.. code-block:: python

    env = repo.get_commit(branch="master")
    env.branch("environment")
    print(env)
    # Commit 643d8873e74f7fc3a73ce4404e4a80b56df43ad24365a6e0819d17ffab57e620 (Branch: environment)
    # Date: 2020-03-19 18:15:15
    #
    #     Adding demo dataset
    #

Now we have the same commit but it's registered for the new branch. Any changes we do in the new
branch are not saved to the old one. For example, let's register our current Python environment:

.. code-block:: python

    from verta.environment import Python
    env.update("environments/python", Python(requirements=["verta"], constraints=Python.read_pip_environment()))
    env.save("Adding local python environment")
    print(env.show())
    # Commit 643d8873e74f7fc3a73ce4404e4a80b56df43ad24365a6e0819d17ffab57e620 (Branch: environment)
    # Date: 2020-03-19 18:15:15
    #
    #     Adding local python environment
    #
    # Contents:
    # datasets/strata (dataset.Path)
    # environments/python (environment.Python)

This new commit now has the information about the local Python setup. It lists `verta` as part of its
requirements and adds constraints for all the other libraries, so that we can recreate the environment
with the correct versions.

If we check the web interface, we can easily compare the two branches and see that the environment was
added in the new branch.

.. image:: /_static/gifs/tutorial-ingredients-2.gif

Once we're done with the changes in our branch, we can merge it back into the `master` branch so that
others can benefit from our changes:

.. code-block:: python

    master.merge(env)
    print(master)
    # Commit 5cb2d7a7963408ce0d00fdc7a9316576888f648e0c829f0acdc3794607c1e18f (Branch: master)
    # Date: 2020-03-19 18:15:30
    #
    #     Merge environment into master
    #

For merge operations, a default commit message is added automatically.

References
^^^^^^^^^^

Now that you know the basic operations for versioning the components of the model, you can:

- Check `the repository and commit APIs <../../api/api/versioning.html>`__ for more information, like
  manipulating diffs, reverting commits and tagging.
  and tagging.
- Use the versioned components to version a model.
