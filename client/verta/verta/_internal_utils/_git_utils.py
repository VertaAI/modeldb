# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import subprocess

from ..external import six


def get_git_commit_hash(ref="HEAD"):
    # if `ref` is an annotated tag, follow it to a commit hash
    try:
        # https://stackoverflow.com/a/54318490
        git_output = subprocess.check_output(["git", "rev-parse", "{}^{{commit}}".format(ref)])
    except:
        pass
    else:
        git_output = six.ensure_str(git_output)
        ref = git_output.strip()

    try:
        return six.ensure_str(
            subprocess.check_output(["git", "rev-parse", "--verify", ref])
        ).strip()
    except:
        pass
    raise OSError("unable to find git commit hash")


def get_git_commit_dirtiness(ref=None):
    if ref is not None:
        try:  # compare `ref` to the working tree and index
            diffs = six.ensure_str(
                subprocess.check_output(["git", "diff-index", ref])
            ).splitlines()
        except:
            pass
        else:
            return len(diffs) > 0
    else:
        try:
            diff_paths = six.ensure_str(
                subprocess.check_output(["git", "status", "--porcelain"])
            ).splitlines()
        except:
            pass
        else:
            return not all(path.startswith("??") for path in diff_paths)
    raise OSError("unable to determine git commit dirtiness")


def get_git_remote_url():
    try:
        return six.ensure_str(
            subprocess.check_output(["git", "ls-remote", "--get-url"])
        ).strip()
    except:
        pass
    raise OSError("unable to find git remote URL")


def get_git_branch_name(ref="HEAD"):
    try:
        git_output = subprocess.check_output(["git", "branch", "--points-at", ref])
    except:
        pass
    else:
        git_output = six.ensure_str(git_output)
        branches = git_output.strip().splitlines()

        # get currently-checked-out branch
        INDICATOR = '* '
        for branch in branches:
            if branch.startswith(INDICATOR):
                return branch[len(INDICATOR):]

        # fall back to first alphabetically-listed branch
        return branches[0] if branches else ""
    raise OSError("unable to find git branch name")


def get_git_commit_tag(ref="HEAD"):
    try:
        git_output = subprocess.check_output(["git", "tag", "--points-at", ref])
    except:
        pass
    else:
        git_output = six.ensure_str(git_output)
        tags = git_output.strip().splitlines()

        # get first alphabetically-listed tag
        return tags[0] if tags else ""
    raise OSError("unable to find git commit tag")


def get_git_repo_root_dir():
    try:
        dirpath = six.ensure_str(
            subprocess.check_output(["git", "rev-parse", "--show-toplevel"])
        ).strip()
    except:
        pass
    else:
        # append trailing separator
        return os.path.join(dirpath, "")
    raise OSError("unable to find git repository root directory")
