var dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const path = require('path');
const app = express();
const api = require('./api');
var bodyParser = require('body-parser')
var session = require('express-session');
var auth = require('./routes/auth.js');
var cors = require('cors');
var cookieParser = require('cookie-parser');

// config express-session
var sess = {
  secret: 'CHANGE THIS TO A RANDOM SECRET',
  cookie: {},
  resave: false,
  saveUninitialized: true
};

if (app.get('env') === 'production') {
  sess.cookie.secure = true; // serve secure cookies, requires https
}

app.use(bodyParser.json());
app.use(cookieParser());
app.use(session(sess));
app.use(cors());

// Load Passport
const passport = require('passport');
const Auth0Strategy = require('passport-auth0');
// Configure Passport to use Auth0
const strategy = new Auth0Strategy({
    domain: process.env.AUTH0_DOMAIN,
    clientID: process.env.AUTH0_CLIENT_ID,
    clientSecret: process.env.AUTH0_CLIENT_SECRET,
    callbackURL: process.env.AUTH0_CALLBACK_URL
  },
  function (accessToken, refreshToken, extraParams, user, done) {
    // accessToken is the token to call Auth0 API (not needed in the most cases)
    // extraParams.id_token has the JSON Web Token
    // extraParams have id_token, access_token, scope
    // profile has all the information from the user
    return done(null, user, extraParams);
  }
);
const secured = (req, res, next) => {
  if (req.user) {
    return next();
  } else {
    res.status(401).send('user is not authorized');
  }
};
const setPrivateHeader = (req, res, next) => {
  req.headers['Grpc-Metadata-bearer_access_token'] = req.session.passport.extraInfo.access_token;
  req.headers['Grpc-Metadata-source'] = 'WebApp';
  next();
}
passport.use(strategy);
passport.serializeUser(function (user, done) {
  done(null, user);
});
passport.deserializeUser(function (user, done) {
  done(null, user);
});
app.use(passport.initialize());
app.use(passport.session());
app.use(express.static('client/build'));

app.get('/api/getProjects', [secured, setPrivateHeader], (req, res) => {
  api.getFromAPI('/v1/project/getProjects', 'get', req.headers)
    .then(response => {
      res.send(response.data);
    })
    .catch(error => {
      console.log(error);
      res.status(500).send("Internal Server Error");
    })
});

app.get('/api/getExperimentRunsInProject', [secured, setPrivateHeader], (req, res) => {
  api.getFromAPI(
      '/v1/experiment-run/getExperimentRunsInProject',
      'get',
      req.headers,
      req.query) // also req.params for post
    .then(response => {
      res.send(response.data);
    })
    .catch(error => {
      console.log(error);
      res.status(500).send("Internal Server Error");
    })
});

app.get('/api/v1/getServiceStatistics/:modelId', [secured, setPrivateHeader], (req, res) => {
  // Disable caching for content files
  res.header("Cache-Control", "no-cache, no-store, must-revalidate");
  res.header("Pragma", "no-cache");
  res.header("Expires", 0);

  api.getFromAPI(
      `/api/v1/controller/statistics/service/${req.params.modelId}`,
      'get',
      req.headers,
      req.query)
    .then(response => {
      res.send(response.data);
    })
    .catch(error => {
      console.log(error);
      res.status(500).send("Internal Server Error");
    })
});

app.get('/api/v1/getDataStatistics/:modelId', [secured, setPrivateHeader], (req, res) => {
  // Disable caching for content files
  res.header("Cache-Control", "no-cache, no-store, must-revalidate");
  res.header("Pragma", "no-cache");
  res.header("Expires", 0);

  api.getFromAPI(
      `/api/v1/controller/data/service/${req.params.modelId}`,
      'get',
      req.headers,
      req.query)
    .then(response => {
      res.send(response.data);
    })
    .catch(error => {
      console.log(error);
      res.status(500).send("Internal Server Error");
    })
});

app.post('/api/v1/controller/deploy', [secured, setPrivateHeader], (req, res) => {
  api.getFromAPI(
      '/api/v1/controller/deploy',
      'post',
      req.headers,
      req.query,
      req.body,
    )
    .then(response => {
      res.send(response.data);
    })
    .catch(error => {
      res.status(500).send("Internal Server Error");
    })
});

app.get('/api/v1/controller/status/:modelId', [secured, setPrivateHeader], (req, res) => {
  // Disable caching for content files
  res.header("Cache-Control", "no-cache, no-store, must-revalidate");
  res.header("Pragma", "no-cache");
  res.header("Expires", 0);

  api.getFromAPI(
      `/api/v1/controller/status/${req.params.modelId}`,
      'get',
      req.headers,
      req.query,
    )
    .then(response => {
      res.send(response.data);
    })
    .catch(error => {
      console.log(error);
      res.status(500).send("Internal Server Error");
    })
});

app.get('/api/getUser',
  secured,
  (req, res) => {
    const {
      _json
    } = req.user;
    res.json(_json);
  }
);

app.use('/api/auth/', auth);

// The "catchall" handler: for any request that doesn't
// match one above, send back React's index.html file.
app.get('*', (req, res) => {
  res.header("Cache-Control", "no-cache, no-store, must-revalidate");
  res.header("Pragma", "no-cache");
  res.header("Expires", 0);

  res.sendFile(path.join(__dirname + '/client/build' + '/index.html'));
});

const port = process.env.PORT || 3000;
app.listen(port);