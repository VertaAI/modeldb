import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 650;
const svg_width = 660;
const height = 400;
const margin = { top: 20, right: 25, bottom: 40, left: 85 };

function extend(base, difference) {
  return base.map(d => {
    const new_date = new Date().setTime(
      d.date.getTime() -
        (Math.random() * 2 - 1) * 24 * 60 * 60 * 1000 -
        difference * 24 * 60 * 60 * 1000
    );
    var new_d = {};
    for (var key in d) {
      new_d[key] = d[key] * Math.pow(Math.random() * 0.2 + 0.8, difference + 1);
    }
    new_d.date = new_date;
    return new_d;
  });
}

class BarChart extends Component {
  state = {
    marks: [],
    xScale: undefined,
    yScale: undefined,
  };

  //.tickFormat(d3.timeFormat('%b/%d %H:%M'))
  xAxis = d3
    .axisBottom()
    .tickFormat(d3.timeFormat('%b/%d %H:%M'))
    .ticks(5);
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    var { flatdata, selectedMetric } = nextProps;
    if (!flatdata) return {};
    Math.seedrandom('hello.');
    flatdata = flatdata.concat(
      extend(flatdata, 0),
      extend(flatdata, 1),
      extend(flatdata, 2),
      extend(flatdata, 3)
    );

    const extent = d3.extent(flatdata, d => d.date);
    const xScale = d3
      .scaleTime()
      .domain(extent)
      .range([margin.left, width - margin.right]);

    const [, max] = d3.extent(flatdata, d => d[selectedMetric]);
    const yScale = d3
      .scaleLinear()
      .domain([0, max * 1.25])
      .range([height - margin.bottom, margin.top]);

    const marks = flatdata.map(d => {
      return {
        cx: xScale(d.date),
        cy: yScale(d[selectedMetric]),
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
      .attr('class', 'axisLabel')
      .attr('transform', 'rotate(-90)')
      .attr('y', -margin.left + 10)
      .attr('x', -height / 2)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.selectedMetric);

    d3.select(this.refs.xAxis)
      .append('text')
      .attr('class', 'axisLabel')
      .attr('y', margin.top + 10)
      .attr('x', width / 2)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text('Time Range');

    // d3.select(this.refs.annotation)
    //   .append('rect')
    //   .attr('width', '160px')
    //   .attr('height', '300px')
    //   .attr('fill', '#f9f9f9');
    // .attr('opacity', 0);

    // d3.select(this.refs.annotation)
    //   .append('text')
    //   .attr('fill', '#444')
    //   .attr('x', 20)
    //   .attr('y', 20)
    //   .attr('font-size', '11px')
    //   .text('Model Metadata');

    // d3.selectAll('circle')
    //   .on('mouseover', d => {
    //     d3.select(this.refs.annotation).attr('opacity', 1);
    //     console.log(d);
    //   })
    //   .on('mouseout', d => {
    //     d3.select(this.refs.annotation).attr('opacity', 0);
    //   });

    // d3.select(this.refs.yAxis)
    //   .append('g')
    //   .attr('class', 'grid')
    //   .call(
    //     this.yAxis
    //       .ticks(6)
    //       .tickSize(-width + margin.right + margin.left)
    //       .tickFormat('')
    //   );
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
      <svg width={svg_width} height={height} className={'summaryChart'}>
        <g
          ref="xAxis"
          className="axis"
          transform={`translate(0, ${height - margin.bottom})`}
        />
        <g
          ref="yAxis"
          className="axis"
          transform={`translate(${margin.left}, 0)`}
        />
        {this.state.marks.map((d, i) => (
          <circle
            cx={d.cx}
            cy={d.cy}
            fill={'#6863ff'}
            r={7}
            key={this.props.selectedMetric + i}
          />
        ))}
        <g ref="annotation" transform={`translate(${width + 20}, 50)`} />
      </svg>
    );
  }
}

export default BarChart;
