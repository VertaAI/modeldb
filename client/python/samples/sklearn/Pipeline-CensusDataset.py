import numpy as np
import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model
from sklearn.pipeline import Pipeline
from sklearn import decomposition

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics
from modeldb.sklearn_native import SyncableRandomSplit

#Pipelining: This chains a PCA and logistic regression, and uses the UCI Census Adult dataset.

name = "pipeline census"
author = "srinidhi"
description = "census data"
SyncerObj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

df = pd.read_csv("../data/adult.data.csv")
newDf = pd.DataFrame()
df.columns = ['age', 'workclass', 'fnlwgt', 'education','education_num','marital_status',
              'occupation', 'relationship','race', 'sex','capital_gain', 'capital_loss', 'hours_per_week', 'native_country'
              ,'income_level']

le = preprocessing.LabelEncoder()

#Assigning 0.0 to represent incomes <=50K, and 1.0 to represent incomes >50K
df['income_level'] = df['income_level'].str.strip()
df['income_level'] = df['income_level'].replace(['<=50K'],[0.0])
df['income_level'] = df['income_level'].replace(['>50K'],[1.0])

#calling labelEncoder on any columns that are object types
for coltype,colname in zip(df.dtypes, df.columns):
    if coltype == 'object':
        le.fitSync(df[colname])
        transformedVals = le.transformSync(df[colname])
        newDf[colname+"_index"] = transformedVals
    else:
        newDf[colname]=df[colname]

#Creating the pipeline
pca = decomposition.PCA()
lr = linear_model.LogisticRegression()
pipe = Pipeline(steps=[('pca', pca), ('logistic', lr)])

#Separating dataset into training and testing sets
X_set, y_set = SyncableRandomSplit.randomSplit(newDf, [0.7, 0.3], 0, newDf['income_level'])
X_train, X_test = X_set[0], X_set[1]
y_train, y_test = y_set[0], y_set[1]

#We don't want to include our label (income_level) when fitting
partialTraining = X_train[X_train.columns[:-1]]
partialTesting = X_test[X_test.columns[:-1]]

#Fit the pipeline
pipe.fitSync(partialTraining, y_train)

#Compute various metrics on the testing set
SyncableMetrics.computeMetrics(pipe, "f1", partialTesting, "predictionCol", "income_level", y_test)
SyncableMetrics.computeMetrics(pipe, "precision", partialTesting, "predictionCol", "income_level", y_test)

Syncer.instance.sync()