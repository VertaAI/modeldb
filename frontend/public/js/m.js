const MODELS_PER_LOAD = 10;
const DEFAULT_X = "Type";
const DEFAULT_Z = "Experiment Run ID";

var models = [];
var summarySpecs;
var exploreSpecs;
var vlSpec;
var min_id = null;
var max_id = null;
var hyperparamKeys = {};
var metricKeys = {};
var keys = ["Experiment Run ID", "Experiment ID", "Project ID",
            "DataFrame ID", "DF numRows", "DF Tag", "DF Filepath",
            "Type", "Spec Tag", "Problem Type",
            "Code SHA", "Filepath"];
var cursor = 0;
var groups = {};
var group = null;
var groupTable;
var category10 = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
                  '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'];


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
      if (this.scrollHeight - $(this).scrollTop() == $(this).outerHeight()) {
        // reached bottom of table, so load more
        loadTable();
      }
    });

    $(document).on('click', '.model-heading.pointer', function() {
      var el = $(this);
      var sorted = el.hasClass('sorted');
      var order = el.data('order');
      var key = el.data('key');
      if (!sorted) {
        // default to ascending
        $('.sorted').removeClass('sorted');
        $('.down').removeClass('down');
        $('.up').removeClass('up');
        el.addClass('sorted');
        el.data('order', 'down');
        el.addClass('down');
        order = 'down';
      } else {
        // flip sorting order
        el.removeClass(el.data('order'));
        if (order == 'up') {
          order = 'down';
        } else {
          order = 'up';
        }
        el.data('order', order);
        el.addClass(order);
      }

      sortTable(key, order);
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

    $('.loader').show();
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
          obj["metadata"] = metadata;
          for (var j=0; j<metadata.length; j++) {
            obj[metadata[j].key] = metadata[j].value;
          }

          // specifications
          obj["Specification ID"] = model.specification.id;
          obj["Type"] = model.specification.transformerType;
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

          models.push(obj);
        }
        metricKeys = Object.values(metricKeys);
        hyperparamKeys = Object.values(hyperparamKeys);
        selectInit();
        vegaInit();
        loadTable();
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

  groupTable = function(key) {
    var container = $('.groups-bar-container');
    var bar = $('.groups-bar')[0];
    var $bar = $(bar);

    // reset values
    group = key;
    groups = {};
    $bar.html("");
    $bar.removeClass('overflow');
    $('.overflow-caret').remove();

    if (key == "None") {
      // remove groups
      $('.groups-open').removeClass('groups-open');
      $('.groups-bar').hide();
      return;
    } else {
      // get group counts
      for (var i=0; i<models.length; i++) {
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
  };

  function sortTable(key, order) {
    console.log(order);
    if (key == "id") {
      models.sort(function(a, b) {
        var x = (order == "down") ? 1 : -1;
        if (a.id < b.id)
          return -1 * x;
        if (a.id > b.id)
          return 1 * x;
        return 0;
      });
    } else if (key =="df") {
       models.sort(function(a, b) {
        var x = (order == "down") ? 1 : -1;
        if (a["DataFrame ID"] < b["DataFrame ID"])
          return -1 * x;
        if (a["DataFrame ID"] > b["DataFrame ID"])
          return 1 * x;
        return 0;
      });
    } {
      // TODO: write sorting function for metrics
    }
    cursor = 0;
    $('.models').html("");
    loadTable();
  };

  function loadTable() {
    var start = cursor;
    for (var i=start; i<Math.min(models.length, start + MODELS_PER_LOAD); i++) {
      var html = getModelDiv(models[i]);
      $('.models').append(html);
      cursor += 1;
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
    }

    $('.x-axis').val(DEFAULT_X);
    $('.z-axis').val(DEFAULT_Z);
  };

  function vegaInit() {
    summarySpecs = {
      "height": 300,
      "width": 820,

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
            {"type": "fold", "fields": metricKeys}
          ]
        },
        {
          "name": "filtered",
          "values": Object.values(models),
          "transform": [
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
          "range": [300,0],
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
              "height": {"value": 300},
              "width": {"value": 820}
            }
          },
          "axes": [
            {"type": "x", "scale": "xDetail", "title": "Model ID"},
            {"type": "y", "scale": "yDetail", "title": "Metrics"}
          ],
          "marks": [
            {
              "type": "group",
              "properties": {
                "enter": {
                  "height": {"field": {"group": "height"}},
                  "width": {"field": {"group": "width"}},
                  "clip": {"value": true}
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
              "y": {"value": 340},
              "height": {"value": 70},
              "width": {"value": 820},
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

    /*
    exploreSpecs = {
      "height": 200,
      "width": 600,

      "data": [
        {
          "name": "table",
          "values": models
        }
      ],

      "scales": [
        {
          "name": "x",
          "range": "width",
          "type": "ordinal",
          "domain": {"data": "table", "field": "Experiment Run ID"}
        },
        {
          "name": "y",
          "range": "height",
          "type": "linear",
          "nice": true,
          "domain": {"data": "table", "field": "accuracy"}
        }
      ],
      "axes": [
        {"type": "x", "scale": "x"},
        {"type": "y", "scale": "y"}
      ],
      "marks": [
        {
          "type": "rect",
          "from": {
            "data": "table",
          },
          "properties": {
            "enter": {
              "x": {"scale": "x", "field": "Experiment Run ID"},
              "width": {"scale": "x", "band": true, "offset": -10},
              "y": {"scale": "y", "field": "accuracy"},
              "y2": {"scale": "y", "value": 0},
              "fill": {"value": "steelblue"}
            },
          }
        }
      ]
    };
    */

    vg.embed(".summary-chart", summarySpecs, function(error, result) {
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
      console.log(error);
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
      console.log(error);
      vg.tooltip(result.view, {showAllFields: true, colorTheme: "dark"});
    });
  };

  function getModelDiv(model) {
    var html = new EJS({url: '/ejs/model.ejs'}).render({"models": [model]});
    return $(html);
  };

  init();
});