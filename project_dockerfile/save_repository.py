HOST="https://socure.app.verta.ai"

# Save project details in a repository
from verta import Client

client = Client(HOST)

repo = client.get_or_create_repository("My repo")
master = repo.get_commit()
print(master)

from verta.environment import Python
master.update(
    "project1/environment",
    Python(
        requirements=["verta", "sklearn"],
        constraints=Python.read_pip_environment(), # Ensure all transient dependencies stay the same
    ),
)
master.save("Adding local python environment")

from verta.code import Git
master.update(
    "project1/code",
    Git(),
)
master.save("Adding code repo")
