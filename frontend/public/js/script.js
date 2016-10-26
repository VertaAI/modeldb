(function ($) {

    $.fn.findByData = function (prop, val) {
        var $self = this;
        if (typeof val === 'undefined') {
            return $self.filter(
                function () { return typeof $(this).data(prop) !== 'undefined'; }
            );
        }
        return $self.filter(
            function () { return $(this).data(prop) == val; }
        );
    };

})(window.jQuery);

$(function() {
	var filters = {};
	var newKey = null;
	var newVal = null;

	$('.filters').on('mouseup', function() {
		if (newKey && newVal) {
			addFilter(newKey, newVal);			
		}
		newKey = null;
		newVal = null;
	});

	$(document).on('click', '.filter-close', function(event) {
		var filter = $(event.target).parent('.filter');
		var key = filter.data('key');
		removeFilter(key);
		filter.remove();
	});

	$('.model-config').draggable({
		revert: true,
		revertDuration: 0,
		start: dragStart
	});

	$('.model-metric').draggable({
		revert: true,
		revertDuration: 0,
		start: dragStart
	});

	function dragStart(event, ui) {
		newKey = ui.helper.data('key');
		newVal = ui.helper.data('val');
	}

	function addFilter(key, val) {
		if (filters.hasOwnProperty(key)) {
			// update existing filter
			filters[key] = val;
			var filterDiv = $('.filter').findByData('key', key);
			filterDiv.find('.filter-val').html(val);
		} else {
			// add new filter
			filters[key] = val;
			var filterDiv = getFilterDiv(key,val);
			$('.filters').append(filterDiv);
		}

		// update models visually
		filter();
	}

	function removeFilter(key) {
		delete filters[key];

		// update models visually
		filter();
	}


	function getFilterDiv(key, val) {
		var div = $(
			'<div class="filter">' +
			'<div class="filter-key">'+ key + '</div>:'+
			'<div class="filter-val">'+ val + '</div>' + 
			'<div class="filter-close">X</div>' +
			'</div>'
		);
		div.data('key', key);
		div.data('val', val);

		return div;
	}

	function filter() {
		var models = $('.model');
		var show = Array(models.length).fill(true);
		console.log(show);
		for (var key in filters) {
		  if (filters.hasOwnProperty(key)) {
		  	filterByKey(key, filters[key], show);
		  }
		}

		for (var i=0; i<show.length; i++) {
			if (show[i]) {
				console.log("show " + i);
				$(models[i]).slideDown();
			} else {
				console.log("hide " + i);
				$(models[i]).slideUp()
			}
		}
	}

	function filterByKey(key, val, show) {
		let models = $('.model');
		for (var i=0; i<models.length; i++) {
			let fields = $(models[i]).find('.model-config, .model-metric');
			let field = fields.findByData('key', key)[0];
			if (field && $(field).data('val') === val) {
				console.log('match');
			} else {
				show[i] = false;
			}
		}
	};
});