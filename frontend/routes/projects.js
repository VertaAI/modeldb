var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET projects listing. */
router.get('/', function(req, res, next) {
  api.getProjects(function(response) {
    res.render('projects', {
      title: 'Projects',
      path: ' > Projects',
      menu: false,
      projects: response
    });
  });
});

/* get all models for specific project */
router.get('/:id/models', function(req, res, next) {
  var projectId = req.params.id;
  api.getProjectModels(projectId, function(response) {
    res.render('models', {
      title: 'Models',
      path: ' > Projects > Models',
      menu: true,
      models: response
    });
  });
});

module.exports = router;
