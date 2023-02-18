import subprocess
import venv

from verta._internal_utils._artifact_utils import serialize_model


def test_model_build(model, python_env):
    """Test that the model builds with the provided requirements.

    Parameters
    ----------
    model : verta.registry.VertaModelBase
    python_env : verta.environment.Python
    """

    env_builder = venv.EnvBuilder(with_pip=True, clear=True)
    env_builder.create("myenv")
    # Otherwise ensure_directories will delete the stuff
    env_builder.clear = False
    env = env_builder.ensure_directories("myenv")
    # Always need Verta to use the VertaBaseModel
    subprocess.check_call([env.env_exe, '-m', 'pip', 'install', "verta"])
    # TODO: add constraints, env_vars (maybe moot if it's being run locally?), apt_packages
    for req in python_env.requirements:
        subprocess.check_call([env.env_exe, '-m', 'pip', 'install', req])
    out = subprocess.run(
        [env.env_exe, "-m", "pip", "list"],
        check=True,
        capture_output=True,
    ).stdout.decode()
    print(out)

    # TODO: make all filepaths start with /tmp/

    serialized_model, deserialization, model_type = serialize_model(model)
    # Write bytestream to file
    model_filepath = "model.pkl"
    with open(model_filepath, "wb") as f:
        f.write(serialized_model)

    # Try making a subprocess run the model with the above-created python env
    python_bin = "model_test_venv/bin/python"
    # TODO: finish script
    script_file = "run_local_model.py"
    subprocess.run([python_bin, script_file, model_filepath, deserialization, model_type])
