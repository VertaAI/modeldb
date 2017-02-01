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

/* get details for specific project */
router.get('/:id', function(req, res, next) {
  var projectId = req.params.id;
  api.getProject(projectId, function(response) {
    res.json(response);
  });
});

/* get all experiments and runs for a specific project */
router.get('/:id/experiments', function(req, res, next) {
  var projectId = req.params.id;
  api.getExperimentsAndRuns(projectId, function(response) {
    res.json(response);
  });
});

/* get all models for specific project */
router.get('/:id/m', function(req, res, next) {
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

router.get('/:id/models', function(req, res, next) {
  var id = req.params.id;
  res.render('m', {
    title: 'Models',
    path: ' > Projects > Models',
    menu: false,
    id: id
  });
});

router.get('/:id/ms', function(req, res, next) {
  var projectId = req.params.id;
  api.getProjectModels(projectId, function(response) {
    res.json(response);
  });
});

router.get('/:id/table', function(req, res, next) {
  var projectId = req.params.id;
  api.getProjectModels(projectId, function(response) {
    res.render('card', {
      models: response
    });
  });
});

module.exports = router;
