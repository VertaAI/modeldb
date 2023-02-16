import subprocess
import venv


class LocalModel:
    def __init__(self, model_filename, model_type, deserialization, venv_location="./venv"):
        self.model_filename = model_filename
        self.model_type = model_type
        self.deserialization = deserialization
        self.venv_location = venv_location

    def test(self):
        self.create_venv()
        self.run_model()

    def create_venv(self):
        env_builder = venv.EnvBuilder(with_pip=True, clear=True)
        env_builder.create(self.venv_location)
        # Otherwise ensure_directories will delete the stuff
        env_builder.clear = False
        env = env_builder.ensure_directories(self.venv_location)
        # Always need Verta to use the VertaBaseModel
        subprocess.check_call([env.env_exe, '-m', 'pip', 'install', "verta"])
        environment = ["pandas"]
        for req in environment:
            subprocess.check_call([env.env_exe, '-m', 'pip', 'install', req])
        out = subprocess.run(
            [env.env_exe, "-m", "pip", "list"],
            check=True,
            capture_output=True,
        ).stdout.decode()
        print(out)

    def run_model(self):
        python_bin = self.venv_location + "/bin/python"
        # TODO: download this file
        script_file = "run_local_model.py"
        subprocess.run([python_bin, script_file])
