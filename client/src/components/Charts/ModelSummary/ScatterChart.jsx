/* eslint-disable */
import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import routes from 'routes';
import * as d3 from 'd3';
const width = 650;
const svg_width = 660;
const height = 400;
const margin = { top: 20, right: 25, bottom: 40, left: 85 };

function extend(base, difference, p_id) {
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
    new_d.id = p_id;
    return new_d;
  });
}

class ScatterChart extends Component {
  state = {
    marks: [],
    xScale: undefined,
    yScale: undefined,
  };

  // Establish the desired formatting options using locale.format():
  // https://github.com/d3/d3-time-format/blob/master/README.md#locale_format
  formatMillisecond = d3.timeFormat('.%L');
  formatSecond = d3.timeFormat(':%S');
  formatMinute = d3.timeFormat('%I:%M');
  formatHour = d3.timeFormat('%I');
  formatDay = d3.timeFormat('%a %d');
  formatWeek = d3.timeFormat('%b %d');
  formatMonth = d3.timeFormat('%B');
  formatYear = d3.timeFormat('%Y');

  // Define filter conditions
  multiFormat(date) {
    // console.log(date);
    // console.log(d3.timeMinute(date) < date);
    return this.formatHour;
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
  }

  //.tickFormat(d3.timeFormat('%b/%d %H:%M'))
  xAxis = d3
    .axisBottom()
    .tickFormat(d3.timeFormat('%b-%d/ %H:%M'))
    .ticks(5);
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    var { flatdata, selectedMetric } = nextProps;
    if (!flatdata) return {};
    const tempId = flatdata[0].id;

    Math.seedrandom('hello.');
    flatdata = flatdata.concat(
      extend(flatdata, 0, tempId),
      extend(flatdata, 1, tempId),
      extend(flatdata, 2, tempId),
      extend(flatdata, 3, tempId)
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
      let projectId = window.location.href.split('/')[4];
      return {
        cx: xScale(d.date),
        cy: yScale(d[selectedMetric]),
        projectId: projectId,
        modelRecordId: d.id,
      };
    });
    return { marks, xScale, yScale };
  }

  componentDidMount() {
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);

    // d3.select(this.refs.yAxisGrid).call(this.yAxis);
    // d3.select(this.refs.yAxisGrid)
    //   .append('g')
    //   .attr('class', 'grid')
    //   .call(
    //     this.yAxis
    //       .ticks(6)
    //       .tickSize(-width + margin.right + margin.left)
    //       .tickFormat('')
    //   );

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

    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);

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
  }
  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);
    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis);
    d3.select('#scatterYLabel').text(this.props.selectedMetric);

    // d3.select(this.refs.yAxisGrid)
    //   .append('g')
    //   .attr('class', 'grid')
    //   .call(
    //     this.yAxis
    //       .ticks(6)
    //       .tickSize(-width + margin.right + margin.left)
    //       .tickFormat('')
    //   );
  }

  mouseOver(d) {
    d3.select(this.refs['ref-' + d.modelRecordId])
      .transition()
      .attr('r', 10)
      .attr('opacity', 0.9);
  }
  mouseOut(d) {
    d3.select(this.refs['ref-' + d.modelRecordId])
      .transition()
      .attr('r', 7)
      .attr('opacity', 0.7);
  }

  render() {
    return (
      <svg width={svg_width} height={height} className={'summaryChart'}>
        {/* <g
          ref="yAxisGrid"
          className="axis"
          transform={`translate(${margin.left}, 0)`}
        /> */}
        <g
          ref="yAxis"
          className="axis"
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
                  modelRecordId: d.modelRecordId,
                })}
              >
                <circle
                  cx={d.cx}
                  cy={d.cy}
                  fill={'#6863ff'}
                  r={7}
                  opacity={0.7}
                  ref={'ref-' + d.modelRecordId}
                  onMouseOut={this.mouseOut.bind(this, d)}
                  onMouseOver={this.mouseOver.bind(this, d)}
                />
              </Link>
            );
          })}
        </g>
        <g ref="annotation" transform={`translate(${width + 20}, 50)`} />
      </svg>
    );
  }
}

export default ScatterChart;
