import React, { Component } from 'react';
import * as d3 from 'd3';

import { errorMessageOnLayer } from 'components/Charts/shared/errorMessages';

import styles from '../GroupedChartManager.module.css';

const width = 680,
  height = 380,
  margin = { top: 30, right: 35, bottom: 100, left: 75 },
  chart_width = width - margin.left - margin.right,
  chart_height = height - margin.top - margin.bottom,
  duration = 250;

let lineOpacity = '0.35';
let lineOpacityHover = '0.75';
let otherLinesOpacityHover = '0.1';
let lineStroke = '1.5px';
let lineStrokeHover = '1.5px';

let circleOpacity = '0.85';
let circleRadius = 3;
let circleRadiusHover = 6;

let svg, legendG;

class MultiLineChart extends Component {
  state = {};
  componentDidMount() {
    const {
      data,
      selectedHyperparams,
      selectedYAxisType,
      colorScale,
    } = this.props;
    if (data.filter(obj => obj.values.length > 0).length > 0) {
      svg = d3
        .select(this._rootNode)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

      var x = d3
          .scaleBand()
          .rangeRound([0, chart_width])
          .padding(0.1),
        y;

      if (selectedYAxisType === 'linear') {
        y = d3.scaleLinear().range([chart_height, 0]);
      } else {
        y = d3.scaleLog().range([chart_height, 0]);
      }

      var xAxis = d3.axisBottom().scale(x);
      var yAxis = d3
        .axisLeft()
        .scale(y)
        .ticks(6);

      if (data && data !== undefined && data !== null && data.length > 0) {
        var indexKey = data[0].values.map(function(d) {
          return d.indexKey;
        });

        x.domain(indexKey);
        const yMinVal = d3.min(data, function(obj) {
          return d3.min(obj.values, function(d) {
            if (!Number.isNaN(d.value)) {
              return d.value;
            }
          });
        });

        const yMaxVal = d3.max(data, function(obj) {
          return d3.max(obj.values, function(d) {
            return d.value;
          });
        });

        const yRange = yMaxVal - yMinVal;
        if (yMinVal - yRange > 0) {
          y.domain([yMinVal - yRange * 0.05, yMaxVal + yRange * 0.05]);
        } else {
          y.domain([yMinVal, yMaxVal + yRange * 0.05]);
        }

        svg
          .append('g')
          .attr('class', 'x grid')
          .attr('transform', 'translate(0,' + chart_height + ')')
          .call(
            xAxis.ticks(6).tickSizeInner(-height + margin.top + margin.bottom)
          );

        let xAxisLayer = svg
          .append('g')
          .attr('class', 'groupedXAxis')
          .attr('transform', 'translate(0,' + chart_height + ')')
          .call(xAxis.ticks(6).tickSize(6));

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
          .attr('transform', 'translate(0,' + (chart_height + 35) + ')');

        xAxisLableLayer
          .append('text')
          .attr('class', 'groupedChartXAxisLabel')
          .attr('y', 40)
          .attr('x', chart_width / 2)
          .style('text-anchor', 'middle')
          .text('Grouped By - (' + selectedHyperparams.join(', ') + ')');

        xAxisLableLayer
          .append('text')
          .attr('y', 60)
          .attr('x', chart_width / 2)
          .attr('class', 'axisLabel')
          .style('text-anchor', 'middle')
          .text('Hyperparameters');

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
          .attr('x', -chart_height / 2)
          .attr('dy', '1em')
          .style('text-anchor', 'middle')
          .style('fill', '#444')
          .text('Metrics');

        /* Add line into SVG */
        var line = d3
          .line()
          .defined(d => !isNaN(d.value))
          .x(d => x(d.indexKey) + x.bandwidth() / 2)
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
          .style('stroke', (d, i) => colorScale(i))
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
          .style('fill', (d, i) => colorScale(i))
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
              .text(`${d.value}`)
              .attr('x', d => x(d.indexKey) + 5)
              .attr('y', d => y(d.value) - 10)
              .style('opacity', d => {
                return Number.isNaN(d.value) ? 0 : 1;
              });
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
          .attr('cx', d => x(d.indexKey) + x.bandwidth() / 2)
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

        //Legend
        legendG = d3
          .select(this._legendNode)
          .append('g')
          .attr('transform', 'translate(120,10)')
          .selectAll('.legend')
          .data(
            data.map(function(d) {
              return d.lineIndex;
            })
          )
          .enter()
          .append('g')
          .attr('class', 'ml-legend')
          .attr('transform', function(d, i) {
            return 'translate(0,' + i * 16 + ')';
          })
          .style('opacity', '0');

        legendG
          .append('rect')
          .attr('x', -12)
          .attr('width', 12)
          .attr('height', 10)
          .attr('rx', 1)
          .attr('ry', 1)
          .style('fill', function(d, i) {
            return colorScale(i);
          });

        legendG
          .append('text')
          .attr('x', -24)
          .attr('y', 5)
          .attr('dy', '.35em')
          .style('text-anchor', 'end')
          .style('font-size', '11px')
          .text(function(d) {
            return d;
          });

        legendG
          .transition()
          .duration(200)
          .style('opacity', '1');
      }
    } else {
      errorMessageOnLayer(
        '.groupedLineChart',
        width,
        margin.left,
        height,
        'notAvailableMsg',
        'data not present for this config',
        '\u20E0'
      );
    }
  }

  componentDidUpdate(prevProps, prevState) {
    const {
      data,
      selectedHyperparams,
      selectedYAxisType,
      colorScale,
    } = this.props;
    d3.select(this._rootNode)
      .selectAll('g')
      .remove();
    d3.select(this._legendNode)
      .selectAll('g')
      .remove();

    if (data.filter(obj => obj.values.length > 0).length > 0) {
      svg = d3
        .select(this._rootNode)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

      var x = d3
          .scaleBand()
          .rangeRound([0, chart_width])
          .padding(0.1),
        y;

      if (selectedYAxisType === 'linear') {
        y = d3.scaleLinear().range([chart_height, 0]);
      } else {
        y = d3.scaleLog().range([chart_height, 0]);
      }

      var xAxis = d3.axisBottom().scale(x);
      var yAxis = d3.axisLeft().scale(y);

      if (data && data !== undefined && data !== null && data.length > 0) {
        var indexKey = data[0].values.map(function(d) {
          return d.indexKey;
        });

        x.domain(indexKey);
        const yMinVal = d3.min(data, function(obj) {
          return d3.min(obj.values, function(d) {
            if (!Number.isNaN(d.value)) {
              return d.value;
            }
          });
        });

        const yMaxVal = d3.max(data, function(obj) {
          return d3.max(obj.values, function(d) {
            return d.value;
          });
        });

        const yRange = yMaxVal - yMinVal;
        if (yMinVal - yRange > 0) {
          y.domain([yMinVal - yRange * 0.05, yMaxVal + yRange * 0.05]);
        } else {
          y.domain([yMinVal, yMaxVal + yRange * 0.05]);
        }

        svg
          .append('g')
          .attr('class', 'x grid')
          .attr('transform', 'translate(0,' + chart_height + ')')
          .call(
            xAxis.ticks(6).tickSizeInner(-height + margin.top + margin.bottom)
          );

        let xAxisLayer = svg
          .append('g')
          .attr('class', 'groupedXAxis')
          .attr('transform', 'translate(0,' + chart_height + ')')
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
          .attr('transform', 'translate(0,' + (chart_height + 35) + ')');

        xAxisLableLayer
          .append('text')
          .attr('class', 'groupedChartXAxisLabel')
          .attr('y', 40)
          .attr('x', chart_width / 2)
          .style('text-anchor', 'middle')
          .text('Grouped By - (' + selectedHyperparams.join(', ') + ')');

        xAxisLableLayer
          .append('text')
          .attr('y', 60)
          .attr('x', chart_width / 2)
          .attr('class', 'axisLabel')
          .style('text-anchor', 'middle')
          .text('Hyperparameters');

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
          .attr('x', -chart_height / 2)
          .attr('dy', '1em')
          .style('text-anchor', 'middle')
          .style('fill', '#444')
          .text('Metrics');

        /* Add line into SVG */
        var line = d3
          .line()
          .defined(d => !isNaN(d.value))
          .x(d => x(d.indexKey) + x.bandwidth() / 2)
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
          .style('stroke', (d, i) => colorScale(i))
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
          .style('fill', (d, i) => colorScale(i))
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
              .text(`${d.value}`)
              .attr('x', d => x(d.indexKey) + 5)
              .attr('y', d => y(d.value) - 10)
              .style('opacity', d => {
                return Number.isNaN(d.value) ? 0 : 1;
              });
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
          .attr('cx', d => x(d.indexKey) + x.bandwidth() / 2)
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

        //Legend
        legendG = d3
          .select(this._legendNode)
          .append('g')
          .attr('transform', 'translate(120,10)')
          .selectAll('.legend')
          .data(
            data.map(function(d) {
              return d.lineIndex;
            })
          )
          .enter()
          .append('g')
          .attr('class', 'ml-legend')
          .attr('transform', function(d, i) {
            return 'translate(0,' + i * 16 + ')';
          })
          .style('opacity', '0');

        legendG
          .append('rect')
          .attr('x', -12)
          .attr('width', 12)
          .attr('height', 10)
          .attr('rx', 1)
          .attr('ry', 1)
          .style('fill', function(d, i) {
            return colorScale(i);
          });

        legendG
          .append('text')
          .attr('x', -24)
          .attr('y', 5)
          .attr('dy', '.35em')
          .style('text-anchor', 'end')
          .style('font-size', '11px')
          .text(function(d) {
            return d;
          });

        legendG
          .transition()
          .duration(200)
          .style('opacity', '1');
      }
    } else {
      errorMessageOnLayer(
        '.groupedLineChart',
        width,
        margin.left,
        height,
        'notAvailableMsg',
        'data not present for this config',
        '\u20E0'
      );
    }
  }

  _setRef(componentNode) {
    this._rootNode = componentNode;
  }

  _setLegendRef(componentNode) {
    this._legendNode = componentNode;
  }

  render() {
    return (
      <div className={styles.groupedChartBlock}>
        <div>
          <React.Fragment>
            <svg
              className="groupedLineChart"
              width={width}
              height={height}
              ref={this._setRef.bind(this)}
            />
            <div />
          </React.Fragment>
        </div>
        <div className={styles.legendBlock}>
          <div className={styles.legendContent}>
            <span className={styles.legendLabel}>Grouped by Metrics:</span>
            <React.Fragment>
              <svg
                className="chartLegend"
                width={180}
                height={360}
                ref={this._setLegendRef.bind(this)}
              />
            </React.Fragment>
          </div>
          <div />
        </div>
      </div>
    );
  }
}

export default MultiLineChart;
