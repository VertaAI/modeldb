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

  var obj = {
    'key': key,
    'value': value,
    'num': num
  };

  var tooltip = $(new EJS({url: '/ejs/group-tooltip.ejs'}).render(obj));
  tooltip.css({'top': top + 'px'});
  $('.groups-bar-container').append(tooltip);
});

$(document).on('mouseleave', '.group-block', function() {
  $('.group-tooltip').remove();
});

});