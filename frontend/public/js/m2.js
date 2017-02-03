const GROUPS_SCROLL_DELAY = 5;
$(function() {

/*
 *  Groups bar caret scrolling
 */

  $(document).on('mousedown', '.overflow-up', function() {
    var bar = $('.groups-bar')[0];
    var heightToScroll = bar.scrollTop;
    var delay = heightToScroll * GROUPS_SCROLL_DELAY;

    $('.groups-bar').animate({scrollTop: 0}, delay);
  });

  $(document).on('mouseup', '.overflow-up', function() {
    $('.groups-bar').stop(true);
  });

  $(document).on('mousedown', '.overflow-down', function() {
    var bar = $('.groups-bar')[0];
    var totalHeight = bar.scrollHeight - bar.clientHeight;
    var heightToScroll = totalHeight - bar.scrollTop;
    var delay = heightToScroll * GROUPS_SCROLL_DELAY;

    $('.groups-bar').animate({scrollTop: bar.scrollHeight}, delay);
  });

  $(document).on('mouseup', '.overflow-down', function() {
    $('.groups-bar').stop(true);
  });

  $(document).on('mouseenter', '.group-block', function(event) {
    var block = $(event.target);
    var key = block.data('key');
    var value = block.data('value');
    var num = block.data('num');

    var top = block.offset().top - $($('.groups-bar-container')[0]).offset().top;
    top += (block.height() / 2 - 20);

    var id = $('body').data('id');

    // if grouping by experiment, additionally fetch the description
    if (key == "Experiment ID" || key == "Experiment Run ID") {
      $.ajax({
        url: '/projects/' + id + '/experiments',
        type: "GET",
        success: function(response) {
          var description = null;
          if (key == "Experiment ID") {
            for (var i=0; i<response.experiments.length; i++) {
              if (response.experiments[i].id == value) {
                description = response.experiments[i].description;
              }
            }
          } else if (key == "Experiment Run ID") {
            for (var i=0; i<response.experimentRuns.length; i++) {
              if (response.experimentRuns[i].id == value) {
                description = response.experimentRuns[i].description;
              }
            }
          }

          var obj = {
            'key': key,
            'value': value,
            'num': num,
            'description': description
          };

          var tooltip = $(new EJS({url: '/ejs/group-tooltip.ejs'}).render(obj));
          tooltip.css({'top': top + 'px'});
          $('.groups-bar-container').append(tooltip);
        }
      });
    } else {
      // otherwise, just show tooltip normally
      var obj = {
        'key': key,
        'value': value,
        'num': num,
        'description': null
      };

      var tooltip = $(new EJS({url: '/ejs/group-tooltip.ejs'}).render(obj));
      tooltip.css({'top': top + 'px'});
      $('.groups-bar-container').append(tooltip);
    }
  });

  $(document).on('mouseleave', '.group-block', function() {
    $('.group-tooltip').remove();
  });


  $(document).on('click', '.md-trigger', function(event) {
    var modelId = $(this).data('id');
    var experimentRunId = $(this).data('experimentrunid');
    $('.md-model-id').html(modelId);
    $('.md-input').data('id', modelId);
    $('.md-input').data('experimentRunId', experimentRunId);

    // fetch annotations
    $.ajax({
      url: '/models/' + modelId + '/annotations',
      type: 'GET',
      success: function(response) {
        $('.md-annotations').html("");
        for (var i=0; i<response.length; i++) {
          $('.md-annotations').append($('<div><div class="md-annotation">' +
            response[i].replace(/Transformer\([0-9]*\)\s/, "") +
            '</div></div>'));
        }
        $('.md-annotations')[0].scrollTop = $('.md-annotations')[0].scrollHeight;
        $('.md-modal').addClass('md-show');
        setTimeout(function(){
          // wasn't working without the timeout
          $('.md-input').focus();
        }, 100);
      }
    });
  });

  $(document).on('click', '.md-close, .md-overlay', function(event) {
    $('.md-modal').removeClass('md-show');
  });

  $(document).on('keyup', '.md-input', function(event) {
    var val = $(event.target).val();
    var code = event.which;
    var modelId = $(event.target).data('id');
    var experimentRunId = $(event.target).data('experimentRunId');

    if (code == 13) {
      storeAnnotation(modelId, experimentRunId, val);
      $(event.target).val("");
    } else {
      if (val.length > 0) {
        $('.md-send').addClass('enabled');
      } else {
        $('.md-send').removeClass('enabled');
      }
    }
  });

  $(document).on('click', '.md-send.enabled', function(event) {
    var input = $('.md-input');
    var modelId = input.data('id');
    var experimentRunId = input.data('experimentRunId');
    var val = input.val();
    storeAnnotation(modelId, experimentRunId, val);
    input.val("");
  });

  function storeAnnotation(modelId, experimentRunId, string) {
    var data = []
    data.push({name:"string", value:string});
    data.push({name:"experimentRunId", value:experimentRunId});
    $.ajax({
      url: '/models/' + modelId + '/annotations',
      type: "POST",
      data: data,
      dataType: "json",
      success: function(response) {
        // add annotation to modal
        $('.md-annotations').append($('<div><div class="md-annotation">' + string + '</div></div>'));
        $('.md-annotations').animate({scrollTop: $('.md-annotations')[0].scrollHeight + 'px'});

        // update annotation message in data table
        $('.annotation-message[data-id="' + modelId + '"]').html(string);
      }
    });
  };

});