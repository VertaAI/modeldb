var dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const path = require('path');
const app = express();
//const api = require('./api');
var bodyParser = require('body-parser')
var session = require('express-session');
var auth = require('./routes/auth.js');
var cors = require('cors');
var cookieParser = require('cookie-parser');
var proxy = require('http-proxy-middleware');

// config express-session
var sess = {
  secret: 'CHANGE THIS TO A RANDOM SECRET',
  cookie: {},
  resave: false,
  saveUninitialized: true
};

/*
if (process.env.DEPLOYED === 'yes') {
  sess.cookie.secure = true; // serve secure cookies, requires https
}
*/

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

passport.use(strategy);
passport.serializeUser(function (user, done) {
  done(null, user);
});
passport.deserializeUser(function (user, done) {
  done(null, user);
});
app.use(passport.initialize());
app.use(passport.session());

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

if (process.env.DEPLOYED !== 'yes') {
  const aws_proxy = proxy({target: apiAddress, changeOrigin: false, ws: true})
  app.use('/api/v1/*', [secured, setPrivateHeader, disableCache, printer], (req, res, next) => {
    return aws_proxy(req, res, next);
  })
}

app.use(bodyParser.json());

const renameProps = (map) => (obj) => {
  return Object
    .entries(map)
    .reduce((res, [oldProp, newProp]) => {
      const { [oldProp]: oldPropValues, ...restProps } = res;
      return { [newProp]: oldPropValues, ...restProps };
    }, obj);
};
app.get('/api/getUser',
  secured,
  (req, res) => {
    const user = renameProps({
      'https://verta.ai/developer_key': 'developer_key',
      'https://verta.ai/roles': 'roles',
    })(req.user._json);

    res.json(user);
  }
);

app.use('/api/auth/', auth);

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