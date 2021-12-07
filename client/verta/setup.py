import os

from setuptools import find_packages, setup

HERE = os.path.abspath(os.path.dirname(__file__))


about = {}
with open(os.path.join(HERE, "verta", "__about__.py"), "r") as f:
    exec(f.read(), about)

with open("README.md", "r") as f:
    readme = f.read()

with open("requirements.txt", "r") as f:
    install_requires = f.read().splitlines()

setup(
    name=about["__title__"],
    version=about["__version__"],
    maintainer=about["__maintainer__"],
    maintainer_email=about["__maintainer_email__"],
    description=about["__description__"],
    long_description=readme,
    long_description_content_type="text/markdown",
    license=about["__license__"],
    url=about["__url__"],
    packages=find_packages(),
    python_requires=">=2.7, !=3.0.*, !=3.1.*, !=3.2.*, !=3.3.*, !=3.4.*",
    install_requires=install_requires,
    entry_points={
        "console_scripts": [
            "verta = verta._cli:cli",
        ],
    },
)
