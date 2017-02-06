$(function() {
  $('#search').focus();
  $('#search').hideseek({
    highlight: true
  });


  function fetchData(projectId) {
    $.ajax({
      url: '/projects/' + projectId + '/ms',
      type: "GET",
      success: function(response) {
        var date = null;
        var types = {};
        var metrics = {};

        for (var i=0; i<response.length; i++) {
          var model = response[i];

          // count model types
          var type = model.specification.transformerType;
          if (types.hasOwnProperty(type)) {
            types[type] += 1;
          } else {
            types[type] = 1;
          }

          // figure out last updated
          // TODO: should be "timestamp" instead of "filepath"
          // update this once server api is fixed
          if (date == null || new Date(model.filepath) > date) {
            date = new Date(model.filepath);
          }

          // figure out best and worst metrics
          for (var j=0; j<model.metrics.length; j++) {
            var metric = model.metrics[j];
            if (metrics.hasOwnProperty(metric.key)) {
              if (metric.val < metrics[metric.key].min) {
                metrics[metric.key].min_id = model.id;
                metrics[metric.key].min = metric.val;
              }
              if (metric.val > metrics[metric.key].max) {
                metrics[metric.key].max_id = model.id;
                metrics[metric.key].max = metric.val;
              }
            } else {
              metrics[metric.key] = {
                'min_id': model.id,
                'max_id': model.id,
                'min': metric.val,
                'max': metric.val
              };
            }
          }
        }

        // insert divs
        var project = $('.project[data-id="' + projectId + '"]');
        var numModels = response.length + (response.length == 1 ? " model" : " models");
        project.find('.project-num-models').html(numModels);

        if (date != null) {
          var dd = date.getDate();
          var mm = date.getMonth()+1; //January is 0!
          var yyyy = date.getFullYear();
          dd = (dd < 10 ? '0' + dd : dd);
          mm = (mm < 10 ? '0' + mm : mm);
          project.find('.project-date').html(mm+'/'+dd+'/'+yyyy);
        }

        project.find('.project-metric-names').append($('<h2>Metrics</h2>'));
        project.find('.project-min-models').append($('<h2>min</h2>'));
        project.find('.project-max-models').append($('<h2>max</h2>'));

        var hasMetrics = false;
        for (var key in metrics) {
          if (metrics.hasOwnProperty(key)) {
            hasMetrics = true;
            project.find('.project-metric-names').append($('<div class="project-metric-name">' + key + '</div>'));
            project.find('.project-min-models').append($('<div class="project-min-model">' + metrics[key].min + '</div>'));
            project.find('.project-max-models').append($('<div class="project-max-model">' + metrics[key].max + '</div>'));
          }
        }

        if (!hasMetrics) {
          project.find('.project-metric-names').append($('<div class="project-metric-name">' + 'N/A' + '</div>'));
          project.find('.project-min-models').append($('<div class="project-metric-name">' + 'N/A' + '</div>'));
          project.find('.project-max-models').append($('<div class="project-metric-name">' + 'N/A' + '</div>'));
        }
        console.log(types);
        console.log(metrics);
      }
    });
  };


  var projects = $('.project');
  for (var i=0; i<projects.length; i++) {
    fetchData($(projects[i]).data('id'));
  }

});