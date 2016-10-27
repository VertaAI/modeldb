var Model = require('../schemas/model.js');
var Project = require('../schemas/project.js');

module.exports = {

  getModel: function(modelId, callback) {
    Model.getModel(modelId, callback);
  },

  getModels: function(callback) {
    Model.getAll(callback);
  },

  getProjectModels: function(projectId, callback) {
    Model.getProjectModels(projectId, callback);
  },

  getProjects: function(callback) {
    Project.getAll(callback);
  }

}