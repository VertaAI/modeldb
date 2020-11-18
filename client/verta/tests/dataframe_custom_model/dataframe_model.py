from verta.deployment import prediction_input_df


class DataFrameModel(object):
    @prediction_input_df
    def predict(self, input_df):
        return input_df["feature-1"].values + input_df["feature-2"].values
