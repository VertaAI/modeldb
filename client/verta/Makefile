# build wheel and source distributions
build: clean
	python3 setup.py sdist bdist_wheel --universal

# clean previously-created builds
clean:
	rm -rf build dist verta.egg-info

# upload distributions to PyPI
upload:
	# checking TWINE_USERNAME and TWINE_PASSWORD env vars
	python3 -m twine upload --non-interactive dist/*

# tag the current commit using the client's version number
tag: VERSION=$$(python3 -c 'import verta; print(verta.__version__)')
tag: TAG="client-v$(VERSION)"
tag:
	git tag -a $(TAG) -m ''
	git push origin tag $(TAG)
