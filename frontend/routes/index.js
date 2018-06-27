var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET home page. */
router.get('/', function(req, res, next) {
  var root = process.env.ROOT_PATH
  if (typeof root === "undefined") {
      root = ""
  }
  res.redirect(root + '/projects');
});

module.exports = router;
