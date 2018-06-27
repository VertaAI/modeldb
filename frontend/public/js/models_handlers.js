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
      var root_path = $('.body').context.body.dataset.root;
      $.ajax({
        url: root_path + '/projects/' + id + '/experiments',
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

          var tooltip = $(new EJS({url: root_path + '/ejs/group-tooltip.ejs'}).render(obj));
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

      var tooltip = $(new EJS({url: root_path + '/ejs/group-tooltip.ejs'}).render(obj));
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

  // filter toggle options
  $(document).on('click', '.filter-expand', function(event) {
    var elt = $(event.target);
    var filter = elt.closest('.filter');
    var options = elt.next('.filter-options');
    if (elt.data('show')) {
      // hide options
      options.slideUp();
      elt.html("&#9660;");
      elt.data('show', false);
    } else {
      // show options
      options.slideDown();
      options.find('input[type="text"]').val(filter.data('val').join(', '));
      options.find('input[type="text"]').focus();
      elt.html("&#9650;");
      elt.data('show', true);
    }
  });

  // show json modal
  $(document).on('click', '.json-md-trigger', function(event) {
    // TODO: duplicate call; avoid
    var modelId = $(event.target).data('id');
    var root_path = $('.body').context.body.dataset.root;
    $.ajax({
      url: root_path + '/models/' + modelId + '/metadata',
      type: "GET",
      success: function(response) {
        var node = new PrettyJSON.view.Node({
          el:$('#md-json'),
          data:JSON.parse(response)
        });
        node.expandAll();
        var editButton = $('<button class="edit-button">Edit</button>');
        var saveButton = $('<button class="save-button" disabled>Save Changes</button>');
        $('#md-json').append(editButton);
        $('#md-json').append(saveButton);
        saveButton.hide();
        $('#md-json').data('modelId', modelId);
        $('#modal-2').addClass('md-show');
        attachModalListeners();
        node.collapseAll();
      }
    });
  });

  $(document).on('click', '.md-close, .md-overlay', function(event) {
    $('.md-modal').removeClass('md-show');
  });

  $(document).on('click', '.edit-button', function(event) {
    $('.edit-button').hide();
    $('.save-button').show();
  });

  // close modal with escape key
  $(document).keyup(function(event) {
    if (event.which == 27) {
      $('.md-modal').removeClass('md-show');
    }
  });

  function attachModalListeners() {
    var json = $('#md-json');
    var leaves = $('#md-json .leaf-container');
    for (var i=0; i<leaves.length; i++) {
      leaves[i] = leaves[i].closest('li');
      var leaf = $(leaves[i]);
      leaf.data('json', true);

      // figure out key value
      var key = leaf.children().html().split("&nbsp;")[0];
      var value = leaf.find('.leaf-container span').html().trim();
      var valueWithoutQuotes = value.replace(/"/g,"");
      value = (valueWithoutQuotes == value) ? parseFloat(value) : valueWithoutQuotes;
      var parent = leaf.parent().closest('li');

      while (parent.length != 0) {
        var split = parent.children().html().split("&nbsp;");

        // flatten the array
        if (split[0].match("node-top node-bracket")) {
          // nested array element
          leaf.addClass('nkv'); 
          key = getArrayIndex(leaf) + "." + key;
        } else if (key.match('<span>')) {
          // regular array element 
          key = key.replace(/<.+?>|,|\s/g, '').trim();
          key = split[0] + "." + getArrayIndex(leaf);
          leaf.addClass('elt');
        } else {
          // not an array element
          key = split[0] + "." + key;
        }
        parent = parent.parent().closest('li');
      }

      // hide MongoDB id
      if (key === '_id.$oid') {
        leaf.parent().closest('li').remove();
      }
      leaf.data('key', "md." + key);
      leaf.data('val', value);
    }

    leaves.addClass('kv');
    leaves.addClass('json-kv');

    // make metadata field editable
    $(document).on('click', '.leaf-container', function(event) {
      var leaf = $(this).closest('.json-kv');

      if (leaf.data('key') !== 'md.MODELDB_model_id' && leaf.data('key') !== 'md.date-created' && $('.save-button').is(':visible')) {
        $(this).attr('contenteditable', 'true');
        if (leaf.is('.ui-draggable')) {
          leaf.draggable('disable');
        }
        leaf.addClass('editable-content');
        $('.save-button').removeAttr('disabled');
        $('.save-button').text('Save Changes')
      }     
    });

    $(document).on('click', '.save-button', function(event) {
      event.stopImmediatePropagation();

      // update the data for each edited fields
      $('.editable-content').each(function () {
        $(this).addClass('edited-content');
        var value = $(this).find('.leaf-container span, .leaf-container font').html().replace(/&nbsp;/g, ' ').trim();
        var valueWithoutQuotes = value.replace(/"/g,'');
        var key = $(this).data('key').replace('md.', '').split('.$date')[0];

        // do type check
        if (isNaN($(this).data('val')) && !isNaN(valueWithoutQuotes)) {
          alert(key + ' cannot be a number');
          $('.save-button').addAttr('disabled');
          return;
        } else if (!isNaN($(this).data('val')) && isNaN(valueWithoutQuotes)) {
          alert(key + ' must be a number');
          $('.save-button').addAttr('disabled');
          return;
        }
        value = isNaN(valueWithoutQuotes) ? valueWithoutQuotes : parseFloat(value);
        $(this).data('val', value);
      });

      var modelId = $('#md-json').data('modelId');
      var kvPairs = {};
      $('.edited-content').each(function () {
        var key = $(this).data('key').replace('md.', '').split('.$date')[0];
        var value = $(this).data('val');
        kvPairs[key] = value;
      });
      console.log(kvPairs)
      editMetadata(modelId, kvPairs);     
    });

    $(document).on('focusout', '.save-button', function(event) {
      if ($(this).is(':visible') && $(this).text().toLowerCase() === 'saved') {
        $('.edit-button').show();
        $('.save-button').hide();
        $('.save-button').text('Save Changes');
      }
    });

  }

  function editMetadata(modelId, kvPairs) {
    var data = [];
    data.push({name: 'kvPairs', value: JSON.stringify(kvPairs)});
    var root_path = $('.body').context.body.dataset.root;
    $.ajax({
      url: root_path + '/models/' + modelId + '/metadata',
      type: "POST",
      data: data,
      dataType: "json",
      success: function(response) {
        $('.save-button').text('Saved');
      },
    });
  }

  function getArrayIndex(leaf) {
    return $(leaf).parents('li').last().find('li:not(.nkv)').index(leaf.closest('li:not(.nkv)'));
  }
});
