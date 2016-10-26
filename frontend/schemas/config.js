var mongoose = require('mongoose');
var Model = require('./model.js');
var Schema = mongoose.Schema;
var ObjectId = Schema.ObjectId;

var configSchema = new Schema({
    model_id: {type: ObjectId, ref: 'Model'},
    key: String,
    value: String
});

// get configs for specified model
configSchema.statics.getConfigs = function(model_id, callback) {
    Config.find({model_id: model_id})
        .exec(function(err, docs){
            if (err) {
                callback({code: 500, err: 'Failed to fetch configs for model: ' + model_id});
            } else {
                callback({code: 200, data: docs});
            }
        });
}

var Config = mongoose.model('Config', configSchema);
module.exports = Config;