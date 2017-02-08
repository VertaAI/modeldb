var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET home page. */
router.get('/', function(req, res, next) {
  api.testConnection();
  //res.render('index', { title: 'Express' });
  res.redirect('/projects');
});

module.exports = router;
