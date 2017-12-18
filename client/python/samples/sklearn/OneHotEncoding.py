import pandas as pd

from sklearn.preprocessing import LabelBinarizer
from sklearn.metrics import accuracy_score

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics

DATA_PATH = '../../../../data/'

name = "logistic regression - one hot encoding"
author = "srinidhi"
description = "predicting income"
syncer_obj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))


def oneHotEncoding(lb, feature, df):
    if lb is None:
        lb = LabelBinarizer()
        feature_numeric = lb.fit_transform(df[[feature]])
    else:
        feature_numeric = lb.transform(df[[feature]])
    col_names = list(map(lambda x: feature + "_" +
                    str(x).strip(), list(lb.classes_)))
    if lb.classes_.shape[0] == 2:
        col_names = col_names[:1]
    feature_df = pd.DataFrame(
        feature_numeric, columns=col_names, index=df.index)
    df = df.join(feature_df)
    return [lb, df]


orig = pd.read_csv_sync(DATA_PATH + 'adult_with_colnames.csv', index_col=0)
[train, test] = cross_validation.train_test_split_sync(
    orig, test_size=0.3, random_state=501)


[lb, train] = oneHotEncoding(None, "workclass", train)
cols = [col for col in train.columns if "workclass_" in col]
[lb2, train] = oneHotEncoding(None, "sex", train)
cols = [col for col in train.columns if "sex_" in col]
train = train.drop(["workclass", "sex"], axis=1)
new_cols = [
    col for col in train.columns if "workclass_" in col or "sex_" in col]

logreg = linear_model.LogisticRegression(C=10)
features = ['capital-gain', 'capital-loss', 'age'] + new_cols
logreg.fit_sync(train[features], train.income)

[lb, test] = oneHotEncoding(lb, "workclass", test)
[lb2, test] = oneHotEncoding(lb2, "sex", test)
test = test.drop(["workclass", "sex"], axis=1)

test_pred = logreg.predict_sync(test[features])
test_proba = logreg.predict_proba(test[features])

accuracy = SyncableMetrics.compute_metrics(
    logreg, accuracy_score, test.income, test_pred, test[features],
    "predictionCol", 'income_level')

syncer_obj.sync()
