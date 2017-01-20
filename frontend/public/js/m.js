var models = {};
var values = [];
var spec;
var embedSpec;
var numModels = 0;
var min_id = null;
var max_id = null;

$(function() {

  function init() {
    var id = $('body').data('id');
    console.log("project id: " + id);
    fetchData(id);
  };

  function fetchData(projectId) {
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
        numModels = response.length;
        var text = numModels + (numModels == 1 ? " model" : " models");
        $('.project-num-models').html(text);

        for (var i=0; i<response.length; i++) {
          var model = response[i];
          var id = model.id;

          if (min_id == null || id < min_id) {
            min_id = id;
          }

          if (max_id == null || id > max_id) {
            max_id = id;
          }

          // id
          models[id] = {"Model ID": id};
          models[id]["Experiment Run ID"] = model.experimentRunId;
          models[id]["Experiment ID"] = model.experimentId;
          models[id]["Project ID"] = model.projectId;

          // dataframe
          models[id]["DataFrame ID"] = model.trainingDataFrame.id;
          models[id]["DF numRows"] = model.trainingDataFrame.numRows;
          models[id]["DF Tag"] = model.trainingDataFrame.tag;
          models[id]["DF Filepath"] = model.trainingDataFrame.filepath;
          var metadata = model.trainingDataFrame.metadata;
          for (var j=0; j<metadata.length; j++) {
            models[id][metadata[j].key] = metadata[j].value;
          }

          // specifications
          models[id]["Specification ID"] = model.specification.id;
          models[id]["Type"] = model.specification.transformerType;
          models[id]["Spec Tag"] = model.specification.tag;
          models[id]["Problem Type"] = model.problemType;
          var hyperparameters = model.specification.hyperparameters;
          for (var j=0; j<hyperparameters.length; j++) {
            models[id][hyperparameters[j].name] = hyperparameters[j].value;
          }

          // metrics
          var metrics = model.metrics;
          for (var j=0; j<metrics.length; j++) {
            models[id][metrics[j].key] = metrics[j].val;
            values.push({
              "id": id,
              "type": model.specification.transformerType,
              "key": metrics[j].key,
              "val": metrics[j].val
            });
          }

          // misc
          models[id]["Code SHA"] = model.sha;
          models[id]["Filepath"] = model.filepath;
        }

        vegaInit();
        $('.loader').hide();
      }
    });
  }

  function vegaInit() {
    specs = {
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
          "values": values
        },
        {
          "name": "filtered",
          "values": values,
          "transform": [
            {"type": "filter", "test": "datum.id >= min_id && datum.id <= max_id"},
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
          "domain": {"data": "table", "field": "val"}
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
          "domain": {"data": "filtered", "field": "val"}
        },
        {
          "name": "color",
          "type": "ordinal",
          "domain": {"data": "table", "field": "type"},
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
                      "y": {"scale": "yDetail", "field": "val"},
                      "y2": {"scale": "yDetail", "value": 0},
                      "size": {"value": 70},
                      "opacity": {"value": 0.5},
                      "fill": {"scale": "color", "field": "type"},
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
                  "y": {"scale": "yOverview", "field": "val"},
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
    console.log(specs);

    vg.embed(".summary-chart", specs, function(error, result) {
      // Callback receiving the View instance and parsed Vega spec
      // result.view is the View, which resides under the '#Barchart' element
      console.log(result);
      console.log(error);
      options = {
        showAllFields: true,
        colorTheme: "dark"
      }
      vg.tooltip(result.view, options);
      result.view.on('click', function(event, item) {
        console.log(event);
        console.log(item);
      });
    });
  }

  init();
});