import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import routes from 'routes';
import * as d3 from 'd3';
import _ from 'lodash';
// const canvasWidth = 960;
const width = 680;
const height = 400;
const margin = { top: 40, right: 40, bottom: 60, left: 60 };

class ScatterChart extends Component {
  state = {
    marks: [],
    xScale: undefined,
    yScale: undefined,
  };

  //'%b/%d %H:%M'
  xAxis = d3
    .axisBottom()
    .tickFormat(d3.timeFormat('%b/%d %H:%M'))
    .ticks(5);
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    const { flatdata, selectedMetric } = nextProps;
    if (!flatdata) return {};

    const extent = d3.extent(flatdata, d => d.dateCreated);
    const xScale = d3
      .scaleTime()
      .domain(extent)
      .range([margin.left, width - margin.right]);

    const [min, max] = d3.extent(flatdata, d => +d[selectedMetric]);
    const yScale = d3
      .scaleLinear()
      .domain([min * 0.95, max * 1.05])
      .range([height - margin.bottom, margin.top]);

    const marks = _.map(flatdata, d => {
      if (d[selectedMetric]) {
        let dataOnChart = d;
        dataOnChart.cx = xScale(d.dateCreated);
        dataOnChart.cy = yScale(d[selectedMetric]);
        return dataOnChart;
      }
    }).filter(obj => obj !== undefined);
    return { marks, xScale, yScale };
  }

  componentDidMount() {
    // d3.select(this.refs.annotation)
    //   .append('rect')
    //   .attr('width', '200px')
    //   .attr('height', '320px')
    //   .attr('fill', '#f9f9f9')
    //   .attr('opacity', 1);

    // d3.select(this.refs.annotation)
    //   .append('text')
    //   .attr('color', '#444')
    //   .attr('x', 20)
    //   .attr('y', 20)
    //   .attr('font-size', '11px')
    //   .text('Model Metadata');

    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    d3.select(this.refs.xAxis)
      .append('text')
      .attr('class', 'axisLabel')
      .attr('y', margin.top)
      .attr('x', width / 2)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text('Time Range');

    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6));
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

    d3.select(this.refs.yAxisGrid).call(
      this.yAxis.ticks(6).tickSize(-width + margin.right + margin.left)
    );
  }
  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6).tickSize(5));
    d3.select('#scatterYLabel').text(this.props.selectedMetric);
  }

  mouseOver(d) {
    d3.select(this.refs['ref-' + d.id])
      .transition()
      .attr('r', 9)
      .attr('opacity', 0.95);
  }
  mouseOut(d) {
    d3.select(this.refs['ref-' + d.id])
      .transition()
      .attr('r', 7)
      .attr('opacity', 0.8);
  }

  render() {
    return (
      // <g width={canvasWidth} height={height}>
      <svg width={width} height={height} className={'summaryChart'}>
        <g
          ref="yAxis"
          className="axis"
          transform={`translate(${margin.left}, 0)`}
        />
        <g
          ref="yAxisGrid"
          className="grid"
          transform={`translate(${margin.left}, 0)`}
        />
        <g
          ref="xAxis"
          className="axis"
          transform={`translate(0, ${height - margin.bottom})`}
        />
        <g ref="dots" className="marks">
          {this.state.marks.map((d, i) => {
            const key = this.props.selectedMetric + i;
            return (
              <Link
                key={key}
                to={routes.modelRecord.getRedirectPath({
                  projectId: d.projectId,
                  modelRecordId: d.id,
                })}
              >
                <circle
                  cx={d.cx}
                  cy={d.cy}
                  fill={'#6863ff'}
                  opacity={0.95}
                  r={7}
                  ref={'ref-' + d.id}
                  onMouseOut={this.mouseOut.bind(this, d)}
                  onMouseOver={this.mouseOver.bind(this, d)}
                />
              </Link>
            );
          })}
        </g>
      </svg>
      //   <g ref="annotation" transform={`translate(${width + 20}, 50)`} />
      // </g>
    );
  }
}

export default ScatterChart;

// Establish the desired formatting options using locale.format():
// https://github.com/d3/d3-time-format/blob/master/README.md#locale_format
// formatMillisecond = d3.timeFormat('.%L');
// formatSecond = d3.timeFormat(':%S');
// formatMinute = d3.timeFormat('%I:%M');
// formatHour = d3.timeFormat('%I');
// formatDay = d3.timeFormat('%a %d');
// formatWeek = d3.timeFormat('%b %d');
// formatMonth = d3.timeFormat('%B');
// formatYear = d3.timeFormat('%Y');

// Define filter conditions
// multiFormat(date) {
// console.log(date);
// console.log(d3.timeMinute(date) < date);
// return this.formatHour;
// return (d3.timeSecond(date) < date
//   ? this.formatHour
//   : d3.timeMinute(date) < date
//   ? this.formatHour
//   : d3.timeHour(date) < date
//   ? this.formatMinute
//   : d3.timeDay(date) < date
//   ? this.formatHour
//   : d3.timeMonth(date) < date
//   ? d3.timeWeek(date) < date
//     ? this.formatDay
//     : this.formatWeek
//   : d3.timeYear(date) < date
//   ? this.formatMonth
//   : this.formatYear)(date);
// }
