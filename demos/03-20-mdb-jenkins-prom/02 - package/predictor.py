import cloudpickle
from flask import Flask, request, jsonify, Response
import prometheus_client
from prometheus_client import Counter, Histogram

REQUEST_COUNT = Counter(
    'request_count', 'App Request Count',
    []
)
REQUEST_LATENCY = Histogram('request_latency_seconds', 'Request latency',
    []
)

app = Flask(__name__)
with open('model.pkl', 'rb') as f:
    model = cloudpickle.load(f)

@app.route('/predict', methods=["POST"])
@REQUEST_LATENCY.time()
def predict():
    REQUEST_COUNT.inc()
    req = request.json
    print(req)
    return jsonify(model.predict(req))

CONTENT_TYPE_LATEST = str('text/plain; version=0.0.4; charset=utf-8')

@app.route('/metrics', methods=["GET"])
def metrics():
    return Response(prometheus_client.generate_latest(), mimetype=CONTENT_TYPE_LATEST)

if __name__ == '__main__':
    app.run(host= '0.0.0.0',debug=True)