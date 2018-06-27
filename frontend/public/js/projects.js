var category10 = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
                  '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'];
var numLoading = 0;


$(function() {
  $('#search').appendTo($('nav'));
  $('#search').focus();
  $('#search').hideseek({
    highlight: true
  });


  function fetchData(projectId) {
    $('.projects-loader').slideDown();
    numLoading += 1;
    var root_path = $('.body').context.body.dataset.root;
    $.ajax({
      url: root_path + '/projects/' + projectId + '/ms',
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
              metrics[metric.key].total += metric.val;
              metrics[metric.key].count += 1;
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
                'max': metric.val,
                'total': metric.val,
                'count': 1
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
        project.find('.project-avg-models').append($('<h2>average</h2>'));

        var hasMetrics = false;
        for (var key in metrics) {
          if (metrics.hasOwnProperty(key)) {
            hasMetrics = true;
            var avg = (metrics[key].total/metrics[key].count).toFixed(2)
            project.find('.project-metric-names').append($('<div class="project-metric-name">' + key + '</div>'));
            project.find('.project-min-models').append($('<div class="project-min-model">' + metrics[key].min + '</div>'));
            project.find('.project-max-models').append($('<div class="project-max-model">' + metrics[key].max + '</div>'));
            project.find('.project-avg-models').append($('<div class="project-avg-model">' + avg + '</div>'));
          }
        }

        if (!hasMetrics) {
          project.find('.project-metric-names').append($('<div class="project-metric-name">' + 'N/A' + '</div>'));
          project.find('.project-min-models').append($('<div class="project-min-model">' + 'N/A' + '</div>'));
          project.find('.project-max-models').append($('<div class="project-max-model">' + 'N/A' + '</div>'));
          project.find('.project-avg-models').append($('<div class="project-avg-model">' + 'N/A' + '</div>'));
        }

        if (!jQuery.isEmptyObject(types)) {
          var cursor = 0;
          var bar = project.find('.project-model-types-bar');
          $('<h2>Project Model Types</h2>').insertBefore(bar);

          for (var key in types) {
            if (types.hasOwnProperty(key)) {
              var block = $('<div></div>');
              block.addClass('project-model-type');
              block.addClass('tooltip-trigger');
              block.addClass('disable-click');
              block.data('keys', [key]);
              block.data('values', [types[key] + " models"]);
              var width = 100 * types[key] / response.length;
              block.css({
                'background-color': category10[cursor],
                'width': width + '%'
              });
              cursor += 1;
              bar.append(block);
            }
          }
        }

        numLoading -= 1;
        if (numLoading == 0) {
          $('.projects-loader').slideUp();
        }
      }
    });
  };


  var projects = $('.project');
  for (var i=0; i<projects.length; i++) {
    fetchData($(projects[i]).data('id'));
  }

});
