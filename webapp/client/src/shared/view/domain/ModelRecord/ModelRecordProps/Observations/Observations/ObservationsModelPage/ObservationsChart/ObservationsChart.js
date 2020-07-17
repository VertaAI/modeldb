import React, { Component } from 'react';
import * as d3 from 'd3';

import { cssTheme } from 'shared/styles/theme';

import Legend from './Legend/Legend';
import styles from './ObservationsChart.module.css';

const width = 620,
  height = 340,
  margin = { top: 40, right: 110, bottom: 70, left: 60 },
  chartWidth = width - margin.left - margin.right,
  chartHeight = height - margin.top - margin.bottom,
  duration = 250;

const colorScale = d3
  .scaleOrdinal()
  .range([
    cssTheme.bgColor2,
    '#CBE11E',
    '#1ECBE1',
    '#E11ECB',
    '#e6ab02',
    '#a6761d',
    '#666666',
  ]);
const getColor = groupedObservationsN => colorScale(groupedObservationsN);

let lineOpacity = '0.35';
let lineOpacityHover = '0.75';
let otherLinesOpacityHover = '0.1';
let lineStroke = '1.5px';
let lineStrokeHover = '1.5px';

let circleOpacity = '0.85';
let circleRadius = 3;
let circleRadiusHover = 6;

class ObservationsChart extends Component {
  componentDidMount() {
    this.createChart();
  }

  componentDidUpdate() {
    d3.select(this._rootNode)
      .selectAll('g')
      .remove();

    this.createChart();
  }

  _setRef(componentNode) {
    this._rootNode = componentNode;
  }

  createChart() {
    const { data } = this.props;

    if (data.length > 0) {
      const svg = d3
        .select(this._rootNode)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

      const { getXValue, xAxis, xAxisLabel } = (() => {
        if (this.props.xAxisType === 'timeStamp') {
          const x = d3
            .scaleTime()
            .rangeRound([0, chartWidth])
            .domain(
              d3.extent(
                data.flatMap(obj =>
                  obj.values.map(({ timeStamp }) => timeStamp)
                )
              )
            );
          const xAxis = d3
            .axisBottom(x)
            .tickFormat(d3.timeFormat('%b/%d %H:%M'));

          const getXValue = d => x(d.timeStamp);

          return { xAxis, getXValue, xAxisLabel: 'Timestamps' };
        } else {
          const x = d3
            .scaleLinear()
            .rangeRound([0, chartWidth])
            .domain(
              d3.extent(
                data.flatMap(obj =>
                  obj.values.map(({ epochNumber }) => epochNumber)
                )
              )
            );
          const xAxis = formatTicksAsInteger(d3.axisBottom(x));

          const getXValue = d => x(d.epochNumber);

          return { xAxis, getXValue, xAxisLabel: 'Epoch' };
        }
      })();

      const y = (() => {
        const yMinVal = d3.min(
          data.flatMap(({ values }) =>
            values.map(({ value }) => value).filter(v => !Number.isNaN(v))
          )
        );
        const yMaxVal = d3.max(
          data.flatMap(({ values }) => values.map(({ value }) => value))
        );
        const yRange = yMaxVal - yMinVal;
        const domain =
          yMinVal - yRange > 0
            ? [yMinVal - yRange * 0.05, yMaxVal + yRange * 0.05]
            : [yMinVal, yMaxVal + yRange * 0.05];

        return d3
          .scaleLinear()
          .range([chartHeight, 0])
          .domain(domain);
      })();
      const yAxis = d3.axisLeft().scale(y);

      svg
        .append('g')
        .attr('class', 'x grid')
        .attr('transform', 'translate(0,' + chartHeight + ')')
        .call(
          xAxis.ticks(6).tickSizeInner(-height + margin.top + margin.bottom)
        );

      let xAxisLayer = svg
        .append('g')
        .attr('class', 'groupedXAxis')
        .attr('transform', 'translate(0,' + chartHeight + ')')
        .call(xAxis.ticks(6).tickSize(5));

      xAxisLayer
        .selectAll('text')
        .style('text-anchor', 'end')
        .attr('dx', '-.8em')
        .attr('dy', '.15em')
        .attr('transform', function(d) {
          return 'rotate(-25)';
        });

      let xAxisLableLayer = svg
        .append('g')
        .attr('class', 'groupedXAxisLable')
        .attr('transform', 'translate(0,' + (chartHeight + 35) + ')');

      xAxisLableLayer
        .append('text')
        .attr('y', 25)
        .attr('x', chartWidth / 2)
        .attr('class', 'axisLabel')
        .style('text-anchor', 'middle')
        .text(xAxisLabel);

      svg
        .append('g')
        .attr('class', 'y grid')
        .call(
          yAxis.ticks(6).tickSizeInner(-width + margin.right + margin.left)
        );

      svg
        .append('g')
        .attr('class', 'y axis')
        .call(yAxis.ticks(6).tickSize(5))
        .append('text')
        .attr('id', 'multiLineYLabel')
        .attr('class', 'axisLabel')
        .attr('transform', 'rotate(-90)')
        .attr('y', -margin.left + 10)
        .attr('x', -chartHeight / 2)
        .attr('dy', '1em')
        .style('text-anchor', 'middle')
        .style('fill', '#444')
        .text('Metrics');

      /* Add line into SVG */
      var line = d3
        .line()
        .defined(d => !isNaN(d.value))
        .x(getXValue)
        .y(d => {
          return y(d.value);
        });

      let lines = svg.append('g').attr('class', 'ml-lines');

      lines
        .selectAll('.line-group')
        .data(data)
        .enter()
        .append('g')
        .attr('class', d => {
          return 'ml-line-group';
        })
        .append('path')
        .attr('class', 'ml-line')
        .attr('d', d => line(d.values))
        .style('stroke', (d, i) => getColor(i))
        .style('opacity', lineOpacity)
        .on('mouseover', function(d) {
          d3.selectAll('.ml-line').style('opacity', otherLinesOpacityHover);
          d3.select(this)
            .transition()
            .duration(duration)
            .style('opacity', lineOpacityHover)
            .style('stroke-width', lineStrokeHover)
            .style('cursor', 'pointer');
        })
        .on('mouseout', function(d) {
          d3.selectAll('.ml-line').style('opacity', lineOpacity);
          d3.select(this)
            .transition()
            .duration(duration)
            .style('opacity', lineOpacity)
            .style('stroke-width', lineStroke)
            .style('cursor', 'none');
        });

      /* Add circles in the line */
      lines
        .selectAll('.ml-circle-group')
        .data(data)
        .enter()
        .append('g')
        .style('fill', (d, i) => getColor(i))
        .selectAll('circle')
        .data(d => d.values)
        .enter()
        .append('g')
        .attr('class', 'ml-circle')
        .on('mouseover', function(d) {
          d3.select(this)
            .style('cursor', 'pointer')
            .append('text')
            .attr('class', 'ml-text')
            .text(`Value: ${d.value}`)
            .attr('x', d => getXValue(d) + 5)
            .attr('y', d => y(d.value) - 10)
            .style('opacity', d => {
              return Number.isNaN(d.value) ? 0 : 1;
            });

          if (d.epochNumber !== undefined) {
            d3.select(this)
              .style('cursor', 'pointer')
              .append('text')
              .attr('class', 'ml-text')
              .text(`Epoch number: ${d.epochNumber}`)
              .attr('x', d => getXValue(d) + 5)
              .attr('y', d => y(d.value) - 24)
              .style('opacity', d => {
                return Number.isNaN(d.value) ? 0 : 1;
              });
          }
        })
        .on('mouseout', function(d) {
          d3.select(this)
            .style('cursor', 'none')
            .transition()
            .duration(duration)
            .selectAll('.ml-text')
            .remove();
        })
        .append('circle')
        .attr('cx', getXValue)
        .attr('cy', d => y(d.value))
        .attr('r', circleRadius)
        .style('opacity', d => {
          return Number.isNaN(d.value) ? 0 : circleOpacity;
        })
        .on('mouseover', function(d) {
          d3.select(this)
            .transition()
            .duration(duration)
            .attr('r', circleRadiusHover);
        })
        .on('mouseout', function(d) {
          d3.select(this)
            .transition()
            .duration(duration)
            .attr('r', circleRadius);
        });
    }
  }

  render() {
    const LegendView = this.props.renderLegend || Legend;

    return (
      <div className={styles.root}>
        <div>
          <React.Fragment>
            <svg
              className="multiLineObsChart"
              width={width}
              height={height}
              ref={this._setRef.bind(this)}
            />
            <div />
          </React.Fragment>
        </div>
        <LegendView
          data={this.props.data.map((x, i) => ({ ...x, color: getColor(i) }))}
        />
      </div>
    );
  }
}

// https://stackoverflow.com/a/56821215
const formatTicksAsInteger = axis => {
  return axis
    .tickValues(
      axis
        .scale()
        .ticks()
        .filter(Number.isInteger)
    )
    .tickFormat(d3.format('d'));
};

export default ObservationsChart;
