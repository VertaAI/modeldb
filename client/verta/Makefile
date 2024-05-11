VERSION:=$$(python3 -c 'import verta; print(verta.__version__)')

# build wheel and source distributions
build: clean
	python setup.py sdist bdist_wheel --universal

# clean previously-created builds
clean:
	rm -rf build dist verta.egg-info

# upload distributions to PyPI
upload:
	# checking TWINE_USERNAME and TWINE_PASSWORD env vars
	python -m twine upload --non-interactive dist/*

# tag the current commit using the client's version number
tag:
	git tag -a client-v$(VERSION) -m ''
	git push origin tag client-v$(VERSION)
