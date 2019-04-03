import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 800;
const height = 360;
const barWidth = 20;
const margin = { top: 25, right: 35, bottom: 45, left: 65 };

// d3.selection.prototype.bringElementAsTopLayer = function() {
//   return this.each(function(){
//   this.parentNode.appendChild(this);
// });
// };

// d3.selection.prototype.pushElementAsBackLayer = function() {
// return this.each(function() {
// var firstChild = this.parentNode.firstChild;
// if (firstChild) {
//    this.parentNode.insertBefore(this, firstChild);
// }
// });

class BarChart extends Component {
  state = {
    bars: [],
  };

  xAxis = d3.axisBottom();

  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    const { data } = nextProps;
    if (!data) {
      return {};
    }

    const xScale = d3
      .scaleBand()
      .domain(
        data.map(function(d) {
          return d.key;
        })
      )
      .range([margin.left, width - margin.right])
      .padding(0.1);

    const [, max] = d3.extent(data, d => d.value);
    const yScale = d3
      .scaleLinear()
      .domain([0, max + max * 0.2])
      .range([height - margin.bottom, margin.top]);

    const bars = data.map(d => {
      return {
        x: xScale.bandwidth() / 2 - barWidth / 2 + xScale(d.key),
        y: yScale(d.value),
        height: height - margin.bottom - yScale(d.value),
      };
    });

    return { bars, xScale, yScale };
  }

  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);

    d3.select('#yLabel').text(this.props.yLabel);
    d3.select('#xLabel').text(this.props.xLabel);
  }

  componentDidMount() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6));

    d3.select(this.refs.yAxis)
      .append('text')
      .attr('id', 'yLabel')
      .attr('transform', 'rotate(-90)')
      .attr('x', -height / 2)
      .attr('y', -margin.left / 2 - 10)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.yLabel);

    d3.select(this.refs.xAxis)
      .append('text')
      .attr('id', 'xLabel')
      .attr('y', margin.top + 10)
      .attr('x', width / 2)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.xLabel);

    d3.select(this.refs.yAxis)
      .append('g')
      .attr('class', 'grid')
      .call(
        this.yAxis
          .ticks(6)
          .tickSize(-width + margin.right + margin.left)
          .tickFormat('')
      );
  }

  render() {
    return (
      <svg width={width} height={height} className={'expChart'}>
        <g ref="xAxis" transform={`translate(0, ${height - margin.bottom})`} />
        <g ref="yAxis" transform={`translate(${margin.left}, 0)`} />
        <g ref="bars">
          {this.state.bars.map(d => (
            <rect
              x={d.x}
              y={d.y}
              width={barWidth}
              height={d.height}
              fill={'#5fe6c9'}
              key={Math.random() + d.y}
            />
          ))}
        </g>
      </svg>
    );
  }
}

export default BarChart;
