$(function() {

  function init() {
    var projectId = $('body').data('id');

    // get experiment descriptions
    $.ajax({
      url: '/projects/' + projectId + '/experiments',
      type: "GET",
      success: function(response) {
        //var html = new EJS({url: '/ejs/experiments.ejs'}).render(response);
      }
    });

  };



  init();
});