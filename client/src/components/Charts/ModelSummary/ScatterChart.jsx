/* eslint-disable */
import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import routes from 'routes';
import * as d3 from 'd3';
const width = 650;
const svg_width = 660;
const height = 400;
const margin = { top: 20, right: 25, bottom: 40, left: 85 };

class ScatterChart extends Component {
  state = {
    marks: [],
    // hover: false,
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
  xAxis = d3.axisBottom().tickFormat(this.multiFormat);
  // .ticks(5);
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    const { flatdata, selectedMetric } = nextProps;
    if (!flatdata) return {};

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

    // d3.select(this.refs.dots)
    //   .selectAll('circle')
    //   .on('mouseover', () => {
    //     console.log(d3.select(this));
    //     // d3.select(this).style('r', 9);
    //   })
    //   .on('mouseout', () => {});

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

  mouseOver() {
    console.log(this);
    // console.log(d3.select(this.refs[ref]));
    // console.log(document.getElementById('id-' + id));
  }
  mouseOut() {
    // console.log(d3.select(this.refs[ref]));
  }

  // hoverOn() {
  //   this.setState({ hover: true });
  // }
  // hoverOff() {
  //   this.setState({ hover: false });
  // }

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
                  // r={this.state.hover ? 9 : 17}
                  r={7}
                  // ref={'ref-' + key}
                  // onMouseEnter={this.hoverOn.bind(this, d)}
                  // onMouseLeave={this.hoverOff.bind(this, d)}
                  // onMouseOut={this.mouseOut.bind(this, d)}
                  // onMouseOver={this.mouseOver.bind(this, d)}
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
