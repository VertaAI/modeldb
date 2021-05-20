import json
import sys

import prometheus_client
import spacy
from flask import Flask, Response, jsonify, request
from prometheus_client import Counter, Histogram

REQUEST_COUNT = Counter(
    'request_count', 'App Request Count',
    []
)
REQUEST_LATENCY = Histogram('request_latency_seconds', 'Request latency',
    []
)

with open('model_metadata.json') as f:
    model_metadata = json.load(f)

# From https://spacy.io/usage/saving-loading
nlp = spacy.blank(model_metadata['lang'])
for pipe_name in model_metadata['pipeline']:
    pipe = nlp.create_pipe(pipe_name)
    nlp.add_pipe(pipe)

with open('model.spacy', 'rb') as f:
    nlp.from_bytes(f.read())

app = Flask(__name__)

log = open('/logs/log.txt', 'w')

@app.route('/predict', methods=["POST"])
@REQUEST_LATENCY.time()
def predict():
    REQUEST_COUNT.inc()
    req = request.json
    predictions = []
    for text in req:
        scores = nlp(text).cats
        if scores['POSITIVE'] > scores['NEGATIVE']:
            predictions.append("POSITIVE")
        else:
            predictions.append("NEGATIVE")

    log_entry = json.dumps({"input": req, "output": predictions})
    print(log_entry, file=sys.stderr)
    log.write(log_entry+"\n")
    log.flush()
    return jsonify(predictions)

CONTENT_TYPE_LATEST = str('text/plain; version=0.0.4; charset=utf-8')
@app.route('/metrics', methods=["GET"])
def metrics():
    return Response(prometheus_client.generate_latest(), mimetype=CONTENT_TYPE_LATEST)

if __name__ == '__main__':
    app.run(host= '0.0.0.0',debug=True)