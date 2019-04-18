import * as d3 from 'd3';

export const errorMessage = (
  node: any,
  w: number,
  mLeft: number,
  h: number,
  className: string,
  text: string,
  unicode: string
) => {
  d3.select(node)
    .append('text')
    .attr('transform', `translate(${w / 2 - mLeft}, ${h / 2})`)
    .attr('class', className)
    .attr('style', 'font-family:FontAwesome;')
    .text(function(d) {
      return unicode;
    });

  d3.select(node)
    .append('text')
    .attr('transform', `translate(${w / 2 - mLeft + 30}, ${h / 2})`)
    .attr('class', className)
    .text(text);
};
