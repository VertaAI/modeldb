$(function() {
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
        $('#modal-1').addClass('md-show');
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
    var data = [];
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
