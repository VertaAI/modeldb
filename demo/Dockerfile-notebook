FROM jupyter/scipy-notebook

USER root

# Install Thrift 0.10.0
RUN cd / && \
    wget -q http://archive.apache.org/dist/thrift/0.10.0/thrift-0.10.0.tar.gz && \
    tar -xzf thrift-0.10.0.tar.gz && \
    cd thrift-0.10.0 && \
    ./configure && \
    make && \
    ln -n /thrift-0.10.0/compiler/cpp/thrift /usr/local/bin/thrift

USER $NB_USER

ADD client/python/requirements.txt /home/$NB_USER/requirements.txt

RUN cd /home/$NB_USER && \
    pip2 install -r requirements.txt

# Add modeldb to the PYTHONPATH in all Python notebooks.
# Make Python 2 the only available kernel.
# The PYTHONPATH bit can be removed once modeldb is available as a pip install.
RUN ipython profile create && \
    echo "\nc.InteractiveShellApp.exec_lines=['import sys; sys.path.append(\"/modeldb/client/python\")']" >> ~/.ipython/profile_default/ipython_kernel_config.py && \
    echo "c.KernelSpecManager.whitelist = {'python2'}" >> ~/.jupyter/jupyter_notebook_config.py

COPY . /modeldb

COPY demo/notebooks /home/$NB_USER/work

USER root

RUN cd /modeldb/client/python && \
    ./build_client.sh

USER $NB_USER
