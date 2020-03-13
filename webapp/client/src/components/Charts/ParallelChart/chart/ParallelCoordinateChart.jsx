import React, { Component } from 'react';
import * as d3 from 'd3';

import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';
import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { errorMessageOnLayer } from 'components/Charts/shared/errorMessages';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import { cssTheme } from 'core/shared/styles/theme';

import ModelRecordCard from '../../ModelRecordCard/ModelRecordCard';
import styles from '../ParallelChartManager.module.css';

const width = 680;
const height = 360;
const margin = { top: 75, right: 45, bottom: 35, left: 50 };
let svg, legendG, tooltip, lineMissingCount;
let categoryAccessor,
  categoryObjRef = '',
  categoryValRef = '';

let colorScale = d3
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

class ParallelCoordinates extends Component {
  state = {
    isModelOpen: false,
    modelRecordObj: {
      name: '',
      date: '',
      metrics: [],
      hyper: [],
      tags: [],
    },
    categoryValueList: 0,
    categoryObjRef: '',
    categoryValRef: '',
  };
  componentDidMount() {
    const { data, metricKeysSet, selectedPanelElements } = this.props;
    lineMissingCount = 0;
    tooltip = d3
      .select(this.refs.tooltipRef)
      .append('div')
      .attr('opacity', 0);

    if (
      data !== undefined &&
      data.length > 0 &&
      selectedPanelElements.length > 0
    ) {
      svg = d3
        .select(this._rootNode)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
      const chart_width = width - margin.left - margin.right;
      const chart_height = height - margin.top - margin.bottom;

      var x = d3
          .scalePoint()
          .range([0, chart_width])
          .padding(0.1),
        y = {};

      var line = d3.line().defined(function(d) {
        return !isNaN(d[1]);
      });

      // filter out the initial metric
      let dimensions = selectedPanelElements.filter(function(d) {
        const [min, max] = d3.extent(data, p => +p[d]);
        const range = max - min;
        if (range === 0) {
          return (y[d] = d3
            .scaleLinear()
            .domain([min - min * 0.05, max + max * 0.05])
            .range([chart_height, 0]));
        }
        return (y[d] = d3
          .scaleLinear()
          .domain([min - range * 0.05, max + range * 0.05])
          .range([chart_height, 0]));
      });
      x.domain(dimensions);

      // Add a group element for each dimension.
      var g = svg
        .selectAll('.dimension')
        .data(dimensions)
        .enter()
        .append('g')
        .attr('class', 'dimension')
        .attr('transform', function(d) {
          return 'translate(' + x(d) + ')';
        });
      g.append('text')
        .attr('class', 'parallel_axis_label')
        .style('text-anchor', 'middle')
        .attr('y', -30)
        .text(function(d) {
          return d;
        });

      g.append('rect')
        .attr('width', 65)
        .attr('height', height - margin.top)
        .attr('rx', '5')
        .attr('ry', '5')
        .style('z-index', -1)
        .attr('transform', `translate(-35,${-margin.top / 4} )`)
        .attr('class', function(d) {
          if (metricKeysSet.has(d)) {
            return 'metricAxis';
          }
          return 'hyperAxis';
        });

      // Add axis
      g.append('g')
        .attr('class', 'axis')
        .each(function(d) {
          d3.select(this).call(d3.axisLeft(y[d]).ticks(6));
        });

      svg
        .append('g')
        .attr('class', 'parallelLines')
        .selectAll('path')
        .data(data)
        .enter()
        .append('path')
        .attr('class', 'parallel_line')
        .attr('d', path)
        .attr('cursor', 'pointer')
        .on('mouseover', function(d) {
          this.parentElement.appendChild(this);
          svg
            .selectAll('.metricAxis')
            .transition()
            .duration(200)
            .style('opacity', 0.4);
          tooltip
            .transition()
            .duration(200)
            .style('opacity', 0.95);
          const reducer = (accumulator, currentValue) =>
            accumulator + currentValue;
          tooltip
            .attr('class', 'tooltip')
            .html(
              Object.keys(d)
                .filter(
                  key =>
                    key !== 'modelRecord' && selectedPanelElements.includes(key)
                )
                .map(function(key) {
                  return `<span class='tooltip-label'> ${key}:</span> <span class='tooltip-value'> ${withScientificNotationOrRounded(d[key])} </span><br/>`;
                })
                .reduce(reducer)
            )
            .style('left', d3.event.pageX + 15 + 'px')
            .style('top', d3.event.pageY + 15 + 'px');
        })
        .on('mouseout', function(d) {
          svg
            .selectAll('.metricAxis')
            .transition()
            .duration(500)
            .style('opacity', 1);
          tooltip
            .transition()
            .duration(300)
            .style('opacity', 0);
        })
        .on('click', d => {
          return this.onClick(d.modelRecord);
        });

      // Returns the path for a given data point.
      function path(d) {
        const lineData = line(
          dimensions.map(function(p) {
            return [x(p), y[p](d[p])];
          })
        );
        if (lineData === null) {
          lineMissingCount = lineMissingCount + 1;
        }
        return lineData;
      }
    } else {
      errorMessageOnLayer(
        '.parallelChart',
        width,
        margin.left + 15, // padding to center message
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
      metricKeysSet,
      selectedPanelElements,
      selectedCategory,
    } = this.props;
    lineMissingCount = 0;
    if (selectedCategory === 'experimentId') {
      categoryAccessor = d => d.modelRecord.shortExperiment.name;
      categoryObjRef = 'shortExperiment';
      categoryValRef = 'name';
    }
    const categoryValueList = [...new Set(data.map(categoryAccessor))];
    if (prevProps !== this.props) {
      this.setState({
        categoryValueList,
        categoryObjRef,
        categoryValRef,
      });
    }

    colorScale.domain(categoryValueList);
    d3.select(this._rootNode)
      .selectAll('g')
      .remove();
    tooltip.remove();

    if (
      data !== undefined &&
      data.length > 0 &&
      selectedPanelElements.length > 0
    ) {
      svg = d3
        .select(this._rootNode)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
      const chart_width = width - margin.left - margin.right;
      const chart_height = height - margin.top - margin.bottom;

      tooltip = d3
        .select(this.refs.tooltipRef)
        .append('div')
        .attr('opacity', 0);

      var x = d3
          .scalePoint()
          .range([0, chart_width])
          .padding(0.1),
        y = {};

      var line = d3.line().defined(function(d) {
        return !isNaN(d[1]);
      });

      // filter out the initial metric
      let dimensions = selectedPanelElements.filter(function(d) {
        const [min, max] = d3.extent(data, p => +p[d]);
        const range = max - min;
        if (range === 0) {
          return (y[d] = d3
            .scaleLinear()
            .domain([min - min * 0.05, max + max * 0.05])
            .range([chart_height, 0]));
        }
        return (y[d] = d3
          .scaleLinear()
          .domain([min - range * 0.05, max + range * 0.05])
          .range([chart_height, 0]));
      });
      x.domain(dimensions);

      // Add a group element for each dimension.
      var g = svg
        .selectAll('.dimension')
        .data(dimensions)
        .enter()
        .append('g')
        .attr('class', 'dimension')
        .attr('transform', function(d) {
          return 'translate(' + x(d) + ')';
        });
      g.append('text')
        .attr('class', 'parallel_axis_label')
        .style('text-anchor', 'middle')
        .attr('y', -30)
        .text(function(d) {
          return d;
        });

      g.append('rect')
        .attr('width', 65)
        .attr('height', height - margin.top)
        .attr('rx', '5')
        .attr('ry', '5')
        .style('z-index', -1)
        .attr('transform', `translate(-35,${-margin.top / 4} )`)
        .attr('class', function(d) {
          if (metricKeysSet.has(d)) {
            return 'metricAxis';
          }
          return 'hyperAxis';
        });

      svg
        .append('g')
        .attr('class', 'parallelLines')
        .selectAll('path')
        .data(data)
        .enter()
        .append('path')
        .attr('d', path)
        .attr('stroke', d =>
          colorScale(d.modelRecord[categoryObjRef][categoryValRef])
        )
        .attr('cursor', 'pointer')
        .on('mouseover', function(d) {
          this.parentElement.appendChild(this);
          svg
            .selectAll('.metricAxis')
            .transition()
            .duration(200)
            .style('opacity', 0.4);
          tooltip
            .transition()
            .duration(200)
            .style('opacity', 0.95);
          const reducer = (accumulator, currentValue) =>
            accumulator + currentValue;
          tooltip
            .attr('class', 'tooltip')
            .html(
              Object.keys(d)
                .filter(
                  key =>
                    key !== 'modelRecord' && selectedPanelElements.includes(key)
                )
                .map(function(key) {
                  return `<span class='tooltip-label'> ${key}:</span> <span class='tooltip-value'> ${withScientificNotationOrRounded(d[key])} </span><br/>`;
                })
                .reduce(reducer)
              // todo add this as a part of the data array to dynamically produce legend colors
              //           .append(`<span class='tooltip-label'>Category  :</span> <span class=${
              //           styles.tooltip_category_value
              //         }
              //             style=color:${colorScale(
              //               d[this.state.categoryObjRef][this.state.categoryValRef]
              //             )}>
              // ${d[this.state.categoryObjRef][this.state.categoryValRef]}</span>`)
            )
            .style('left', d3.event.pageX + 15 + 'px')
            .style('top', d3.event.pageY + 15 + 'px');
        })
        .on('mouseout', function(d) {
          svg
            .selectAll('.metricAxis')
            .transition()
            .duration(500)
            .style('opacity', 1);
          tooltip
            .transition()
            .duration(300)
            .style('opacity', 0);
        })
        .on('click', d => {
          return this.onClick(d.modelRecord);
        });

      // Add axis
      g.append('g')
        .attr('class', 'axis')
        .each(function(d) {
          d3.select(this).call(d3.axisLeft(y[d]).ticks(6));
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

      // Returns the path for a given data point.
      function path(d) {
        const lineData = line(
          dimensions.map(function(p) {
            return [x(p), y[p](d[p])];
          })
        );
        if (lineData === null) {
          lineMissingCount = lineMissingCount + 1;
        }
        return lineData;
      }
    } else {
      errorMessageOnLayer(
        '.parallelChart',
        width,
        margin.left + 15, // padding to center message
        height,
        'notAvailableMsg',
        'data not present for this config',
        '\u20E0'
      );
    }
  }

  onClick(d) {
    let metricObj = [],
      hyperObj = [];
    d.metrics.forEach(d => {
      metricObj.push(d);
    });
    d.hyperparameters.forEach(d => {
      hyperObj.push(d);
    });
    this.setState({ isModelOpen: true });
    this.setState({
      modelRecordObj: {
        name: d.name,
        timestamp: getFormattedDateTime(d.dateCreated),
        projectId: d.projectId,
        id: d.id,
        metrics: metricObj,
        hyper: hyperObj,
        tags: d.tags,
      },
    });
  }

  onClose = () => {
    this.setState({ isModelOpen: false });
  };

  _setRef(componentNode) {
    this._rootNode = componentNode;
  }

  _setLegendRef(componentNode) {
    this._legendNode = componentNode;
  }

  render() {
    return (
      <div className={styles.parallelChartBlock}>
        <div>
          <React.Fragment>
            <div ref="tooltipRef" />
            <svg
              className="parallelChart"
              width={width}
              height={height}
              ref={this._setRef.bind(this)}
            />
            {lineMissingCount > 0 && (
              <div>
                <span className={styles.parallelMeta}>
                  {`Lines with missing data: ${lineMissingCount}`}
                </span>
              </div>
            )}
            <div>
              <span className={styles.parallelMeta}>
                * hover over the lines to inspect respective ModelRecord
              </span>
            </div>
          </React.Fragment>
        </div>
        <div
          className={
            !this.state.isModelOpen
              ? styles.legend_block
              : styles.legend_block_hide
          }
        >
          <ScrollableContainer
            maxHeight={340}
            containerOffsetValue={12}
            children={
              <React.Fragment>
                <div className={styles.legend_section_label}>Categories:</div>
                {this.state.categoryValueList.length > 0 && (
                  <svg
                    width={220}
                    height={this.state.categoryValueList.length * 18}
                  >
                    <g ref="legend-rects" className="legend-marks">
                      {this.state.categoryValueList.map((d, i) => {
                        return (
                          <g key={i}>
                            <rect
                              className={styles.legend_rect}
                              key={i}
                              x={20}
                              y={i * 18}
                              rx={2}
                              ry={2}
                              fill={colorScale(d)}
                              ref={'ref-' + d}
                            />
                            <text
                              className={styles.legend_label}
                              x={40}
                              y={10 + i * 18}
                            >
                              {d}
                            </text>
                          </g>
                        );
                      })}
                    </g>
                  </svg>
                )}
              </React.Fragment>
            }
          />
        </div>

        <div>
          <ModelRecordCard
            isOpen={this.state.isModelOpen}
            onRequestClose={this.onClose}
            data={this.state.modelRecordObj}
          />
        </div>
      </div>
    );
  }
}

export default ParallelCoordinates;
