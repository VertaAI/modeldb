var mongoose = require('mongoose');
var Model = require('./model.js');
var Schema = mongoose.Schema;
var ObjectId = Schema.ObjectId;

var projectSchema = new Schema({
    name: String,
    models: [{type: ObjectId, ref: 'Model'}],
    created_at: {type: Date, default: Date.now},
    updated_at: {type: Date, default: Date.now}
});

// get all projects
projectSchema.statics.getAll = function(callback) {
    Project.find({}).populate('models').exec(function(err, projects) {
        if (err) {
            callback({code: 500, err: 'Failed to fetch all project'});
        } else {
            callback({code: 200, data: projects});
        }
    });
};

var Project = mongoose.model('Project', projectSchema);
module.exports = Project;