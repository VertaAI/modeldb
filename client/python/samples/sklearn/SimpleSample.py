import pandas as pd
from sklearn.cross_validation import train_test_split
from sklearn import linear_model
from sklearn.metrics import accuracy_score

# from modeldb.sklearn_native import SyncableMetrics

DATA_PATH = '../../../../data/'
'''
Source: http://archive.ics.uci.edu/ml/datasets/default+of+credit+card+clients
'''

# modeldb start

# name = "simple sample"
# author = "srinidhi"
# description = "simple LR for credit default prediction"
# syncer_obj = Syncer(
#    NewOrExistingProject(name, author, description),
#    DefaultExperiment(),
#    NewExperimentRun("credit test"))

# modeldb end

df = pd.read_csv(DATA_PATH + 'credit-default.csv', skiprows=[0])

# modeldb start
# .read_csv_sync(DATA_PATH + 'credit-default.csv', skiprows=[0])
# modeldb end

target = df['default payment next month']
df = df[["LIMIT_BAL", "SEX", "EDUCATION", "MARRIAGE", "AGE"]]

x_train, x_test, y_train, y_test = train_test_split(
    df, target, test_size=10)

# modeldb start
# .train_test_split_sync(df, target, test_size=0.3)
# modeldb end

lr = linear_model.LogisticRegression()

lr.fit(x_train, y_train)
# modeldb start
# .fit_sync(x_train, y_train)
# modeldb end

y_pred = lr.predict(x_test)
# modeldb start
# .predict_sync(x_test)
# modeldb end

score = accuracy_score(y_test, y_pred)
# modeldb start
# SyncableMetrics.compute_metrics(
#     lr, accuracy_score, y_test, y_pred, x_train, "features",
#     'default payment next month')
# modeldb end

# modeldb start
# syncer_obj.sync()
# modeldb end
