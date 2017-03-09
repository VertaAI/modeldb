$(function() {
  $(document).on('click', '.disable-click', function(event) {
    event.preventDefault();
    event.stopPropagation();
  });

  $(document).on('mouseenter', '.tooltip-trigger', function(event) {
    var elt = $(event.target);

    if (elt.hasClass('nkv') || elt.hasClass('show-on-overflow')) {
      if (event.target.scrollWidth <= elt.innerWidth()) {
        return;
      }
    }

    var keys = [];
    var values = [];

    if (elt.data('key') != null) {
      keys.push(elt.data('key'));
    }
    keys.push.apply(keys, elt.data('keys'));

    if (elt.data('val') != null) {
      values.push(elt.data('val'));
    }
    values.push.apply(values, elt.data('values'));

    var obj = {
      'keys': keys,
      'values': values
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