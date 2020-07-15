var dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const path = require('path');
const app = express();
var bodyParser = require('body-parser');
var proxy = require('http-proxy-middleware');

if (process.env.DISABLE_LOGS) {
  console.log = function() {};
  console.info = function() {};
}

const disableCache = (req, res, next) => {
  res.header('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.header('Pragma', 'no-cache');
  res.header('Expires', 0);
  next();
};
const printer = (req, res, next) => {
  console.log('Requesting', req.originalUrl);
  res.on('finish', () => {
    console.info(
      `Returning ${res.statusCode} ${res.statusMessage}; ${res.get(
        'Content-Length'
      ) || 0}b sent`
    );
  });
  next();
};

const apiAddress = `${process.env.BACKEND_API_PROTOCOL}://${
  process.env.BACKEND_API_DOMAIN
}`;
const apiHost = `${process.env.BACKEND_API_DOMAIN}`;

// Since the cloud system is configured by hostname, change the request when it's going to AWS so
// that it appears to be targeted to the right hostname instead of localhost:3000
const hostnameApiSwitch = (req, res, next) => {
  req.headers['x-forwarded-host'] = req.headers['host'];
  req.headers['original-host'] = req.headers['host'];
  req.headers['host'] = apiHost;
  next();
};

if (process.env.DEPLOYED === 'yes') {
  app.use((req, res, next) => {
    if (typeof req.headers['x-envoy-original-path'] !== "undefined") {
      // if (req.url != req.headers['x-envoy-original-path']) {
        req.url = req.headers['x-envoy-original-path'];
      // }
    }
    console.log(req.url)
    next();
  })

  // MDB starts with /v1, while /api is used by the API gateway
  const mdb_proxy = proxy({
    target: process.env.MDB_ADDRESS,
    pathRewrite: {'^/api/v1/modeldb' : '/v1'},
    // logLevel: "debug",
    changeOrigin: process.env.MDB_CHANGE_ORIGIN || false,
    ws: true,
  })
  app.use(
    '/api/v1/modeldb/*',
    [disableCache, hostnameApiSwitch, printer],
    (req, res, next) => {
      return mdb_proxy(req, res, next);
    }
  );

  const artifactory_proxy = proxy({
    target: process.env.ARTIFACTORY_ADDRESS,
    // pathRewrite: {'^/api/v1/artifact' : '/v1'},
    // logLevel: "debug",
    changeOrigin: process.env.ARTIFACTORY_CHANGE_ORIGIN || false,
    ws: true,
  })
  app.use(
    '/api/v1/artifact/*',
    [disableCache, hostnameApiSwitch, printer],
    (req, res, next) => {
      return artifactory_proxy(req, res, next);
    }
  );

  const graphql_proxy = proxy({
    target: process.env.GQL_ADDRESS,
    pathRewrite: {'^/api/v1/graphql/' : '/'},
    // logLevel: "debug",
    changeOrigin: process.env.GRAPHQL_CHANGE_ORIGIN || false,
    ws: true,
  })
  app.use(
    '/api/v1/graphql/*',
    [disableCache, hostnameApiSwitch, printer],
    (req, res, next) => {
      return graphql_proxy(req, res, next);
    }
  );

  app.use(express.static('client/build'));

  // Any left over is sent to index
  app.get('*', (req, res) => {
    res.header('Cache-Control', 'no-cache, no-store, must-revalidate');
    res.header('Pragma', 'no-cache');
    res.header('Expires', 0);
    res.sendFile(path.join(__dirname + '/client/build' + '/index.html'));
  });
} else {
  const aws_proxy = proxy({
    target: apiAddress,
    changeOrigin: process.env.AWS_CHANGE_ORIGIN || false,
    ws: true,
  });

  app.use(
    '/api/v1/*',
    [disableCache, hostnameApiSwitch, printer],
    (req, res, next) => {
      return aws_proxy(req, res, next);
    }
  );
  app.use(
    '/api/auth/*',
    [disableCache, hostnameApiSwitch, printer],
    (req, res, next) => {
      return aws_proxy(req, res, next);
    }
  );

  // app.use(bodyParser.json());

  const local_proxy = proxy({
    target: 'http://localhost:3001',
    changeOrigin: process.env.LOCAL_CHANGE_ORIGIN || false,
    ws: true,
  });
  app.use('*', (req, res, next) => {
    return local_proxy(req, res, next);
  });
}

const port = process.env.PORT || 3000;
app.listen(port);
