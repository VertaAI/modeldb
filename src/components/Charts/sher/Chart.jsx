import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 650;
const height = 400;
const margin = { top: 20, right: 5, bottom: 20, left: 35 };

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
    d3.select(this.refs.xAxis2).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis2).call(this.yAxis);
  }
  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis2).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis2).call(this.yAxis);
  }

  render() {
    return (
      <svg width={width} height={height}>
        {this.state.marks.map(d => (
          <circle cx={d.cx} cy={d.cy} fill={'#6863ff'} r={7} key={Math.random() * d.cx} />
        ))}
        <g ref="xAxis2" transform={`translate(0, ${height - margin.bottom})`} />
        <g ref="yAxis2" transform={`translate(${margin.left}, 0)`} />
      </svg>
    );
  }
}

export default BarChart;
