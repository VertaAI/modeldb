from verta import Client
client = Client("https://dev.verta.ai")
dataset = client.set_dataset("test")
print(dataset)
print('')

from verta.dataset import Path
content = Path("test.py")
print(content)
print('')

dataset_version = dataset.create_version(content)
print(dataset_version)
print('')

dataset_version = dataset.get_latest_version()
print(dataset_version)
print('')
