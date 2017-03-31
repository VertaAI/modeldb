import os.path
import requests

def version(version_config):
    # check that the experiment directory exists
    if not os.path.isdir(version_config.export_directory):
        print 'Experiment dir %s does not exist. Please specify in config.' \
        % version_config.export_directory
        return None

    if not os.path.isdir(version_config.repo_directory):
        print 'Git repo dir %s does not exist. Please specify in config.' \
        % version_config.repo_directory
        return None

    session = requests.Session()
    session.headers['Authorization'] = 'token %s' \
        % version_config.access_token

    # check if the repo exists, if not create it
    repo_exists_url = 'https://api.github.com/repos/%s/%s' % \
        (version_config.username,
            version_config.repo)
    repo_exists = session.get(repo_exists_url)

    if repo_exists.status_code == 404:
        print "repo named %s doesn't exist, creating one." % \
        version_config.repo

        create_repo_url = 'https://api.github.com/user/repos'
        create_repo_params = {'name' : version_config.repo}
        print create_repo_params
        create_repo = session.post(create_repo_url,
            json=create_repo_params
            )
        if create_repo.status_code == 201:
            # successfully created repo
            git_init_cmd = 'pushd %s;git init;' \
                'git remote add origin ' \
                'git@github.com:%s/%s.git;' \
                'echo \'.syncer.json\'> .gitignore;' \
                'popd;' % \
                (version_config.repo_directory,
                    version_config.username,
                    version_config.repo)
            # TODO: redirect the output to log or screen?
            os.system(git_init_cmd)
        else:
            print "Error creating repo: %s" % create_repo_url
            print create_repo.json()
            return None
    elif repo_exists.status_code == 200:
        # repo exists
        pass
    else:
        print 'Error with checking if repo exists: %s' % repo_exists_url
        print repo_exists.json()
        return None

    # copy code to the experiment dir
    copy_code_cmd = 'rsync -av --progress %s/* %s --exclude .git' \
    % (version_config.export_directory,
        version_config.repo_directory)
    os.system(copy_code_cmd)

    # commit code
    git_commit_cmd = 'pushd %s;' \
    'git add .;' \
    'git commit -m "running experiment";' \
    'git push origin master;' \
    'popd;' % version_config.repo_directory

    os.system(git_commit_cmd)

    # get the sha for the lastest commit
    git_commit_url = 'https://api.github.com/repos/%s/%s/commits/master' \
    % (version_config.username,
        version_config.repo)
    git_commit = session.get(git_commit_url)

    sha = None
    if git_commit.status_code == 200:
        sha = git_commit.json()['sha']
    else:
        print "Error performing a git commit on the repo."
        print git_commit.json()

    return sha