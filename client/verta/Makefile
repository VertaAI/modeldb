# build wheel and source distributions
build: clean
	python setup.py sdist bdist_wheel --universal

# clean previously-created builds
clean:
	rm -rf build dist verta.egg-info

# upload distributions to PyPI
upload: build
	python -m twine upload dist/* -u \${PYPI_USERNAME} -p \${PYPI_PASSWORD}
