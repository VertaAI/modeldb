#!/bin/bash

ag $1 | ag -v /_protos/ | ag -v /_swagger/
