$(function() {
  var container;
  var network;
  var nodes = new vis.DataSet([]);
  var edges = new vis.DataSet([]);

  var vis_data = {
    nodes: nodes,
    edges: edges
  };

  var options = {
    manipulation: {
        enabled: false
    },
    edges: {
        arrows: "to"
    },
    height: '400px',
    layout: {
        hierarchical: {
            enabled: true,
            direction: 'LR',
            levelSeparation: 400,
            sortMethod: "directed"
        }
    },
    groups: {
    }
  };

  function init() {
    // create a network
    container = document.getElementById('network');
    var modelId = $('body').data('id');

    // initialize your network!
    network = new vis.Network(container, vis_data, options);

    // clear data
    nodes.clear();
    edges.clear();

    // initialize dictionaries
    var edges_dict = {};
    var root_path = $('.body').context.body.dataset.root;

    // fetch ancestry
    $.ajax({
      url: root_path + '/models/ancestry/' + modelId,
      type: "GET",
      success: function(response) {
        if (response.transformEvents.length === 0) {
          $('#network').hide();
          return;
        }

        // add start node
        nodes.add({
          id: "start",
          label: "Start",
          color: "rgb(255,168,7)"
        });

        // add transformer nodes
        for (var i=0; i<response.transformEvents.length; i++) {
          var obj = response.transformEvents[i];

          if (nodes.get(obj.transformer.id) === null) {
            nodes.add({
              id: obj.transformer.id,
              label: 'transformer id: ' + obj.transformer.id + '\n' +  obj.transformer.transformerType
            });
          }

          if (!edges_dict.hasOwnProperty(obj.oldDataFrame.id)) {
            edges_dict[obj.oldDataFrame.id] = {};
          }

          if (!edges_dict.hasOwnProperty(obj.newDataFrame.id)) {
            edges_dict[obj.newDataFrame.id] = {};
          }

          edges_dict[obj.oldDataFrame.id].to = obj.transformer.id;
          edges_dict[obj.oldDataFrame.id].label = "dataframe id: " + obj.oldDataFrame.id;
          edges_dict[obj.newDataFrame.id].from = obj.transformer.id;
          edges_dict[obj.newDataFrame.id].label = "dataframe id: " + obj.newDataFrame.id;
        }

        // add fitevent
        nodes.add({
          id: "fitevent" + response.modelId,
          label: response.fitEvent.spec.transformerType,
          color: "#7BE141"
        });

        edges_dict[response.fitEvent.df.id].to = "fitevent" + response.modelId;

        for (var key in edges_dict) {
          if (edges_dict.hasOwnProperty(key)) {
            if (edges_dict[key].to != null && edges_dict[key].from != null) {
              edges.add(edges_dict[key]);
            } else {
              // manually connect to start
              if (edges_dict[key].from == null) {
                edges_dict[key].from = "start";
                edges.add(edges_dict[key]);
              }
            }
          }
        }

        network.fit();
        network.moveTo({
          scale: 0.75
        })
      }
    });

  };

  init();
});
