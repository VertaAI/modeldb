$(function() {

  $(document).on('mouseenter', '.tooltip-trigger', function(event) {
    var elt = $(event.target);
    var obj = {
      'keys': elt.data('keys'),
      'values': elt.data('values')
    }

    var tooltip = $(new EJS({url: '/ejs/tooltip.ejs'}).render(obj));

    $('body').append(tooltip);

    var top = elt.offset().top - tooltip.outerHeight() - 10;
    var offset = elt.width()/2 - tooltip.outerWidth()/2;
    var left = Math.max(0, elt.offset().left + offset);
    tooltip.css({
      'top': top + 'px',
      'left': left + 'px'
    })
  });

  $(document).on('mouseleave', '.tooltip-trigger', function() {
    $('.tooltip').remove();
  });
});