$(function() {

  function init() {
    var modelId = $('body').data('id');
    $.ajax({
      url: '/models/' + modelId + '/data',
      type: "GET",
      success: function(response) {
        var model = formatData(response);
        var html = $(new EJS({url: '/ejs/model.ejs'}).render({'model': model}));
        $('.models').append(html);
        $('.kv[data-key="Model ID"]').unwrap();

        var projectId = $('<a href="/projects/' + model["Project ID"] + '/models"></a>');
        projectId.append($('<div class="kv">Project ID: ' + model["Project ID"] + '</div>'));
        $('.model-section.model-ids').append(projectId);

        // update link in breadcrumb to point to project models
        $('.logo a')[2].href += '/' + model["Project ID"] + "/models";
      }
    });

    // set up listeners
    $(document).on('click', '.popup-label', function(event) {
      $(this).parent().toggleClass('open');
    });

    // Model see more listener
    $(document).on('click', '.model-see-more', function(event) {
      var elt = $(event.target);
      var info = elt.parent().find('.model-additional-info');
      var show = elt.data('show');
      if (show) {
        elt.data('show', false);
        elt.html("See More");
        info.slideUp();
        info.animate(
          { opacity: 0 },
          { queue: false, duration: 'slow' }
        );
      } else {
        elt.data('show', true);
        elt.html("See Less");
        info.slideDown();
        info.animate(
          { opacity: 1 },
          { queue: false, duration: 'slow' }
        );
      }
    });
  };

  function formatData(model) {
    var obj = {};

    // id
    obj["id"] = model.id;
    obj["Experiment Run ID"] = model.experimentRunId;
    obj["Experiment ID"] = model.experimentId;
    obj["Project ID"] = model.projectId;

    // dataframe
    obj["DataFrame ID"] = model.trainingDataFrame.id;
    obj["DF numRows"] = model.trainingDataFrame.numRows;
    obj["DF Tag"] = model.trainingDataFrame.tag;
    obj["DF Filepath"] = model.trainingDataFrame.filepath;
    obj["df_metadata"] = model.trainingDataFrame.metadata;

    // specifications
    obj["Specification ID"] = model.specification.id;
    obj["Type"] = model.specification.transformerType;
    obj["Spec Tag"] = model.specification.tag;
    obj["Problem Type"] = model.problemType;
    obj["hyperparams"] = model.specification.hyperparameters;

    // metrics
    obj["metrics"] = model.metrics;

    // misc
    obj["Code SHA"] = model.sha;
    obj["Filepath"] = model.filepath;
    obj["annotations"] = model.annotations;
    obj["metadata"] = model.metadata;

    // show
    obj["show"] = true;

    return obj;
  }

  init();

});