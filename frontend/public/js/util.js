(function ($) {
  $.fn.findByData = function (prop, val) {
    var $self = this;
    if (typeof val === 'undefined') {
      return $self.filter(
        function () { return typeof $(this).data(prop) !== 'undefined'; }
      );
    }
    return $self.filter(
      function () { return $(this).data(prop) == val; }
    );
  };
})(window.jQuery);