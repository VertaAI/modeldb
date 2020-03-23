window.onload = function() {
  this.deleteArrows();
};

function deleteArrows() {
  const links = document.querySelectorAll("#rellinks li");
  links.forEach(link =>
    link.childNodes.forEach(n => {
      if (
        n.nodeValue &&
        (n.nodeValue.includes("→") || n.nodeValue.includes("←"))
      ) {
        n.remove();
      }
    })
  );
}

// open external links in new window
$(document).ready(function() {
  $("a[href^='http']").attr('target','_blank');
});
