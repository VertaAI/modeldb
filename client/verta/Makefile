# build wheel and source distributions
build: clean
	python setup.py sdist bdist_wheel --universal

# clean previously-created builds
clean:
	rm -rf build dist verta.egg-info

# upload distributions to PyPI
upload:
	python -m twine upload dist/* -u $(PYPI_USERNAME) -p $(PYPI_PASSWORD)

# tag the current commit using the client's version number
tag: VERSION=$$(python -c 'import verta; print(verta.__version__)')
tag: TAG="client-v$(VERSION)"
tag:
	git tag -a $(TAG) -m ''
	git push origin tag $(TAG)
