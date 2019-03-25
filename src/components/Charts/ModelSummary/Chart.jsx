import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 650;
const height = 400;
const margin = { top: 20, right: 5, bottom: 40, left: 55 };

class BarChart extends Component {
  state = {
    marks: []
  };

  xAxis = d3.axisBottom().tickFormat(d3.timeFormat('%b'));
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    const { flatdata, selectedMetric } = nextProps;
    if (!flatdata) return {};

    const extent = d3.extent(flatdata, d => d.date);
    const xScale = d3
      .scaleTime()
      .domain(extent)
      .range([margin.left, width - margin.right]);

    const [min, max] = d3.extent(flatdata, d => d[selectedMetric]);
    const yScale = d3
      .scaleLinear()
      .domain([min, max])
      .range([height - margin.bottom, margin.top]);

    const marks = flatdata.map(d => {
      return {
        cx: xScale(d.date),
        cy: yScale(d[selectedMetric])
      };
    });
    return { marks, xScale, yScale };
  }

  componentDidMount() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);

    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);

    d3.select(this.refs.yAxis)
      .append('text')
      .attr('id', 'scatterYLabel')
      .attr('transform', 'rotate(-90)')
      .attr('y', -margin.left)
      .attr('x', -height / 2)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.selectedMetric);

    d3.select(this.refs.xAxis)
      .append('text')
      .attr('y', margin.top + 10)
      .attr('x', width / 2)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text('Time Range');
  }
  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);

    d3.select('#scatterYLabel').text(this.props.selectedMetric);
  }

  render() {
    return (
      <svg width={width} height={height}>
        {this.state.marks.map(d => (
          <circle cx={d.cx} cy={d.cy} fill={'#6863ff'} r={7} key={Math.random() * d.cx} />
        ))}
        <g ref="xAxis" transform={`translate(0, ${height - margin.bottom})`} />
        <g ref="yAxis" transform={`translate(${margin.left}, 0)`} />
      </svg>
    );
  }
}

export default BarChart;
