import React, { Component } from 'react';
import * as d3 from 'd3';

import { errorMessage } from 'components/Charts/shared/errorMessages';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';

const width = 680;
const height = 360;
const barWidth = 20;
const margin = { top: 40, right: 35, bottom: 65, left: 85 };
let tooltip;

class BarChart extends Component {
  state = {
    bars: [],
  };

  xAxis = d3.axisBottom();
  yAxis = d3.axisLeft();
  yAxisGrid = d3.axisLeft();

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
      .domain([0, max + max * 0.07])
      .range([height - margin.bottom, margin.top]);

    const bars = data.map(d => {
      return {
        x: xScale.bandwidth() / 2 - barWidth / 2 + xScale(d.key),
        y: yScale(d.value),
        height: height - margin.bottom - yScale(d.value),
        value: d.value,
      };
    });

    return { bars, xScale, yScale };
  }

  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);

    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6).tickSize(5));
    d3.select(this.refs.yAxisGrid).call(
      this.yAxis.ticks(6).tickSize(-width + margin.right + margin.left)
    );

    d3.select('#yLabel').text(this.props.yLabel);
    d3.select('#xLabel').text(this.props.xLabel);
    if (
      this.props.yLabel !== 'data not available' &&
      this.props.yLabel !== 'metrics not available' &&
      this.props.data !== undefined &&
      this.props.data.length > 0
    ) {
      d3.select('.errorMessage_aggChartId').remove();
    }
  }

  componentDidMount() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis);

    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6));
    d3.select(this.refs.yAxisGrid).call(
      this.yAxis.ticks(6).tickSize(-width + margin.right + margin.left)
    );

    let yLabelPos = -margin.left / 2 - 20;
    d3.select(this.refs.yAxis)
      .append('text')
      .attr('id', 'yLabel')
      .attr('class', 'axisLabel')
      .attr('transform', 'rotate(-90)')
      .attr('x', -height / 2)
      .attr('y', yLabelPos)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.yLabel);

    d3.select(this.refs.xAxis)
      .append('text')
      .attr('id', 'xLabel')
      .attr('class', 'axisLabel')
      .attr('y', margin.top)
      .attr('x', width / 2 + (margin.left + yLabelPos))
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.xLabel);

    if (this.props.data === undefined || this.props.data.length === 0) {
      let errorReason =
        this.props.yLabel === 'metrics not available'
          ? this.props.yLabel
          : 'data not available';
      errorMessage(
        '.aggregationChart',
        width,
        margin.left,
        height,
        'notAvailableMsg',
        errorReason,
        '\u20E0'
      );
    }

    tooltip = d3
      .select(this.refs.tooltipRef)
      .append('div')
      .attr('opacity', 0);
  }

  mouseOver(obj, event) {
    const d = obj.obj;
    let textContent =
      "<span class='tooltip-label'>" +
      obj.label +
      " :</span> <span class='tooltip-value'>" +
      withScientificNotationOrRounded(d.value) +
      '</span>';

    tooltip
      .transition()
      .duration(100)
      .style('opacity', 1);

    let tipHtml = tooltip.attr('class', 'tooltip').html(textContent);
    tipHtml
      .style('left', event.pageX + 15 + 'px')
      .style('top', event.pageY + 15 + 'px');
  }

  mouseOut(d) {
    tooltip
      .transition()
      .duration(100)
      .style('opacity', 0);
  }

  render() {
    return (
      <div>
        <React.Fragment>
          <svg width={width} height={height} className={'aggregationChart'}>
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
            <g ref="bars">
              {this.state.bars.map(d => (
                <rect
                  x={d.x}
                  y={d.y}
                  width={barWidth}
                  height={d.height}
                  fill={'#5fe6c9'}
                  key={Math.random() + d.y}
                  onMouseOut={this.mouseOut.bind(this, d)}
                  onMouseOver={this.mouseOver.bind(this, {
                    obj: d,
                    label: this.props.yLabel,
                  })}
                />
              ))}
            </g>
          </svg>
          <div ref="tooltipRef" />
        </React.Fragment>
      </div>
    );
  }
}

export default BarChart;
