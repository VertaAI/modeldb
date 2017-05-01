import dpath.util
import os
import json
import sys


def create_config():
    file_dir = os.path.dirname(__file__)
    filename = file_dir + '/syncer.json'
    old_config = json.load(file(filename, 'r'))
    new_config = json.load(file(filename, 'r'))

    print(
        'This utility helps you create a modeldb configuration file.\n'
        'The file is used by the modeldb python and/or scala clients to '
        'connect to the modeldb server.\n\n'
        'Pressing <ENTER> enters the default values')

    print('\n== Output Filename ==')
    prompt = 'output filename (default=custom_syncer.json): '
    output_filename = str(raw_input(prompt))
    if output_filename == '':
        output_filename = 'custom_syncer.json'

    print('\n== Config Values ==')
    prompt_for_keys(new_config, old_config, True)

    # create file in the current directory
    new_file = open(output_filename, 'w')
    new_file.write(str(json.dumps(new_config, sort_keys=True, indent=4)))
    new_file.close()

    print('\n== RESULT ==')
    print(
        'New syncer file, %s, created at %s') % (output_filename, os.getcwd())


def prompt_for_keys(new_config, d, top_level, path=''):
    for k, v in d.iteritems():
        if isinstance(v, dict):
            print('\n-- %s --') % k
            path = '/%s' % k
            prompt_for_keys(new_config, v, False, path)
        else:
            # necessary because not all leaves are nested
            if top_level is True:
                print('\n-- %s --') % k
                path = ''

            leaf_path = '%s/%s' % (path, k)
            prompt = "{0} (default={1}): ".format(leaf_path[1:], v)
            user_input = raw_input(prompt)

            user_input = to_int_bool_none_or_return(user_input)
            if user_input == '':
                user_input = v

            dpath.util.set(new_config, leaf_path, user_input)


def to_int_bool_none_or_return(value):
    bools = {'true': True, 't': True,
             'false': False, 'f': False,
             }

    nones = ['none', 'null']

    if isinstance(value, int):
        return value

    if isinstance(value, bool):
        return value

    lower_value = value.lower()
    if lower_value in bools:
        return bools[lower_value]
    elif represents_int(lower_value):
        return int(lower_value)
    elif lower_value in nones:
        return None
    else:
        return value


def represents_int(s):
    try:
        int(s)
        return True
    except ValueError:
        return False


if __name__ == '__main__':
    if sys.argv[1] == 'create_config':
        try:
            create_config()
        except KeyboardInterrupt:
            pass
