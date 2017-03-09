var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET projects listing. */
router.get('/', function(req, res, next) {
  api.getProjects(function(response) {
    res.render('projects', {
      title: 'Projects',
      path: {
        'labels': ['Projects'],
        'links': ['/projects']
      },
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
router.get('/:id/models', function(req, res, next) {
  var id = req.params.id;
  res.render('models', {
    title: 'Models',
    path: {
      'labels': ['Projects', 'Models'],
      'links': ['/projects', '/projects/' + id + '/models']
    },
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
