var dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const path = require('path');
const app = express();
const JSON = require('circular-json');
const api = require('./api');

app.get('/api/getProjects', (req, res) => {
  api.getFromAPI('/v1/project/getProjects', req.headers)
  .then(response => {
    res.send(response.data);
  })
  .catch(error => {
    console.log(JSON.stringify(error));
    res.status(500).send("Internal Server Error");
  })
});

app.get('/api/getExperimentRunsInProject', (req, res) => {
  api.getFromAPI(
    '/v1/experiment-run/getExperimentRunsInProject', 
    req.headers,
    req.query) // also req.params for post
  .then(response => {
    res.send(response.data);
  })
  .catch(error => {
    console.log(JSON.stringify(error));
    res.status(500).send("Internal Server Error");
  })
});

app.get('*', (req, res) => {
  tmpPath = req.path;

  if (tmpPath == '/') {
    tmpPath = '/index.html';
  }

  // console.log(req);
  res.sendFile(path.join(__dirname + '/client/build' + tmpPath));
});

const port = process.env.PORT || 3000;
app.listen(port);

console.log(`ModelDB server listening on ${port}`);