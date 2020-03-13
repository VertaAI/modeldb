from verta import Client

client = Client('https://dev.verta.ai')
client.set_project('Demo - Jenkins+Prometheus')
client.set_experiment('Demo')
run = client.set_experiment_run()

class Predictor(object):
    def __init__(self):
        pass

    def predict(self, X):
        return X

run.log_model(Predictor())