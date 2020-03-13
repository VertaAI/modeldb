FROM python:3.7.3-slim-stretch

RUN set -ex \
    && buildDeps=" \
        libgomp1 \
    " \
    && apt-get update && apt-get install -y $buildDeps --no-install-recommends \
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
    && rm -rf /var/lib/apt/lists/*


COPY ./workflows/requirements.txt usr/src/app/
RUN pip3 install --no-cache-dir -r /usr/src/app/requirements.txt
COPY . /usr/src/app
RUN pip3 install -e /usr/src/app/verta/.

ENV PYTHONPATH /usr/src/app/
RUN jupyter notebook --generate-config
RUN echo "c.NotebookApp.password='sha1:e5ef86290924:a398482531f2eee66144f1d1f57b6e1604578e7d'">>/root/.jupyter/jupyter_notebook_config.py
RUN echo "c.NotebookApp.ip = '0.0.0.0'" >> ~/.jupyter/jupyter_notebook_config.py

ENTRYPOINT jupyter notebook /usr/src/app/workflows/demos/ --ip=0.0.0.0 --allow-root
