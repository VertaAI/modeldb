# -*- coding: utf-8 -*-
"Entities for registering ML models to the Verta backend."

from tempfile import NamedTemporaryFile

from verta._internal_utils import documentation

from ._model import RegisteredModel
from ._models import RegisteredModels
from ._modelversion import RegisteredModelVersion
from ._modelversions import RegisteredModelVersions


documentation.reassign_module(
    [
        RegisteredModel,
        RegisteredModels,
        RegisteredModelVersion,
        RegisteredModelVersions,
    ],
    module_name=__name__,
)



def log_reference_data(self, X, Y, *args, **kwargs):
    if isinstance(X, pd.Series):
        X = X.to_frame()
    if isinstance(Y, pd.Series):
        Y = Y.to_frame()

    df = pd.DataFrame()

    for c in X.columns:
        df['input.'+c] = X[c]
    for c in Y.columns:
        df['output.'+c] = Y[c]

    df['source'] = "reference"
    df['model_version_id'] = self.id

    with NamedTemporaryFile() as f:
        df.to_csv(f.name, encoding='utf-8', index=False)
        self.log_artifact("reference_data", f.name, *args, **kwargs)


RegisteredModelVersion.log_reference_data = log_reference_data