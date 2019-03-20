// routes/auth.js

var express = require('express');
var router = express.Router();
var passport = require('passport');

router.get('/login', passport.authenticate('auth0', {
  scope: 'openid email profile'
}), function (req, res) {
  res.redirect('/');
});

// Perform the final stage of authentication and redirect to previously requested URL or '/user'
router.get('/callback', function (req, res, next) {
  passport.authenticate('auth0', function (err, user, info) {
    if (err) { 
      return next(err); 
    }
    if (!user) {
      return res.redirect('/login'); 
    }
    req.logIn(user, function (err) {
      if (err) { return next(err); }

      req.session.passport.extraInfo = info;

      const returnTo = req.session.returnTo;
      delete req.session.returnTo;
      res.redirect(returnTo || '/');
    });
  })(req, res, next);
});

// Perform session logout and redirect to homepage
router.get('/logout', (req, res) => {
  req.logout();
  res.status(204).end();
});

module.exports = router;