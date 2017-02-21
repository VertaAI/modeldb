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
});