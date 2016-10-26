var express = require('express');
var router = express.Router();
var api = require('../util/api.js');

/* GET models listing. */
router.get('/', function(req, res, next) {
	api.getModels(function(response) {
		res.render('models', {
			title: 'Models',
			models: response.data 
		});		
	});
});


module.exports = router;
