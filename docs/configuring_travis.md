# Configuring Travis

ModelDB runs continuous integration testing via [Travis-CI](https://travis-ci.org/mitdbg).

When new branches are pushed, Travis...

- Launches a new Amazon S3 Server (we do not have direct access to it)
    - The exact build is based the `sudo`, `dist`, `language`, and `addons` parameters in `/.travis.yml`
- Runs through the commands in `.travis.yml` `before_install`, `install`, `before_script`, and `script`
- Records `error`, `fail`, or `pass`

## Travis can be picky.

Hopefully these notes help.

- Travis doesn't actually use bash script. It seems to do some pre-processing, and then executes the result.
    - This means that that commands executed manually may fail, while if they're executed in a script, they may pass.
    - Make sure to add shebangs to the top of `.sh` files. i.e. `#!/usr/bin/env bash`
- Travis occasionally doesn't like `&&` commands, and will fail. It's unclear why, but breaking them up into separate lines helps.
- Travis *can* come with some built-in packages. This significantly speeds up build time, but versions are limited.
- It can be difficult to get Travis to record logs. If your scripts are failing unexpectedly, try giving travis some sleep time after executing.
    For example, sleep for 30 seconds: `-/bin/sleep 30`
- Travis [does not have near future plans](https://github.com/travis-ci/travis-ci/issues/4090) to support multiple-languages in a single `.yml` file. Given that this uses 3 languages, that means most packages in need to be configured manually
