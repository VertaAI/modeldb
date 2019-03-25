import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 600;
const height = 300;
const barWidth = 20;
const margin = { top: 20, right: 5, bottom: 20, left: 55 };

class BarChart extends Component {
  state = {
    bars: []
  };

  xAxis = d3.axisBottom();
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    const { data, xLabel, yLabel } = nextProps;
    if (!data) return {};

    const xScale = d3
      .scaleBand()
      .domain(
        data.map(function(d) {
          return d.key;
        })
      )
      .range([margin.left, width - margin.right])
      .padding(0.1);

    const [min, max] = d3.extent(data, d => d.value);
    const yScale = d3
      .scaleLinear()
      .domain([0, max + max * 0.2])
      .range([height - margin.bottom, margin.top]);

    const bars = data.map(d => {
      return {
        x: xScale.bandwidth() / 2 - barWidth / 2 + xScale(d.key),
        y: yScale(d.value),
        height: height - margin.bottom - yScale(d.value)
      };
    });

    return { bars, xScale, yScale };
  }

  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);
  }

  componentDidMount() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);
    d3.select('.expChart')
      .append('text')
      .attr('transform', 'rotate(-90)')
      .attr('y', margin.left)
      .attr('x', height / 2)
      .style('text-anchor', 'middle')
      .text(this.props.yLabel);

    d3.select('.expChart')
      .append('text')
      .attr('y', height + margin.top)
      .attr('x', width / 2)
      .style('text-anchor', 'middle')
      .text(this.props.xLabel);
  }

  render() {
    return (
      <svg width={width} height={height} className={'expChart'}>
        {this.state.bars.map(d => (
          <rect x={d.x} y={d.y} width={barWidth} height={d.height} fill={'#5fe6c9'} key={Math.random() + d.y} />
        ))}
        <g ref="xAxis" transform={`translate(0, ${height - margin.bottom})`} />
        <g ref="yAxis" transform={`translate(${margin.left}, 0)`} />
      </svg>
    );
  }
}

export default BarChart;
