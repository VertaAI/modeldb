var async = require('async');
var Model = require('../schemas/model.js');
var Project = require('../schemas/project.js');
var Thrift = require('./thrift.js');

module.exports = {

  getModel: function(modelId, callback) {
    Model.getModel(modelId, callback);
  },

  getModels: function(callback) {
    Model.getAll(callback);
  },

  getProjectModels: function(projectId, callback) {
    //Model.getProjectModels(projectId, callback);
    var models = []

    Thrift.client.getRunsAndExperimentsInProject(projectId, function(err, response) {
      var runs = response.experimentRuns;

      async.each(runs, function(item, finish) {
        Thrift.client.getExperimentRunDetails(item.id, function(err, response) {
          Array.prototype.push.apply(models,response.modelResponses);
          finish();
        });
      }, function(err) {

        // reformat metrics
        for (var i=0; i<models.length; i++) {
          var model_metrics = models[i].metrics;
          var metrics = [];
          for (key in model_metrics) {
            if (model_metrics.hasOwnProperty(key)) {
              var val = Object.keys(model_metrics[key]).map(function(k){return model_metrics[key][k]})[0];
              val = Math.round(parseFloat(val) * 1000) / 1000;
              metrics.push({
                "key": key,
                "val": val
              });
            }
          }

          models[i].metrics = metrics;
        }
        callback(models);
      });
    });
  },

  getProjects: function(callback) {
    //Project.getAll(callback);
    Thrift.client.getProjectOverviews(function(err, response) {
      callback(response);
    });
  },


  testConnection: function() {
    console.log("hello");
    var models = []

    Thrift.client.getRunsAndExperimentsInProject(1, function(err, response) {
      var runs = response.experimentRuns;

      async.each(runs, function(item, finish) {
        Thrift.client.getExperimentRunDetails(item.id, function(err, response) {
          Array.prototype.push.apply(models,response.modelResponses);
          finish();
        });
      }, function(err) {
        console.log(models);
      });
    });
  }
};