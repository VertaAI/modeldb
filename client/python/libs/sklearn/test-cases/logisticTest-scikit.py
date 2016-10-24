import numpy as np
import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model
import client.SyncableRandomSplit as SyncableRandomSplit
import client.SyncableMetrics as SyncableMetrics
import client.ModelDbSyncer as ModelDbSyncer

name = "logistic-test"
author = "srinidhi"
description = "income-level logistic regression"
SyncerObj = ModelDbSyncer.Syncer([name, author, description])
SyncerObj.startExperiment("convert income level to 0 or 1")

df = pd.read_csv("adult.data.csv")
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
    	print("calling fitsync")
        le.fitSync(df[colname])
        transformedVals = le.transformSync(df[colname])
        newDf[colname+"_index"] = transformedVals
    else:
        newDf[colname]=df[colname]

lr = linear_model.LogisticRegression()

X_set, y_set = SyncableRandomSplit.randomSplit(newDf, [0.7, 0.3], 0, newDf['income_level'])
X_train, X_test = X_set[0], X_set[1]
y_train, y_test = y_set[0], y_set[1]

#We don't want to include our label (income_level) when fitting
partialTraining = X_train[X_train.columns[:-1]]
partialTesting = X_test[X_test.columns[:-1]]
lr.fitSync(partialTraining, y_train)

SyncableMetrics.computeMetrics(lr, "precision", partialTesting, "predictionCol", "income_level",y_test)
SyncableMetrics.computeMetrics(lr, "recall", partialTesting, "predictionCol", "income_level",y_test)
SyncerObj.endExperiment()
ModelDbSyncer.Syncer.instance.sync()