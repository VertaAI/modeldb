var dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const path = require('path');
const app = express();
const api = require('./api');
const JSON = require('circular-json');

app.use(express.static('client/build'));

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
  res.sendFile(path.join(__dirname + '/client/build' + '/index.html'));
});

const port = process.env.PORT || 3000;
app.listen(port);

console.log(`ModelDB server listening on ${port}`);