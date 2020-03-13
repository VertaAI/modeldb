python3 setup.py sdist bdist_wheel --universal
python3 -m twine upload dist/*
rm -rf build dist verta.egg-info
