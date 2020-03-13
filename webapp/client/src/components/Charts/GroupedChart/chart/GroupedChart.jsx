import React, { Component } from 'react';
import * as d3 from 'd3';

import { errorMessageOnLayer } from 'components/Charts/shared/errorMessages';

import styles from '../GroupedChartManager.module.css';

const width = 680,
  height = 380,
  margin = { top: 30, right: 35, bottom: 100, left: 75 };
let svg, legendG, tooltip;

class GroupedChart extends Component {
  state = {};
  componentDidMount() {
    const {
      data,
      selectedHyperparams,
      selectedYAxisType,
      colorScale,
    } = this.props;

    tooltip = d3
      .select(this.refs.tooltipRef)
      .append('div')
      .attr('opacity', 0);

    if (data.filter(obj => obj.values.length > 0).length > 0) {
      svg = d3
        .select(this._rootNode)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
      const chart_width = width - margin.left - margin.right;
      const chart_height = height - margin.top - margin.bottom;

      var x0 = d3
        .scaleBand()
        .rangeRound([0, chart_width])
        .padding(0.1);

      var x1 = d3.scaleBand(),
        y;

      if (selectedYAxisType === 'linear') {
        y = d3.scaleLinear().range([chart_height, 0]);
      } else {
        y = d3.scaleLog().range([chart_height, 0]);
      }

      var xAxis = d3.axisBottom().scale(x0);

      var yAxis = d3.axisLeft().scale(y);

      if (data && data !== undefined && data !== null && data.length > 0) {
        var indexKeys = data.map(function(d) {
          return d.indexKey;
        });
        var metricKeys = data[0].values.map(function(d) {
          return d.metricKey;
        });

        x0.domain(indexKeys);
        x1.domain(metricKeys).rangeRound([0, x0.bandwidth()]);
        const yMinVal = selectedYAxisType === 'log' ? 1e-6 : 0;
        y.domain([
          yMinVal,
          d3.max(data, function(obj) {
            return d3.max(obj.values, function(d) {
              return d.value;
            });
          }),
        ]);

        let xAxisLayer = svg
          .append('g')
          .attr('class', 'groupedXAxis')
          .attr('transform', 'translate(0,' + chart_height + ')')
          .call(xAxis);

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
          .attr('class', 'y axis')
          .style('opacity', '0')
          .call(yAxis)
          .append('text')
          .attr('id', 'scatterYLabel')
          .attr('class', 'axisLabel')
          .attr('transform', 'rotate(-90)')
          .attr('y', -margin.left + 10)
          .attr('x', -chart_height / 2)
          .attr('dy', '1em')
          .style('text-anchor', 'middle')
          .text('Metrics');

        svg
          .select('.y')
          .transition()
          .duration(100)
          .style('opacity', '1');

        var slice = svg
          .selectAll('.slice')
          .data(data)
          .enter()
          .append('g')
          .attr('class', 'g')
          .attr('transform', function(d) {
            return 'translate(' + x0(d.indexKey) + ',0)';
          });

        slice
          .selectAll('rect')
          .data(function(d) {
            return d.values;
          })
          .enter()
          .append('rect')
          .attr('width', x1.bandwidth())
          .attr('x', function(d) {
            return x1(d.metricKey);
          })
          .style('fill', function(d, i) {
            return colorScale(i);
          })
          .attr('y', function(d) {
            return y(0);
          })
          .attr('height', function(d) {
            return chart_height - y(0);
          })
          .on('mouseover', function(d) {
            tooltip
              .transition()
              .duration(200)
              .style('opacity', 0.95);

            tooltip
              .attr('class', 'tooltip')
              .html(
                `<span class='tooltip-label'> ${
                  d.metricKey
                }:</span> <span class='tooltip-value'> ${d.value} </span>`
              )
              .style('left', d3.event.pageX + 15 + 'px')
              .style('top', d3.event.pageY + 15 + 'px');
          })
          .on('mouseout', function(d) {
            tooltip
              .transition()
              .duration(300)
              .style('opacity', 0);
          });

        slice
          .selectAll('rect')
          .attr('y', function(d) {
            return y(d.value);
          })
          .attr('height', function(d) {
            return chart_height - y(d.value);
          });

        //Legend
        legendG = d3
          .select(this._legendNode)
          .append('g')
          .attr('transform', 'translate(120,10)')
          .selectAll('.legend')
          .data(
            data[0].values.map(function(d) {
              return d.metricKey;
            })
          )
          .enter()
          .append('g')
          .attr('class', 'legend')
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
        '.groupedBarChart',
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
      const chart_width = width - margin.left - margin.right;
      const chart_height = height - margin.top - margin.bottom;

      var x0 = d3
        .scaleBand()
        .rangeRound([0, chart_width])
        .padding(0.1);

      var x1 = d3.scaleBand(),
        y;

      if (selectedYAxisType === 'linear') {
        y = d3.scaleLinear().range([chart_height, 0]);
      } else {
        y = d3.scaleLog().range([chart_height, 0]);
      }

      var xAxis = d3.axisBottom().scale(x0);

      var yAxis = d3.axisLeft().scale(y);

      if (data && data !== undefined && data !== null && data.length > 0) {
        var indexKeys = data.map(d => d.indexKey);
        var metricKeys = data[0].values.map(d => d.metricKey);

        x0.domain(indexKeys);
        x1.domain(metricKeys).rangeRound([0, x0.bandwidth()]);
        const yMinVal = selectedYAxisType === 'log' ? 1e-6 : 0;
        y.domain([
          yMinVal,
          d3.max(data, function(obj) {
            return d3.max(obj.values, function(d) {
              return d.value;
            });
          }),
        ]);

        let xAxisLayer = svg
          .append('g')
          .attr('class', 'groupedXAxis')
          .attr('transform', 'translate(0,' + chart_height + ')')
          .call(xAxis);

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
          .attr('class', 'y axis')
          .style('opacity', '0')
          .call(yAxis)
          .append('text')
          .attr('id', 'scatterYLabel')
          .attr('class', 'axisLabel')
          .attr('transform', 'rotate(-90)')
          .attr('y', -margin.left + 10)
          .attr('x', -chart_height / 2)
          .attr('dy', '1em')
          .style('text-anchor', 'middle')
          .text('Metrics');

        svg
          .select('.y')
          .transition()
          .duration(100)
          .style('opacity', '1');

        var slice = svg
          .selectAll('.slice')
          .data(data)
          .enter()
          .append('g')
          .attr('class', 'g')
          .attr('transform', function(d) {
            return 'translate(' + x0(d.indexKey) + ',0)';
          });

        slice
          .selectAll('rect')
          .data(function(d) {
            return d.values;
          })
          .enter()
          .append('rect')
          .attr('width', x1.bandwidth())
          .attr('x', function(d) {
            return x1(d.metricKey);
          })
          .style('fill', function(d, i) {
            return colorScale(i);
          })
          .attr('y', function(d) {
            return y(0);
          })
          .attr('height', function(d) {
            return chart_height - y(0);
          })
          .on('mouseover', function(d) {
            tooltip
              .transition()
              .duration(200)
              .style('opacity', 0.95);

            tooltip
              .attr('class', 'tooltip')
              .html(
                `<span class='tooltip-label'> ${
                  d.metricKey
                }:</span> <span class='tooltip-value'> ${d.value} </span>`
              )
              .style('left', d3.event.pageX + 15 + 'px')
              .style('top', d3.event.pageY + 15 + 'px');
          })
          .on('mouseout', function(d) {
            tooltip
              .transition()
              .duration(300)
              .style('opacity', 0);
          });

        slice
          .selectAll('rect')
          .attr('y', function(d) {
            return y(d.value);
          })
          .attr('height', function(d) {
            return chart_height - y(d.value);
          });

        legendG = d3
          .select(this._legendNode)
          .append('g')
          .attr('transform', 'translate(120,10)')
          .selectAll('.legend')
          .data(
            data[0].values.map(function(d) {
              return d.metricKey;
            })
          )
          .enter()
          .append('g')
          .attr('class', 'legend')
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
        '.groupedBarChart',
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
            <div ref="tooltipRef" />
            <svg
              className="groupedBarChart"
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

export default GroupedChart;
