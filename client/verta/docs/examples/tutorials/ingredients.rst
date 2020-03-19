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
    # Commit ea568177a186436c77d1103df2b59b3d3499a90de21ac0175c5d80f273ed3dd1 (Branch: master)
    # Date: 2020-03-19 12:24:47
    #
    #     Initial commit

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
    # Date: 2020-03-19 12:24:47
    #
    #     Initial commit

    print(master.show())
    # unsaved Commit (was Branch: master)
    # Date: 2020-03-19 12:24:47
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
    # Commit b1e02c26db8b4da656146ad0aeaebbb3d7df6edb4cb2e2761d2aae4784d1eb3b (Branch: master)
    # Date: 2020-03-19 13:51:27
    #
    #     Adding demo dataset

The commit is now saved in the server and the branch has been updated.

**TODO: add log**
**TODO: add webapp**

Branch operations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Next, let's version the environment that we want to use to train models. We'll use the previous commit
as base and build on a branch:

.. code-block:: python

    env = repo.get_commit(branch="master")
    env.branch("environment")
    print(commit)
    # Commit 7349a807b9b5de692938c03e5d3b63d8c8f6e359e8eb1075cc5bcdbabffbdc46 (Branch: environment)
    # Date: 2020-03-19 14:07:50
    #
    #     Adding demo dataset

Now we have the same commit but it's registered for the new branch. Any changes we do in the new
branch are not saved to the old one. For example, let's register our current Python environment:

.. code-block:: python

    from verta.environment import Python
    env.update("environments/python", Python(requirements=["verta"], constraints=Python.read_pip_environment()))
    env.save("Adding local python environment")
    print(env.show())
    # Commit 0fa4095dbad060e1c7faf84fb4f944407171a4f2b12f1606b7d469e95907e59f (Branch: environment)
    # Date: 2020-03-19 14:14:49
    #
    #     Adding local python environment
    #
    # Contents:
    # datasets/strata (dataset.Path)
    # environments/python (environment.Python)

This new commit now has the information about the local Python setup. It lists `verta` as part of its
requirements and adds constraints for all the other libraries, so that we can recreate the environment
with the correct versions.

**TODO: add webapp**

Once we're done with the changes in our branch, we can merge it back into the `master` branch so that
others can benefit from our changes:

.. code-block:: python

    master.merge(env)
    print(master)
    # Commit e2862f3d71f8f4cc47902eb7b8f8545fd6de989a4299f0d8551f58a48283d32d (Branch: master)
    # Date: 2020-03-19 14:22:35
    #
    #     Merge environment into master

For merge operations, a default commit message is added automatically.

References
^^^^^^^^^^

Now that you know the basic operations for versioning the components of the model, you can:

- Check the repository and commit APIs for more information, like manipulating diffs, reverting commits
  and tagging.
- Use the versioned components to version a model.