import pandas as pd
from sklearn import linear_model
from sklearn.metrics import accuracy_score

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics

DATA_PATH = '../../../../data/'

'''
Source: http://archive.ics.uci.edu/ml/datasets/default+of+credit+card+clients
'''

# modeldb start

name = "simple sample"
author = "srinidhi"
description = "simple LR for credit default prediction"
syncer_obj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("credit test"))

# modeldb end

# modeldb start
df = pd.read_csv_sync(DATA_PATH + 'credit-default.csv', skiprows=[0])
# modeldb end

target = df['default payment next month']
df = df[["LIMIT_BAL", "SEX", "EDUCATION", "MARRIAGE", "AGE"]]

x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
    df, target, test_size=0.3)

lr = linear_model.LogisticRegression(C=2)

# modeldb start
lr.fit_sync(x_train, y_train)
# modeldb end

# modeldb start
y_pred = lr.predict_sync(x_test)
# modeldb end

# modeldb start
score = SyncableMetrics.compute_metrics(
    lr, accuracy_score, y_test, y_pred, x_train, "features",
    'default payment next month')
# modeldb end

# modeldb start
syncer_obj.sync()
# modeldb end
