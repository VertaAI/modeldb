import React, { Component } from 'react';
import * as d3 from 'd3';
import _ from 'lodash';

import { errorMessage } from 'components/Charts/shared/errorMessages';
import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import { cssTheme } from 'core/shared/styles/theme';

import ModelRecordCard from '../../ModelRecordCard/ModelRecordCard';
import styles from '../SummaryChartManager.module.css';

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

const width = 680;
const height = 380;
const margin = { top: 30, right: 40, bottom: 80, left: 70 };
let tooltip;

class ScatterChart extends Component {
  state = {
    marks: [],
    xScale: undefined,
    yScale: undefined,
    isModelOpen: false,
    modelRecordObj: {
      name: '',
      date: '',
      metrics: [],
      hyper: [],
      tags: [],
    },
  };

  xAxis = d3
    .axisBottom()
    .tickFormat(d3.timeFormat('%b/%d %H:%M'))
    .ticks(5);
  yAxis = d3.axisLeft();

  static getDerivedStateFromProps(nextProps, prevState) {
    var { flatdata, selectedMetric, selectedCategory } = nextProps;
    if (!flatdata) {
      return {};
    }
    let categoryAccessor,
      categoryObjRef = '',
      categoryValRef = '';
    if (selectedCategory === 'experimentId') {
      categoryAccessor = d => d.shortExperiment.name;
      categoryObjRef = 'shortExperiment';
      categoryValRef = 'name';
    }

    flatdata = flatdata.filter(x => x[selectedMetric] !== undefined);

    const extent = d3.extent(flatdata, d => d.dateCreated);
    // If less than a minute of difference, plot as if they were the same time to avoid crazy resolution
    if (
      extent[0] &&
      extent[1] &&
      extent[1].getSeconds() - extent[0].getSeconds() < 60
    ) {
      // NOTE: in-place modifications will modify the coordinates for the actual point
      extent[1] = new Date(extent[1].getTime()).setSeconds(
        extent[1].getSeconds() + 60
      );
      extent[0] = new Date(extent[0].getTime()).setSeconds(
        extent[0].getSeconds() - 60
      );
    }

    let xRange = extent[1] - extent[0];
    const xScale = d3
      .scaleTime()
      .domain([extent[0] - xRange * 0.05, extent[1] + xRange * 0.05])
      .range([margin.left, width - margin.right]);

    const [min, max] = d3.extent(flatdata, d => +d[selectedMetric]);
    var yRange = max - min;
    if (yRange === 0) {
      yRange = 1e-1;
    }
    const yScale = d3
      .scaleLinear()
      .domain([min - yRange * 0.07, max + yRange * 0.07])
      .range([height - margin.bottom, margin.top]);

    const marks = _.map(flatdata, d => {
      if (d[selectedMetric]) {
        let dataOnChart = { ...d };
        dataOnChart.cx = xScale(d.dateCreated);
        dataOnChart.cy = yScale(d[selectedMetric]);
        return dataOnChart;
      }
    }).filter(obj => obj !== undefined);

    const categoryValueList = [...new Set(_.map(flatdata, categoryAccessor))];
    colorScale.domain(categoryValueList);
    return {
      marks,
      xScale,
      yScale,
      categoryValueList,
      categoryAccessor,
      categoryObjRef,
      categoryValRef,
    };
  }

  componentDidMount() {
    let xAxisLabel = 'Time Range';
    if (this.props.flatdata === undefined || this.props.flatdata.length === 0) {
      errorMessage(
        '.summaryChart',
        width,
        margin.left,
        height,
        'notAvailableMsg',
        this.props.selectedMetric,
        '\u20E0'
      );
    }

    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis.ticks(6).tickSize(5));

    d3.select(this.refs.xAxisGrid)
      .append('text')
      .attr('class', 'axisLabel')
      .attr('y', margin.top + 30)
      .attr('x', width / 2)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(xAxisLabel);
    d3.select(this.refs.xAxisGrid).call(
      this.xAxis.ticks(6).tickSize(-height + margin.top + margin.bottom)
    );

    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6).tickSize(5));
    d3.select(this.refs.yAxis)
      .append('text')
      .attr('id', 'scatterYLabel')
      .attr('class', 'axisLabel')
      .attr('transform', 'rotate(-90)')
      .attr('y', -margin.left + 7)
      .attr('x', -height / 2)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.selectedMetric);
    d3.select(this.refs.yAxisGrid).call(
      this.yAxis.ticks(6).tickSize(-width + margin.right + margin.left)
    );

    tooltip = d3
      .select(this.refs.tooltipRef)
      .append('div')
      .attr('opacity', 0);
  }
  componentDidUpdate() {
    this.xAxis.scale(this.state.xScale);
    d3.select(this.refs.xAxis).call(this.xAxis.ticks(6).tickSize(5));
    d3.select(this.refs.xAxisGrid).call(
      this.xAxis.ticks(6).tickSize(-height + margin.top + margin.bottom)
    );

    d3.select(this.refs.xAxis)
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', function(d) {
        return 'rotate(-25)';
      });

    this.yAxis.scale(this.state.yScale);
    d3.select(this.refs.yAxis).call(this.yAxis.ticks(6).tickSize(5));
    d3.select(this.refs.yAxisGrid).call(
      this.yAxis.ticks(6).tickSize(-width + margin.right + margin.left)
    );
    d3.select('#scatterYLabel').text(this.props.selectedMetric);
    if (
      this.props.selectedMetric !== 'data not available' &&
      this.props.flatdata !== undefined &&
      this.props.flatdata.length > 0
    ) {
      d3.select('.errorMessage_summaryChartId').remove();
    }
  }

  mouseOver(d, event) {
    d3.select('g.marks')
      .node()
      .appendChild(d3.select(this.refs['ref-' + d.id]).node());

    d3.select(this.refs['ref-' + d.id])
      .transition()
      .duration(100)
      .attr('r', 8)
      .style('stroke', '#eee')
      .style('stroke-width', '2px')
      .attr('opacity', 0.95);

    tooltip
      .transition()
      .duration(100)
      .style('opacity', 1);

    let textContent =
      "<span class='tooltip-label'>" +
      this.props.selectedMetric +
      " :</span> <span class='tooltip-value'>" +
      withScientificNotationOrRounded(d[this.props.selectedMetric]) +
      '</span><br/>' +
      "<span class='tooltip-label'>Date  :</span> <span class='tooltip-value'>" +
      getFormattedDateTime(d.dateCreated) +
      '</span><br/>' +
      "<span class='tooltip-label'>Category  :</span> <span class=" +
      styles.tooltip_category_value +
      ' style=color:' +
      colorScale(d[this.state.categoryObjRef][this.state.categoryValRef]) +
      '>' +
      d[this.state.categoryObjRef][this.state.categoryValRef] +
      '</span>';

    let tipHtml = tooltip.attr('class', 'tooltip').html(textContent);

    tipHtml
      .style('left', event.pageX + 15 + 'px')
      .style('top', event.pageY + 15 + 'px');
  }
  mouseOut(d) {
    d3.select(this.refs['ref-' + d.id])
      .transition()
      .duration(100)
      .attr('r', 6)
      .style('stroke', 'none')
      .attr('opacity', 0.75);

    tooltip
      .transition()
      .duration(100)
      .style('opacity', 0);
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

  render() {
    return (
      <div className={styles.scatterplotBlock}>
        <div>
          <React.Fragment>
            <div ref="tooltipRef" />
            <svg width={width} height={height} className={'summaryChart'}>
              <g
                ref="xAxisGrid"
                className="grid"
                transform={`translate(0, ${height - margin.bottom})`}
              />
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
                    <circle
                      className={styles.marks_circle}
                      key={key}
                      cx={d.cx}
                      cy={d.cy}
                      fill={colorScale(
                        d[this.state.categoryObjRef][this.state.categoryValRef]
                      )}
                      r={6}
                      ref={'ref-' + d.id}
                      onClick={this.onClick.bind(this, d)}
                      onMouseOut={this.mouseOut.bind(this, d)}
                      onMouseOver={this.mouseOver.bind(this, d)}
                    />
                  );
                })}
              </g>
            </svg>
            <div>
              <span className={styles.scatterMeta}>
                *click on the marks to view corresponding ModelRecord
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

export default ScatterChart;
