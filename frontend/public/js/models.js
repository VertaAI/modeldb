const MODELS_PER_LOAD = 10;
const DEFAULT_X = "Type";
const DEFAULT_Z = "Experiment Run ID";

var models = [];
var summarySpecs;
var summarySpecsWithTimestamps;
var useTimestamps = false;
var exploreSpecs;
var vlSpec;
var min_id = null;
var max_id = null;
var min_date = null;
var max_date = null;
var hyperparamKeys = {};
var metricKeys = {};
var modelTypes = {};
var keys = ["Experiment Run ID", "Experiment ID", "Project ID",
            "DataFrame ID", "DF numRows", "DF Tag", "DF Filepath",
            "Type", "Spec Tag", "Problem Type",
            "Code SHA", "Filepath"];
var cursor = 0;
var groups = {};
var groupKey = "None";
var groupValue = null;
var category10 = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
                  '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'];

// filter variables
var filterKey = null;
var filterVal = null;
var supportsRange;
var filters = {};
var ranges = {};
var filterId = 0;
var rangeId = 0;

$(function() {

  function init() {
    var id = $('body').data('id');
    console.log("project id: " + id);
    fetchData(id);

    // set up listeners
    $(document).on('click', '.popup-label', function(event) {
      $(this).parent().toggleClass('open');
    });

    $(document).on('click', '.compare-button', function(event) {
      vegaUpdate();
    });

    $(document).on('change', '.group-by', function(event) {
      var key = event.target.value;
      groupTable(key);
    });

    $(document).on('click', '.group-block', function(event) {
      $('.group-selected').removeClass('group-selected');
      var block = $(event.target);
      block.addClass('group-selected');
      groupKey = block.data('key');
      groupValue = block.data('value');
      reloadTable();
    });

    // change tabs
    $(document).on('click', '.menu-tab', function(event) {
      var tabId = $(this).data('id');

      // update highlighted tab in menu
      $('.selected').removeClass('selected');
      $(this).addClass('selected');
      $('.selected-arrow').css({'top': (17 + tabId * 60) + 'px'});

      // calculate left margin
      var tab = $('.tab[data-id="0"]');
      var w = tab.width();
      var margin = (-1 * w - 3) * tabId

      // scroll to proper tab
      tab.animate({'margin-left': margin + 'px'});

      var sections = $('.menu-section-container');
      for (var i=0; i<sections.length; i++) {
        if ($(sections[i]).hasClass('menu' + tabId)) {
          $(sections[i]).slideDown();
        } else {
          $(sections[i]).slideUp();
        }
      }
    });

    $('.models-container').scroll(function(event) {
      if (this.scrollHeight - $(this).scrollTop() <= $(this).outerHeight() + 10) {
        // reached bottom of table, so load more
        loadTable();
      }
    });

    $(document).on('click', '.triangle-container', function() {
      var el = $(this);
      var sorted = el.hasClass('sorted');
      var order = el.data('order');
      var key = el.data('key');

      var dropdown = el.next('.dropdown-sort');

      if (key == null) {
        // use 1st key by default
        key = dropdown.find('option')[1].value // 1 because index 0 is None
        el.data('key', key);
        dropdown.val(key);
      }

      order = (order == 'asc') ? 'dsc' : 'asc';

      $('.sorted').data('order', null);
      el.data('order', order);
      $('.sorted').removeClass('sorted');
      $('.asc').removeClass('asc');
      $('.dsc').removeClass('dsc');
      el.addClass('sorted');
      el.addClass(order);

      sortTable(key, order);
    });

    $(document).on('change', '.dropdown-sort', function(event){
      var key = event.target.value;
      var triangleContainer = $(event.target).prev('.triangle-container');
      triangleContainer.data('key', key);

      var order = triangleContainer.data('order');
      if (order == null) {
        order = 'asc';
      }

      // update classes
      $('.sorted').data('order', null);
      triangleContainer.data('order', order);
      $('.sorted').removeClass('sorted');
      $('.asc').removeClass('asc');
      $('.dsc').removeClass('dsc');
      triangleContainer.addClass('sorted');
      triangleContainer.addClass(order);

      sortTable(key, order);
    });

    $(document).on("mouseenter", '.kv:not(.nkv):not(.elt)', function(event){
      var item = $(this);
      //check if the item is already draggable
      if (item.is('.ui-draggable-disabled')) {
        item.removeClass('editable-content');
        item.addClass('edited-content');
        item.draggable('enable');
      } else if (!item.is('.ui-draggable')) {
        item.draggable({
          helper: 'clone',
          appendTo: 'body',
          revert: true,
          revertDuration: 0,
          start: dragStart,
          stop: dragStop
        });
      }
    });

    $(document).on('mouseup', '.filter-area', function() {
      if (filterKey != null && filterVal != null) {
        if (supportsRange) {
          addRange(filterKey);
        } else {
          addFilter(filterKey, filterVal);
        }
        $('.filter-button').removeClass('filter-button-disabled');
      }
      filterKey = null;
      filterVal = null;
    });

    $(document).on('keyup', '.range-options select, .range-options input', function(event) {
      var range = $(this).closest('.range');
      var id = range.data('id');
      var key = range.data('key');
      var val = range.find('input').val();
      var type = range.find('select').val();
      updateRange(id, key, val, type);
      $('.filter-button').removeClass('filter-button-disabled');

      // enable press enter to filter
      if (event.which == 13) {
        $('.filter-button').click();
      }
    });

    $(document).on('keyup', '.filter-options input[type="text"]', function(event) {
      var elt = $(event.target);
      var filter = $(this).closest('.filter');
      var key = filter.data('key');
      var vals = elt.val().trim().split(/\s*,\s*/).filter(v=>v!='');
      var invert = filter.find('input[type="checkbox"]')[0].checked;

      // update filter
      updateFilter(key, vals, invert);
      filter.find('.filter-val').html(vals.join(', '));
      filter.find('.filter-val').data('val', vals.join(', '));
      filter.data('val', vals);
      $('.filter-button').removeClass('filter-button-disabled');

      // enable press enter to filter
      if (event.which == 13) {
        $('.filter-button').click();
      }
    });

    $(document).on('change', '.filter-invert', function(event) {
      var filter = $(this).closest('.filter');
      var key = filter.data('key');
      var vals = filter.data('val');
      var invert = event.target.checked;
      updateFilter(key, vals, invert);

      if (invert) {
        filter.find('.filter-val').addClass('invert');
      } else {
        filter.find('.filter-val').removeClass('invert');
      }
      $('.filter-button').removeClass('filter-button-disabled');
    });

    $(document).on('click', '.filter-close', function(event) {
      var filter = $(event.target).parent('.filter');
      var key = filter.data('key');
      var val = filter.data('val');
      filter.remove();
      removeFilter(key);
      $('.filter-button').removeClass('filter-button-disabled');
    });

    $(document).on('click', '.range-close', function(event) {
      var range = $(event.target).parent('.range');
      var id = range.data('id');
      range.remove();
      removeRange(id);
      $('.filter-button').removeClass('filter-button-disabled');
    });

    $(document).on('click', '.filter-button', function(event) {
      if (!$(this).hasClass('filter-button-disabled')) {
        filter();
        $(this).addClass('filter-button-disabled');
        hideModelCard();
        vegaInit();
        vegaUpdate();
      }
    });

    $(document).on('click', '.summary-toggle-radio', function(event) {
      var prev = useTimestamps;
      useTimestamps = (event.target.value == "timestamp");
      if (prev != useTimestamps) {
        vegaInit();
      }
    });

  };

  function fetchData(projectId) {
    cursor = 0;
    // get project details
    $.ajax({
      url: '/projects/' + projectId,
      type: "GET",
      success: function(response) {
        $('.project-name').html(response.name);
        $('.project-author').html(response.author);
        $('.project-description').html(response.description);
      }
    });

    $('.loader').fadeIn();
    // get models
    $.ajax({
      url: '/projects/' + projectId + '/ms',
      type: "GET",
      success: function(response) {
        values = [];

        // set number of models
        var numModels = response.length;
        var text = numModels + (numModels == 1 ? " model" : " models");
        $('.project-num-models').html(text);

        for (var i=0; i<response.length; i++) {
          var model = response[i];
          var id = model.id;
          var obj = {};

          if (min_id == null || id < min_id) {
            min_id = id;
          }

          if (max_id == null || id > max_id) {
            max_id = id;
          }

          var timestamp = new Date(model.filepath);

          if (min_date == null || timestamp < min_date) {
            min_date = timestamp;
          }

          if (max_date == null || timestamp > max_date) {
            max_date = timestamp;
          }

          // id
          obj["id"] = id;
          obj["Experiment Run ID"] = model.experimentRunId;
          obj["Experiment ID"] = model.experimentId;
          obj["Project ID"] = model.projectId;

          // dataframe
          obj["DataFrame ID"] = model.trainingDataFrame.id;
          obj["DF numRows"] = model.trainingDataFrame.numRows;
          obj["DF Tag"] = model.trainingDataFrame.tag;
          obj["DF Filepath"] = model.trainingDataFrame.filepath;
          var metadata = model.trainingDataFrame.metadata;
          obj["df_metadata"] = metadata;
          for (var j=0; j<metadata.length; j++) {
            obj[metadata[j].key] = metadata[j].value;
          }

          // specifications
          obj["Specification ID"] = model.specification.id;
          obj["Type"] = model.specification.transformerType;
          if (!modelTypes.hasOwnProperty(model.specification.transformerType)) {
            modelTypes[model.specification.transformerType] = true;
          }
          obj["Spec Tag"] = model.specification.tag;
          obj["Problem Type"] = model.problemType;
          var hyperparameters = model.specification.hyperparameters;
          obj["hyperparams"] = hyperparameters;
          for (var j=0; j<hyperparameters.length; j++) {
            obj[hyperparameters[j].name] = hyperparameters[j].value;

            // add hyperparam key to dictionary
            if (!hyperparamKeys.hasOwnProperty(hyperparameters[j].key)) {
              hyperparamKeys[hyperparameters[j].name] = hyperparameters[j].name;
            }
          }

          // metrics
          var metrics = model.metrics;
          obj["metrics"] = metrics;
          for (var j=0; j<metrics.length; j++) {
            obj[metrics[j].key] = metrics[j].val;

            // add metric key to dictionary
            if (!metricKeys.hasOwnProperty(metrics[j].key)) {
              metricKeys[metrics[j].key] = metrics[j].key;
            }
          }

          // misc
          obj["Code SHA"] = model.sha;
          obj["Filepath"] = model.filepath;
          obj["annotations"] = model.annotations;
          obj["metadata"] = model.metadata;

          // add flattened json metadata data
          if (model.metadata) {
            var flattened = JSON.flatten(JSON.parse(model.metadata));
            for (var flatKey in flattened) {
              if (flattened.hasOwnProperty(flatKey)) {
                obj["md." + flatKey] = flattened[flatKey];
              }
            }
          }

          // TODO: update this once api is fixed
          obj["timestamp"] = timestamp;

          // show
          obj["show"] = true;

          models.push(obj);
        }
        metricKeys = Object.values(metricKeys);
        hyperparamKeys = Object.values(hyperparamKeys);
        selectInit();
        vegaInit();
        reloadTable();
        $('.loader').hide();
      }
    });
  };

  function hideModelCard() {
    $('.model-card').animate({"right": "-300px"});
  };

  function showModelCard(model_id) {
    $('.model-card').css({"right": "-300px"});
    $('.model-card').load('/models/' + model_id + '/card', function() {
      $('.model-card').animate({"right": "20px"});
    });
  };

  function groupTable(key) {
    var container = $('.groups-bar-container');
    var bar = $('.groups-bar')[0];
    var $bar = $(bar);

    // reset values
    groupKey = key;
    groupValue = null;
    groups = {};
    $bar.html("");
    $bar.removeClass('overflow');
    $('.overflow-caret').remove();

    if (key == "None") {
      // remove groups
      $('.groups-open').removeClass('groups-open');
      $('.groups-bar').hide();
    } else {
      // get group counts
      for (var i=0; i<models.length; i++) {
        if (!models[i].show) {
          continue;
        }
        var value = models[i][key];
        if (groups.hasOwnProperty(value)) {
          groups[value] += 1;
        } else {
          groups[value] = 1;
        }
      }

      var groupCursor = 0;
      var numModels = Object.values(groups).reduce((a, b) => a + b, 0);
      var numGroups = Object.values(groups).length;
      var totalHeight = $bar.height() - 2*numGroups;

      // add blocks to groups bar
      for (var value in groups) {
        if (groups.hasOwnProperty(value)) {
          // create group block and append to bar
          var height = (totalHeight * groups[value] / numModels );
          var block = $('<div class="group-block"></div>');
          block.data('key', key);
          block.data('value', value)
          block.data('num', groups[value]);
          block.css({
            'background-color': category10[groupCursor],
            'height': height + 'px'
          });
          $bar.append(block);

          // next color
          groupCursor = (groupCursor + 1) % category10.length;
        }
      }

      $('.groups-bar').css({'display': 'inline-block'});

      // apparently this must be done after display
      // inline-block, otherwise heights will be 0
      if (bar.scrollHeight > bar.clientHeight) {
        $bar.addClass('overflow');
        container.append('<div class="overflow-caret overflow-up"><img src="/images/caret-up.png"></div>');
        container.append('<div class="overflow-caret overflow-down"><img src="/images/caret-down.png"></div>');
      }

      // show groups bar and shrink data table
      $('.model-heading').addClass('groups-open');
      $('.model-section').addClass('groups-open');
      $('.data-table').addClass('groups-open');
    }
    reloadTable();
  };

  function sortTable(key, order) {
    models.sort(function(a, b) {
      var x = (order == "asc" || order == "down") ? 1 : -1;
      if (a[key] == null && b[key] == null) {
        return (a.id > b.id);
      } else if (a[key] == null) {
        return 1;
      } else if (b[key] == null) {
        return -1;
      } else if (a[key] < b[key]) {
        return -1 * x;
      } else if (a[key] > b[key]) {
        return 1 * x;
      } else {
        return 0;
      }
    });

    reloadTable();
  };

  /*
  function loadTable() {
    var start = cursor;
    for (var i=start; i<Math.min(models.length, start + MODELS_PER_LOAD); i++) {
      var html = getModelDiv(models[i]);
      $('.models').append(html);
      cursor += 1;
    }
  };
  */

  function reloadTable() {
    cursor = 0;
    $('.models').html("");
    loadTable();
    $('.models-container').scrollTop(0);
  }

  function loadTable() {
    var index = cursor;
    var i=0;
    while (i < MODELS_PER_LOAD) {
      if (index >= models.length) {
        return;
      }

      // check filters
      if (models[index].show) {

        // groups bar closed or
        // groups bar open but no group is selected
        if (groupKey == "None" || groupValue == null) {
          var html = getModelDiv(models[index]);

          // no group is selected
          if (!(groupKey == "None" && groupValue == null)) {
            html.find('.model-section').addClass('groups-open');
          }

          $('.models').append(html);
          i += 1;
        } else if (models[index][groupKey] == groupValue ||
          models[index][groupKey] == null && groupValue == "null") {
          // groups bar open and group is selected
          var html = getModelDiv(models[index]);
          html.find('.model-section').addClass('groups-open');
          $('.models').append(html);
          i += 1;
        }

      }

      cursor += 1;
      index += 1;
    }
  };

  function selectInit() {
    // add standard keys
    for (var i=0; i<keys.length; i++) {
      $('.x-axis').append(new Option(keys[i], keys[i]));
      $('.z-axis').append(new Option(keys[i], keys[i]));
      $('.group-by').append(new Option(keys[i], keys[i]));
    }

    // add hyperparam keys
    for (var i=0; i<hyperparamKeys.length; i++) {
      $('.x-axis').append(new Option(hyperparamKeys[i], hyperparamKeys[i]));
      $('.z-axis').append(new Option(hyperparamKeys[i], hyperparamKeys[i]));
    }

    // add metric keys
    for (var i=0; i<metricKeys.length; i++) {
      $('.y-axis').append(new Option(metricKeys[i], metricKeys[i]));
      $('.metrics-sort').append(new Option(metricKeys[i], metricKeys[i]));
    }

    $('.x-axis').val(DEFAULT_X);
    $('.z-axis').val(DEFAULT_Z);
  };

  function vegaInit() {
    var legendWidth = 0;
    for (var i=0; i<Object.keys(modelTypes).length; i++) {
      legendWidth = Math.max(legendWidth, Object.keys(modelTypes)[i].length);
    }
    for (var i=0; i<metricKeys.length; i++) {
      legendWidth = Math.max(legendWidth, metricKeys[i].length);
    }

    var width = $('.container').width() - (180 + 4 * legendWidth);
    height = width / 2.73333;

    summarySpecs = {
      "height": height,
      "width": width,
      "signals": [
        {
          "name": "brush_start",
          "streams": [{
            "type": "@overview:mousedown",
            "expr": "eventX()",
            "scale": {"name": "xOverview", "invert": true}
          }]
        },
        {
          "name": "brush_end",
          "init": {"expr": min_id},
          "streams": [{
            "type": "@overview:mousedown, [@overview:mousedown, window:mouseup] > window:mousemove, @overview:mouseup",
            "expr": "clamp(eventX(), 0, 820)",
            "scale": {"name": "xOverview", "invert": true}
          }]
        },
        {
          "name": "min_id",
          "init": {"expr": min_id},
          "expr": "brush_start == brush_end ? " + min_id + " : min(brush_start, brush_end)"
        },
        {
          "name": "max_id",
          "init": {"expr": max_id},
          "expr": "brush_start == brush_end ? " + max_id + " : max(brush_start, brush_end)"
        }
      ],


      "data": [
        {
          "name": "table",
          "values": Object.values(models),
          "transform": [
            {"type": "filter", "test": "datum.show == true"},
            {"type": "fold", "fields": metricKeys}
          ]
        },
        {
          "name": "filtered",
          "values": Object.values(models),
          "transform": [
            {"type": "filter", "test": "datum.show == true"},
            {"type": "filter", "test": "datum.id >= min_id && datum.id <= max_id"},
            {"type": "fold", "fields": metricKeys}
          ]
        }
      ],
      "scales": [
        {
          "name": "xOverview",
          "range": "width",
          "nice": true,
          "domain": {"data": "table", "field": "id"},
          "zero": false
        },
        {
          "name": "yOverview",
          "range": [70, 0],
          "nice": true,
          "domain": {"data": "table", "field": "value"}
        },
        {
          "name": "xDetail",
          "range": "width",
          "domainMin": {"signal": "min_id"},
          "domainMax": {"signal": "max_id"},
          "zero": false
        },
        {
          "name": "yDetail",
          "range": [height,0],
          "nice": true,
          "domain": {"data": "filtered", "field": "value"}
        },
        {
          "name": "color",
          "type": "ordinal",
          "domain": {"data": "table", "field": "Type"},
          "range": "category10"
        },
        {
          "name": "shape",
          "type": "ordinal",
          "domain": {"data": "table", "field": "key"},
          "range": "shapes"
        }
      ],

      "legends": [
        {
          "fill": "color",
          "title": "Model Type",
          "offset": 20,
          "properties": {
            "symbols": {
              "fillOpacity": {"value": 0.5},
              "stroke": {"value": "transparent"}
            }
          }
        },
        {
          "shape": "shape",
          "title": "Metric Name",
          "offset": 20,
          "properties": {
            "symbols": {
              "fillOpacity": {"value": 0.5},
              "fill": "#3182bd"
            }
          }
        }
      ],

      "marks": [
        {
          "type": "group",
          "name": "detail",
          "properties": {
            "enter": {
              "height": {"value": height},
              "width": {"value": width}
            }
          },
          "axes": [
            {"type": "x", "scale": "xDetail", "title": "Model ID"},
            {"type": "y", "scale": "yDetail", "title": "Metric Values"}
          ],
          "marks": [
            {
              "type": "group",
              "properties": {
                "enter": {
                  "height": {"field": {"group": "height"}},
                  "width": {"field": {"group": "width"}},
                  "clip": {"value": false}
                }
              },
              "marks": [
                {
                  "type": "symbol",
                  "from": {
                    "data": "filtered"
                  },
                  "properties": {
                    "update": {
                      "x": {"scale": "xDetail", "field": "id"},
                      "y": {"scale": "yDetail", "field": "value"},
                      "y2": {"scale": "yDetail", "value": 0},
                      "size": {"value": 70},
                      "opacity": {"value": 0.5},
                      "fill": {"scale": "color", "field": "Type"},
                      "shape": {"scale": "shape", "field": "key"}
                    },
                    "hover": {
                      "fill": {"value": "#de2d26"},
                      "size": {"value": 150},
                      "cursor": {"value": "pointer"}
                    }
                  }
                }
              ]
            }
          ]
        },

        {
          "type": "group",
          "name": "overview",
          "properties": {
            "enter": {
              "x": {"value": 0},
              "y": {"value": height + 40},
              "height": {"value": 70},
              "width": {"value": width},
              "fill": {"value": "transparent"}
            },
            "hover": {
              "cursor": {"value": "ew-resize"}
            }
          },
          "axes": [
            {"type": "x", "scale": "xOverview"}
          ],
          "marks": [
            {
              "type": "symbol",
              "from": {
                "data": "table",
                "transform": [{"type": "formula", "field": "hidetooltip", "expr": "true"}]
              },
              "properties": {
                "update": {
                  "x": {"scale": "xOverview", "field": "id"},
                  "y": {"scale": "yOverview", "field": "value"},
                  "y2": {"scale": "yOverview", "value": 0},
                  "fill": {"value": "steelblue"}
                },
                "hover": {
                  "cursor": {"value": "ew-resize"}
                },
              }
            },
            {
              "type": "rect",
              "properties":{
                "enter":{
                  "y": {"value": 0},
                  "height": {"value":70},
                  "fill": {"value": "#333"},
                  "fillOpacity": {"value":0.2}
                },
                "hover": {
                  "cursor": {"value": "ew-resize"}
                },
                "update":{
                  "x": {"scale": "xOverview", "signal": "brush_start"},
                  "x2": {"scale": "xOverview", "signal": "brush_end"}
                }
              }
            }
          ]
        }

      ]
    };

    summarySpecsWithTimestamps = {
      "height": height,
      "width": width,
      "signals": [
        {
          "name": "brush_start",
          "streams": [{
            "type": "@overview:mousedown",
            "expr": "eventX()",
            "scale": {"name": "xOverview", "invert": true}
          }]
        },
        {
          "name": "brush_end",
          "init": {"expr": "datetime('" + min_date + "')"},
          "streams": [{
            "type": "@overview:mousedown, [@overview:mousedown, window:mouseup] > window:mousemove, @overview:mouseup",
            "expr": "clamp(eventX(), 0," + width + ")",
            "scale": {"name": "xOverview", "invert": true}
          }]
        },
        {
          "name": "min_date",
          "init": {"expr": "datetime('" + min_date + "')"},
          "expr": "time(brush_start) == time(brush_end) ? datetime('" + min_date + "') : min(brush_start, brush_end)"
        },
        {
          "name": "max_date",
          "init": {"expr": "datetime('" + max_date + "')"},
          "expr": "time(brush_start) == time(brush_end) ? datetime('" + max_date + "') : max(brush_start, brush_end)"
        }
      ],


      "data": [
        {
          "name": "table",
          "values": Object.values(models),
          "transform": [
            {"type": "filter", "test": "datum.show == true"},
            {"type": "fold", "fields": metricKeys},
            {"type": "formula", "field": "date", "expr": "datetime(datum.timestamp)"}
          ]
        },
        {
          "name": "filtered",
          "values": Object.values(models),
          "transform": [
            {"type": "filter", "test": "datum.show == true"},
            {"type": "formula", "field": "date", "expr": "datetime(datum.timestamp)"},
            {"type": "filter", "test": "datum.date >= min_date && datum.date <= max_date"},
            {"type": "fold", "fields": metricKeys}
          ]
        }
      ],
      "scales": [
        {
          "name": "xOverview",
          "range": "width",
          "type": "time",
          "domain": {"data": "table", "field": "date"},
        },
        {
          "name": "yOverview",
          "range": [70, 0],
          "nice": true,
          "domain": {"data": "table", "field": "value"}
        },
        {
          "name": "xDetail",
          "range": "width",
          "type": "time",
          "domainMin": {"signal": "min_date"},
          "domainMax": {"signal": "max_date"},
        },
        {
          "name": "yDetail",
          "range": [height,0],
          "nice": true,
          "domain": {"data": "filtered", "field": "value"}
        },
        {
          "name": "color",
          "type": "ordinal",
          "domain": {"data": "table", "field": "Type"},
          "range": "category10"
        },
        {
          "name": "shape",
          "type": "ordinal",
          "domain": {"data": "table", "field": "key"},
          "range": "shapes"
        }
      ],

      "legends": [
        {
          "fill": "color",
          "title": "Model Type",
          "offset": 20,
          "properties": {
            "symbols": {
              "fillOpacity": {"value": 0.5},
              "stroke": {"value": "transparent"}
            }
          }
        },
        {
          "shape": "shape",
          "title": "Metric Name",
          "offset": 20,
          "properties": {
            "symbols": {
              "fillOpacity": {"value": 0.5},
              "fill": "#3182bd"
            }
          }
        }
      ],

      "marks": [
        {
          "type": "group",
          "name": "detail",
          "properties": {
            "enter": {
              "height": {"value": height},
              "width": {"value": width}
            }
          },
          "axes": [
            {"type": "x", "scale": "xDetail", "title": "Time"},
            {"type": "y", "scale": "yDetail", "title": "Metric Values"}
          ],
          "marks": [
            {
              "type": "group",
              "properties": {
                "enter": {
                  "height": {"field": {"group": "height"}},
                  "width": {"field": {"group": "width"}},
                  "clip": {"value": false}
                }
              },
              "marks": [
                {
                  "type": "symbol",
                  "from": {
                    "data": "filtered"
                  },
                  "properties": {
                    "update": {
                      "x": {"scale": "xDetail", "field": "date"},
                      "y": {"scale": "yDetail", "field": "value"},
                      "y2": {"scale": "yDetail", "value": 0},
                      "size": {"value": 70},
                      "opacity": {"value": 0.5},
                      "fill": {"scale": "color", "field": "Type"},
                      "shape": {"scale": "shape", "field": "key"}
                    },
                    "hover": {
                      "fill": {"value": "#de2d26"},
                      "size": {"value": 150},
                      "cursor": {"value": "pointer"}
                    }
                  }
                }
              ]
            }
          ]
        },

        {
          "type": "group",
          "name": "overview",
          "properties": {
            "enter": {
              "x": {"value": 0},
              "y": {"value": height + 40},
              "height": {"value": 70},
              "width": {"value": width},
              "fill": {"value": "transparent"}
            },
            "hover": {
              "cursor": {"value": "ew-resize"}
            }
          },
          "axes": [
            {"type": "x", "scale": "xOverview"}
          ],
          "marks": [
            {
              "type": "symbol",
              "from": {
                "data": "table",
                "transform": [{"type": "formula", "field": "hidetooltip", "expr": "true"}]
              },
              "properties": {
                "update": {
                  "x": {"scale": "xOverview", "field": "date"},
                  "y": {"scale": "yOverview", "field": "value"},
                  "y2": {"scale": "yOverview", "value": 0},
                  "fill": {"value": "steelblue"}
                },
                "hover": {
                  "cursor": {"value": "ew-resize"}
                },
              }
            },
            {
              "type": "rect",
              "properties":{
                "enter":{
                  "y": {"value": 0},
                  "height": {"value":70},
                  "fill": {"value": "#333"},
                  "fillOpacity": {"value":0.2}
                },
                "hover": {
                  "cursor": {"value": "ew-resize"}
                },
                "update":{
                  "x": {"scale": "xOverview", "signal": "brush_start"},
                  "x2": {"scale": "xOverview", "signal": "brush_end"}
                }
              }
            }
          ]
        }

      ]
    };

    var specs = useTimestamps ? summarySpecsWithTimestamps : summarySpecs;

    vg.embed(".summary-chart", specs, function(error, result) {
      // Callback receiving the View instance and parsed Vega spec
      // result.view is the View, which resides under the '.summary-chart' element
      options = {
        showAllFields: false,
        fields: [
          {
            field: "id",
            formatType: "string"
          },
          {
            field: "Type",
            title: "type",
            formatType: "string"
          },
          {
            field: "key",
            formatType: "string"
          },
          {
            field: "value",
            title: "val",
            formatType: "number"
          }
        ],
        colorTheme: "dark"
      }
      vg.tooltip(result.view, options);
      result.view.on('click', function(event, item) {
        if (item != null) {
          if (item.datum.id != null && !item.datum.hidetooltip) {
            showModelCard(item.datum.id);
          }
        } else {
          hideModelCard();
        }
      });
    });

    vlSpec = {
      "data": {
        "values": Object.values(models)
      },
      "transform": {"filter": "datum.show == true"},
      "mark": "bar",
      "encoding": {
        "column": {
          "type": "nominal",
          "axis": {"orient": "bottom", "axisWidth": 1, "offset": -8}
        },
        "x": {
          "type": "nominal",
          "axis": null
        },
        "y": {
          "aggregate": "mean",
          "type": "numeric",
          "axis": {
          }
        },
        "color": {
          "type": "nominal",
        },
        "config": {"facet": {"cell": {"strokeWidth": 0}}}
      }
    };

    var y = metricKeys[0];
    var x = DEFAULT_X;
    var z = DEFAULT_Z;

    vlSpec.encoding.column.field  = x;
    vlSpec.encoding.y.field = y;
    vlSpec.encoding.column.axis.title = x;
    vlSpec.encoding.y.axis.title = y;
    vlSpec.encoding.x.field = z;
    vlSpec.encoding.color.field = z;

    exploreSpecs = vl.compile(vlSpec);

    vg.embed(".explore-chart", exploreSpecs, function(error, result) {
      //console.log(error);
      vg.tooltip(result.view, {showAllFields: true, colorTheme: "dark"});
    });
  };

  function vegaUpdate() {
    var x = $('.x-axis').val();
    var y = $('.y-axis').val();
    var z = $('.z-axis').val();
    var aggregate = $('.aggregate').val();

    var specs = jQuery.extend(true, {}, vlSpec);

    specs.encoding.column.field  = x;
    specs.encoding.y.field = y;
    specs.encoding.column.axis.title = x;
    specs.encoding.y.axis.title = y;
    specs.encoding.x.field = z;
    specs.encoding.color.field = z;
    specs.encoding.y.aggregate = aggregate;
    specs.encoding.column.axis.labelMaxLength = 8;

    if (aggregate == "count") {
      specs.encoding.y.axis.title = aggregate;
    }

    if (z == "None") {
      specs.encoding.color = null;
      specs.encoding.column = null;
      specs.encoding.x.field = x;
      specs.encoding.x.axis = {"title": x};
    }

    exploreSpecs = vl.compile(specs);

    vg.embed(".explore-chart", exploreSpecs, function(error, result) {
      //console.log(error);
      vg.tooltip(result.view, {showAllFields: true, colorTheme: "dark"});
    });
  };

  function getModelDiv(model) {
    var html = new EJS({url: '/ejs/model.ejs'}).render({"model": model});
    return $(html);
  };

  function dragStart(event, ui) {
    filterKey = $(ui.helper.context).data('key');
    filterVal = $(ui.helper.context).data('val');
    $('.filter-area').addClass('filter-area-highlight');
    if ($(ui.helper.context).data('json')) {
      $('.md-overlay').css({
        '-webkit-transition': 'none',
        'transition': 'none'
      })
      $('.md-overlay').fadeOut(200);
    }
    supportsRange = ui.helper.data('num');
    $(ui.helper.context).css({opacity: 0});
  };

  function dragStop(event, ui) {
    filterKey = null;
    filterVal = null;
    $(ui.helper.context).css({opacity: 1});
    if ($(ui.helper.context).data('json')) {
      $('.md-overlay').fadeIn(200, function() {
        $('.md-overlay').removeAttr("style");
      });
    }
    $('.filter-area').removeClass('filter-area-highlight');
  };

  function addFilter(key, val) {
    // check if filter with same key already exists
    if (filters[key] != null) {
      return;
    }

    if (!filters.hasOwnProperty(key)) {
      filters[key] = {
        "vals": {},
        "invert": false
      };
      filters[key].vals[val] = true;
    }

    var obj = {key: key, val: val};
    var filterDiv = $(new EJS({url: '/ejs/filter.ejs'}).render(obj));
    $('.filter-area').append(filterDiv);
  }

  function removeFilter(key) {
    delete filters[key];
  }

  function addRange(key) {
    var rangeDiv = $(new EJS({url: '/ejs/range.ejs'}).render({key: key}));
    rangeDiv.data('key', key);
    rangeDiv.data('id', rangeId);
    rangeId += 1;
    $('.filter-area').append(rangeDiv);
  }

  function updateRange(id, key, val, type) {
    ranges[id] = {
      "key": key,
      "val": val,
      "type": type
    };
  }

  function updateFilter(key, vals, invert) {
    if (filters.hasOwnProperty(key)) {
      filters[key].vals = {};
      for (var i=0; i<vals.length; i++) {
        filters[key].vals[vals[i]] = true;
      }
      filters[key].invert = invert;
    }
  }

  function removeRange(id) {
    delete ranges[id];
  }

  function filter() {
    var show = Array(models.length).fill(true);

    for (var key in filters) {
      if (filters.hasOwnProperty(key)) {
        filterByKey(key, show);
      }
    }

    for (var id in ranges) {
      if (ranges.hasOwnProperty(id)) {
        var key = ranges[id].key;
        var val = ranges[id].val;
        var type = ranges[id].type;
        filterByRange(key, val, type, show);
      }
    }

    min_id = null;
    max_id = null;
    for (var i=0; i<show.length; i++) {
      models[i].show = show[i];

      // update min and max id to set new
      // chart xscale properly
      if (models[i].show) {
        if (min_id == null || models[i].id < min_id) {
          min_id = models[i].id;
        }
        if (max_id == null || models[i].id > max_id) {
          max_id = models[i].id;
        }
      }
    }

    groupTable($('.group-by').val());
    reloadTable();
  };

  function filterByKey(key, show) {
    var vals = filters[key].vals;
    if (jQuery.isEmptyObject(vals)) {
      return;
    }
    var invert = filters[key].invert;

    for (var i=0; i<models.length; i++) {
      var field = models[i][key];
      if (invert) {
        // check to see that field is not one of specified values
        show[i] = show[i] && (vals[field] == null);
      } else {
        // check to see that field is one of specified values
        show[i] = show[i] && (vals[field] != null);
      }
    }
  };

  function filterByRange(key, val, type, show) {
    if (isNaN(parseFloat(val))) {
      return;
    } else {
      for (var i=0; i<models.length; i++) {
        var field = models[i][key];
        if (field) {
          if (type === "<") {
            show[i] = show[i] && (field < parseFloat(val));
          } else if (type === ">") {
            show[i] = show[i] && (field > parseFloat(val));
          }
        } else {
        }
      }
    }
  }

  init();
});
