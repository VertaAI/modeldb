import * as d3 from 'd3';

export const errorMessage = (
  node: string,
  width: number,
  marginLeft: number,
  height: number,
  className: string,
  text: string,
  unicode: string
) => {
  const idName =
    node === '.aggregationChart'
      ? 'aggChartId'
      : '.summaryChart'
      ? 'summaryChartId'
      : '';

  const error_message_layer = d3
    .select(node)
    .append('g')
    .attr('class', 'errorMessage_' + idName);
  error_message_layer
    .append('text')
    .attr('class', className + '_icon')
    .text(unicode)
    .attr('transform', `translate(${width / 2 - marginLeft}, ${height / 2})`);

  error_message_layer
    .append('text')
    .attr(
      'transform',
      `translate(${width / 2 - marginLeft + 30}, ${height / 2})`
    )
    .attr('class', className)
    .text(text);
};

export const errorMessageOnLayer = (
  node: string,
  width: number,
  marginLeft: number,
  height: number,
  className: string,
  text: string,
  unicode: string
) => {
  const error_message_layer = d3
    .select(node)
    .append('g')
    .attr('class', 'errorMessage');
  error_message_layer
    .append('text')
    .attr('class', className + '_icon')
    .text(unicode)
    .attr(
      'transform',
      `translate(${width / 2 - 2 * marginLeft}, ${height / 2})`
    );

  error_message_layer
    .append('text')
    .attr(
      'transform',
      `translate(${width / 2 - 2 * marginLeft + 30}, ${height / 2})`
    )
    .attr('class', className)
    .text(text);
};
