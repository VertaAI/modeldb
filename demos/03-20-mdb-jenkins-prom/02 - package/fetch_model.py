from verta import Client
import cloudpickle

client = Client('https://dev.verta.ai')
proj = client.set_project('Demo - Jenkins+Prometheus')
run = proj.expt_runs[0]

model = run.get_model()
with open('model.pkl', 'wb') as f:
    cloudpickle.dump(model, f)