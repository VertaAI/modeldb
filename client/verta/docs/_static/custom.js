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
