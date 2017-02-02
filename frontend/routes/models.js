var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET specific model */
router.get('/:id', function(req, res, next) {
  var modelId = req.params.id;
  api.getModel(modelId, function(response) {
    res.render('model', {
      title: 'Model',
      path: ' > Model',
      menu: false,
      models: [response]
    });
  });
});

router.get('/:id/annotations', function(req, res, next) {
  var modelId = req.params.id;
  api.getAnnotations(modelId, function(response) {
    res.json(response);
  })
});

router.post('/:id/annotations', function(req, res, next) {
  var modelId = req.params.id;
  var experimentRunId = req.body.experimentRunId;
  var string = req.body.string;
  api.storeAnnotation(modelId, experimentRunId, string, function(response) {
    console.log(response);
    res.json(response);
  });
});

router.get('/:id/card', function(req, res, next) {
  var modelId = req.params.id;
  api.getModel(modelId, function(response) {
    res.render('card', {
      models: [response]
    });
  });
});

router.get('/ancestry/:id', function(req, res, next) {
  var modelId = req.params.id;
  api.getModelAncestry(modelId, function(response) {
    res.json(response);
  });
});

module.exports = router;
