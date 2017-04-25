import os
import json


def to_bool(value):
    valid = {'true': True, 't': True, '1': True,
             'false': False, 'f': False, '0': False,
             }

    if isinstance(value, bool):
        return value

    if not isinstance(value, basestring):
        raise ValueError('invalid literal for boolean. Not a string.')

    lower_value = value.lower()
    if lower_value in valid:
        return valid[lower_value]
    else:
        raise ValueError('invalid literal for boolean: "%s"' % value)


if __name__ == '__main__':
    execution_dir = os.getcwd()
    file_dir = os.path.dirname(__file__)
    filename = file_dir + '/syncer.json'
    config = json.load(file(filename, 'r'))
    new_config = json.load(file(filename, 'r'))

    prompt = '> '

    raw_input(
        'This utility helps you create a modeldb configuration file.\n'
        'The file is used by the modeldb python and/or scala clients to '
        'connect to the modeldb server.\n\n'
        'Press <ENTER> to use defaults')

    print('\n== Output Filename ==')
    prompt = 'output filename (default=custom_syncer.json): '
    output_filename = str(raw_input(prompt))
    if output_filename == '':
        output_filename = 'custom_syncer.json'

    print('\n== Thrift ==')

    prompt = 'host (default=%s): ' % config['thrift']['host']
    thrift_host = raw_input(prompt)
    if thrift_host == '':
        thrift_host = config['thrift']['host']
    new_config['thrift']['host'] = thrift_host

    prompt = 'port (default=%s): ' % (config['thrift']['port'])
    thrift_port = raw_input(prompt)
    if thrift_port == '':
        thrift_port = config['thrift']['port']
    thrift_port = int(thrift_port)
    new_config['thrift']['port'] = thrift_port

    print('\n== Syncing Strategy =='
          'eager/lazy')

    prompt = 'syncing strategy (default=%s): ' % config['syncingStrategy']
    syncing_strategy = raw_input(prompt)
    if syncing_strategy == '':
        syncing_strategy = config['syncingStrategy']
    new_config['syncingStrategy'] = syncing_strategy

    print('\n== Project ==')
    prompt = 'name (default=%s): ' % (config['project']['name'])
    project_name = raw_input(prompt)
    if project_name == '':
        project_name = config['project']['name']
    new_config['project']['name'] = project_name

    prompt = 'author (default=%s): ' % (config['project']['author'])
    project_author = raw_input(prompt)
    if project_author == '':
        project_author = config['project']['author']
    new_config['project']['author'] = project_author

    prompt = 'description (default=%s): ' % (config['project']['description'])
    project_description = raw_input(prompt)
    if project_description == '':
        project_description = config['project']['description']
    new_config['project']['description'] = project_description

    print('\n== Experiment ==')
    prompt = 'name (default=%s): ' % (config['experiment']['name'])
    experiment_name = raw_input(prompt)
    if experiment_name == '':
        experiment_name = config['experiment']['name']
    new_config['experiment']['name'] = experiment_name

    prompt = 'description (default=%s): ' % (
        config['experiment']['description'])
    experiment_description = raw_input(prompt)
    if experiment_description == '':
        experiment_description = config['experiment']['description']
    new_config['experiment']['description'] = experiment_description

    print('\n== Experiment Run Description ==')
    prompt = 'experiment run description (default=%s): ' % (
        config['experimentRun']['description'])
    experiment_run_description = raw_input(prompt)
    if experiment_run_description == '':
        experiment_run_description = config['experimentRun']
    new_config['experimentRun'] = experiment_run_description

    print('\n== ShouldCountRows ==')
    prompt = 'Should Count Rows (default=%s): ' % (config['shouldCountRows'])
    should_count_rows = raw_input(prompt)
    if should_count_rows == '':
        should_count_rows = config['shouldCountRows']
    should_count_rows = to_bool(should_count_rows)
    new_config['shouldCountRows'] = should_count_rows

    print('\n== shouldStoreGSCVE ==')
    prompt = 'Should store GSCVE (default=%s): ' % (config['shouldStoreGSCVE'])
    should_store_gscve = raw_input(prompt)
    if should_store_gscve == '':
        should_store_gscve = config['shouldStoreGSCVE']
    should_store_gscve = to_bool(should_store_gscve)
    new_config['shouldStoreGSCVE'] = should_store_gscve

    print('\n== shouldStoreSpecificModels ==')
    prompt = ('Should store specific models (default=%s): '
              % (config['shouldStoreSpecificModels']))
    should_store_specific_models = raw_input(prompt)
    if should_store_specific_models == '':
        should_store_specific_models = config['shouldStoreSpecificModels']
    should_store_specific_models = to_bool(should_store_specific_models)
    new_config[
        'shouldStoreSpecificModels'] = should_store_specific_models

    print('\n== Git ==')
    prompt = 'version_code (default=%s): ' % (config['git']['versionCode'])
    git_version_code = raw_input(prompt)
    if git_version_code == '':
        git_version_code = config['git']['versionCode']
    new_config['git']['versionCode'] = git_version_code

    prompt = 'username (default=%s): ' % (config['git']['username'])
    git_username = raw_input(prompt)
    if git_username == '':
        git_username = config['git']['username']
    new_config['git']['username'] = git_username

    prompt = 'access token (default=%s): ' % (
        str(config['git']['accessToken']))
    git_access_token = raw_input(prompt)
    if git_access_token == '':
        git_access_token = config['git']['accessToken']
    new_config['git']['accessToken'] = git_access_token

    prompt = 'repo (default=%s): ' % (str(config['git']['repo']))
    git_repo = raw_input(prompt)
    if git_repo == '':
        git_repo = config['git']['repo']
    new_config['git']['repo'] = git_repo

    prompt = 'export directory (default=%s): ' % (
        str(config['git']['exptDir']))
    git_export_dir = raw_input(prompt)
    if git_export_dir == '':
        git_export_dir = config['git']['exptDir']
    new_config['git']['exptDir'] = git_export_dir

    prompt = 'repo directory (default=%s): ' % (str(config['git']['repoDir']))
    git_repo_dir = raw_input(prompt)
    if git_repo_dir == '':
        git_repo_dir = config['git']['repoDir']
    new_config['git']['repoDir'] = git_repo_dir

    # create file in the current directory
    # execution_dir
    file = open(output_filename, 'w')
    file.write(str(json.dumps(new_config, sort_keys=True, indent=4)))
    file.close()

    print('\n== RESULT ==')
    print(
        'New syncer file, %s, created at %s') % (output_filename, os.getcwd())
