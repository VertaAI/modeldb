import os.path
import requests

EXPT_DIR_KEY = 'EXPT_DIR'
GIT_USERNAME_KEY = 'GIT_USERNAME'
GIT_REPO_KEY = 'GIT_REPO'
GIT_REPO_DIR_KEY = 'GIT_REPO_DIR'
ACCESS_TOKEN_KEY = 'ACCESS_TOKEN'

def version(config):
    # check that the experiment directory exists

    if not os.path.isdir(config[EXPT_DIR_KEY]):
        print 'Experiment dir %s does not exist. Please specify in config.' \
        % config[EXPT_DIR_KEY]
        return None
    
    if not os.path.isdir(config[GIT_REPO_DIR_KEY]):
        print 'Git repo dir %s does not exist. Please specify in config.' \
        % config[GIT_REPO_DIR_KEY]
        return None

    session = requests.Session()
    session.headers['Authorization'] = 'token %s' % config[ACCESS_TOKEN_KEY]
    
    # check if the repo exists, if not create it
    # auth=requests.auth.HTTPBasicAuth('user', 'pwd')
    # repo_exists = session.get(repo_exists_url, auth=auth)
    repo_exists_url = 'https://api.github.com/repos/%s/%s' % \
        (config[GIT_USERNAME_KEY], config[GIT_REPO_KEY])
    repo_exists = session.get(repo_exists_url)

    if repo_exists.status_code == 404:
        print "repo named %s doesn't exist, creating one." % \
        config[GIT_REPO_KEY]

        create_repo_url = 'https://api.github.com/user/repos'
        create_repo_params = {'name' : config[GIT_REPO_KEY]}
        print create_repo_params
        create_repo = session.post(create_repo_url, 
            json=create_repo_params # THIS NEEDS TO SAY JSON WTF
            )
        if create_repo.status_code == 201:
            # successfully created repo
            git_init_cmd = 'pushd %s;git init;' \
                'git remote add origin ' \
                'git@github.com:%s/%s.git;' \
                'popd;' % \
                (config[GIT_REPO_DIR_KEY], 
                    config[GIT_USERNAME_KEY], 
                    config[GIT_REPO_KEY])
            # print(git_init_cmd)
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
    % (config[EXPT_DIR_KEY], config[GIT_REPO_DIR_KEY])

    os.system(copy_code_cmd)

    # commit code. provide a way to specify a commit msg? just use the run id
    # run id can be stored in the modeldb state and can be just reused
    git_commit_cmd = 'pushd %s;' \
    'git add .;' \
    'git commit -m "running experiment";' \
    'git push origin master;' \
    'popd;' % config[GIT_REPO_DIR_KEY]

    # print git_commit_cmd
    os.system(git_commit_cmd)

    git_commit_url = 'https://api.github.com/repos/%s/%s/commits/master' \
    % (config[GIT_USERNAME_KEY], config[GIT_REPO_KEY])
    

    git_commit = session.get(git_commit_url)

    sha = None
    if git_commit.status_code == 200:
        sha = git_commit.json()['sha']
        print sha
    else:
        print "Error performing a git commit on the repo."
        print git_commit.json()

    return sha