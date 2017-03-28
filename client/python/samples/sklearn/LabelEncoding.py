import pandas as pd

from sklearn import preprocessing
from sklearn import linear_model
from sklearn.metrics import precision_score
from sklearn.metrics import recall_score

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics

DATA_PATH = '../../../../data/'

name = "logistic-test"
author = "srinidhi"
description = "income-level logistic regression"
syncer_obj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

df = pd.read_csv(DATA_PATH + 'adult.data.csv')
new_df = pd.DataFrame()
df.columns = [
    'age', 'workclass', 'fnlwgt', 'education', 'education_num',
    'marital_status', 'occupation', 'relationship', 'race', 'sex',
    'capital_gain', 'capital_loss', 'hours_per_week', 'native_country',
    'income_level']

le = preprocessing.LabelEncoder()

# Assigning 0.0 to represent incomes <=50K, and 1.0 to represent incomes >50K
df['income_level'] = df['income_level'].str.strip()
df['income_level'] = df['income_level'].replace(['<=50K'], [0.0])
df['income_level'] = df['income_level'].replace(['>50K'], [1.0])

# calling labelEncoder on any columns that are object types
for coltype, colname in zip(df.dtypes, df.columns):
    if coltype == 'object':
        le.fit_sync(df[colname])
        transformed_vals = le.transform_sync(df[colname])
        new_df[colname + "_index"] = transformed_vals
    else:
        new_df[colname] = df[colname]

lr = linear_model.LogisticRegression()

x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
    new_df, new_df['income_level'], test_size=0.3, random_state=0)

# We don't want to include our label (income_level) when fitting
partial_training = x_train[x_train.columns[:-1]]
partial_testing = x_test[x_test.columns[:-1]]
lr.fit_sync(partial_training, y_train)
y_pred = lr.predict_sync(partial_testing)
SyncableMetrics.compute_metrics(
    lr, precision_score, y_test, y_pred, partial_testing, "predictionCol",
    'income_level')
SyncableMetrics.compute_metrics(
    lr, recall_score, y_test, y_pred, partial_testing, "predictionCol",
    'income_level')

syncer_obj.sync()
