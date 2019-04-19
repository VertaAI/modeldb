var dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const path = require('path');
const app = express();
var bodyParser = require('body-parser')
var proxy = require('http-proxy-middleware');

const disableCache = (req, res, next) => {
  res.header("Cache-Control", "no-cache, no-store, must-revalidate");
  res.header("Pragma", "no-cache");
  res.header("Expires", 0);
  next();
}
const printer = (req, res, next) => {
  console.log('Requesting', req.originalUrl)
  res.on('finish', () => {
    console.info(`Returning ${res.statusCode} ${res.statusMessage}; ${res.get('Content-Length') || 0}b sent`)
  })
  next()
}

const apiAddress = `${process.env.BACKEND_API_PROTOCOL}://${process.env.BACKEND_API_DOMAIN}${
  process.env.BACKEND_API_PORT ? `:${process.env.BACKEND_API_PORT}` : ''
}`;
const apiHost = `${process.env.BACKEND_API_DOMAIN}${
  process.env.BACKEND_API_PORT ? `:${process.env.BACKEND_API_PORT}` : ''
}`;

if (process.env.DEPLOYED !== 'yes') {
  // Since the cloud system is configured by hostname, change the request when it's going to AWS so
  // that it appears to be targeted to the right hostname instead of localhost:3000
  const hostnameApiSwitch = (req, res, next) => {
    req.headers['original-host'] = req.headers['host']
    req.headers['host'] = apiHost;
    next()
  }

  const aws_proxy = proxy({target: apiAddress, changeOrigin: false, ws: true})
  app.use('/api/v1/*', [disableCache, hostnameApiSwitch, printer], (req, res, next) => {
    return aws_proxy(req, res, next);
  })
  app.use('/api/auth/*', [disableCache, hostnameApiSwitch, printer], (req, res, next) => {
    return aws_proxy(req, res, next);
  })
  app.use('/api/uac-proxy/*', [disableCache, hostnameApiSwitch, printer], (req, res, next) => {
    return aws_proxy(req, res, next);
  })
}

app.use(bodyParser.json());

if (process.env.DEPLOYED === 'yes') {
  app.use(express.static('client/build'));

  // Any left over is sent to index
  app.get('*', (req, res) => {
    res.header("Cache-Control", "no-cache, no-store, must-revalidate");
    res.header("Pragma", "no-cache");
    res.header("Expires", 0);
    res.sendFile(path.join(__dirname + '/client/build' + '/index.html'));
  });
}
else {
  const local_proxy = proxy({target: 'http://localhost:3001', changeOrigin: false, ws: true})
  app.use('*', (req, res, next) => {
    return local_proxy(req, res, next);
  })
}

const port = process.env.PORT || 3000;
app.listen(port);