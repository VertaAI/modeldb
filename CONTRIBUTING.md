# Contributing to ModelDB

We welcome and solicit contributions to ModelDB. You can contribute to ModelDB in a variety of ways, small or big.

## Types of contributions

- Try it out and give us feedback! Tell us what works for your use case, what missing etc.
- Documentation and Tutorials: We welcome documentation of various types including docstrings, tutorials etc.
- Samples: Examples of ModelDB integration in your workflow (e.g. using ModelDB in spark.ml to do X)
- Issues and Feature Requests: File issues for things that don't work and features you'd like to see (see [Filing Issues](#filing-issues))
- Bug fixes: Pick off an issue and submit a patch (see [Contributing Code](#contributing-code))
- New functionality: Implement new functionality on the client or server or frontend including clients in different languages
- Other: Let us know if you can help with reviewing PRs, designing graphics, benchmarking etc.

## Filing issues

- Open an issue with an informative title and label it [BUG] or [FEATURE]
- Provide a brief summary of what the issue it and why it is important
- If a bug, provide details on how to reproduce it
- If a feature request, provide details about what you are requesting (e.g. what can ModelDB do to address X?)
- Include architectural or visual design details, if any

## Contributing code

_Before contributing code_
- Make sure an issue exists for the bug you are fixing or feature you are implementing. Open one if required.
- If working on a feature, update the issue to include your design choices or design changes you are proposing
- Update who is working on it or if you need more hands
- Update the issue periodically as appropriate (e.g. PR submitted, waiting on something etc.)

_When contributing code_
(adapted from [scikit-learn](https://github.com/scikit-learn/scikit-learn/blob/master/CONTRIBUTING.md))

1. Fork the [project repository](https://github.com/mitdbg/modeldb)
   by clicking on the 'Fork' button near the top right of the page. This creates
   a copy of the code under your GitHub user account.

2. Clone your fork of the modeldb repo from your GitHub account to your local disk:

   ```bash
   $ git clone git@github.com:mitdbg/modeldb.git
   $ cd modeldb
   ```

3. Create a ``feature`` branch to hold your development changes:

   ```bash
   $ git checkout -b my_feature
   ```

   Always use a ``feature`` branch. It's good practice to never work on the ``master`` branch!

4. Develop the feature on your feature branch. Add changed files using ``git add`` and then ``git commit`` files:

   ```bash
   $ git add modified_files
   $ git commit
   ```

   to record your changes in Git, then push the changes to your GitHub account with:

   ```bash
   $ git push -u origin my_feature
   ```

5. Go to the GitHub web page of your fork of the modeldb repo.
Click the 'Pull request' button to send your changes to the project's maintainers for
review. This will send an email to the committers.

6. If your pull request addresses an issue, please use the pull request title
   to describe the issue and mention the issue number in the pull request description.

7. Prefix the title of your pull request with `[MRG]` (Ready for
   Merge), if the contribution is complete and ready for a detailed review.
   Prefix incomplete contributions as `[WIP]` (to indicate a work
   in progress) and change to `[MRG]` when it matures. WIPs may be useful
   to: indicate you are working on something to avoid duplicated work,
   request broad review of functionality or API, or seek collaborators.
   WIPs often benefit from the inclusion of a
   [task list](https://github.com/blog/1375-task-lists-in-gfm-issues-pulls-comments)
   in the PR description

Code Style
- Ensure that new code conforms to the code around it. Since ModelDB has components written in different languages, be aware of the code style in your particular component
- Python: Please conform to the [pep-8](https://www.python.org/dev/peps/pep-0008/) style guide whenever possible. As part of that, please don't `import *` if it is avoidable!

Tests
- Ensure that existing tests are not broken by the new addition.
   - [Travis-CI](https://travis-ci.org/mitdbg/modeldb) automatically runs tests on branches and pull requests
- Add unit tests for every fix or feature
- Add end-to-end tests where appropriate
- python: `python -m unittest discover MODELDB_DIR/client/python/modeldb/tests/sklearn`
- scala: `cd MODELDB_DIR/client/scala/libs/spark.ml && sbt test`
- server (mvn): `cd MODELDB_DIR/server && mvn test -Dthrift_version=THRIFT_VERSION` where `THRIFT_VERSION` is the version of thrift you are using. i.e. `0.10.0` or `0.9.3`


Documentation
- Document every new code addition in the same way as the existing code

Please reach out to through the [Google Group](https://groups.google.com/forum/#!forum/modeldb) or the developer mailing list (modeldb _at_ csail.mit.edu) with questions.
