var mongoose = require('mongoose');
var Model = require('./model.js');
var Schema = mongoose.Schema;
var ObjectId = Schema.ObjectId;

var metricSchema = new Schema({
    model_id: {type: ObjectId, ref: 'Model'},
    key: String,
    value: String
});

// get metrics for specified model
metricSchema.statics.getMetrics = function(model_id, callback) {
    Metric.find({model_id: model_id})
        .exec(function(err, docs){
            if (err) {
                callback({code: 500, err: 'Failed to fetch metrics for model: ' + model_id});
            } else {
                callback({code: 200, data: docs});
            }
        });
}

var Metric = mongoose.model('Metric', metricSchema);
module.exports = Metric;