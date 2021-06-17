# -*- coding: utf-8 -*-

import pickle

from verta.registry import VertaModelBase


class VertaModel(VertaModelBase):
    ARTIFACT_KEY = "artifact"

    def __init__(self, artifacts):
        with open(artifacts[self.ARTIFACT_KEY], "rb") as f:
            self.artifact = pickle.load(f)

    def predict(self, input):
        return self.artifact
