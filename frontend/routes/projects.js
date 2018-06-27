var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET projects listing. */
router.get('/', function(req, res, next) {
  api.getProjects(function(response) {
    var root = process.env.ROOT_PATH
    if (typeof root === "undefined") {
        root = ""
    }
    res.render('projects', {
      title: 'Projects',
      path: {
        'labels': ['Projects'],
        'links': [root + '/projects']
      },
      menu: false,
      projects: response,
      rootPath: process.env.ROOT_PATH
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
router.get('/:id/models', function(req, res, next) {
  var id = req.params.id;
  var root = process.env.ROOT_PATH
  if (typeof root === "undefined") {
      root = ""
  }
  res.render('models', {
    title: 'Models',
    path: {
      'labels': ['Projects', 'Models'],
      'links': [root + '/projects', root + '/projects/' + id + '/models']
    },
    menu: false,
    id: id,
    rootPath: root
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
  var root = process.env.ROOT_PATH
  if (typeof root === "undefined") {
      root = ""
  }
  api.getProjectModels(projectId, function(response) {
    res.render('card', {
      models: response,
      rootPath: root
    });
  });
});

module.exports = router;
