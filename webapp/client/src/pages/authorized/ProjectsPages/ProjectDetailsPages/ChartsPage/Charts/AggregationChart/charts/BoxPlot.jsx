import React, { Component } from 'react';
import * as d3 from 'd3';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';

const width = 680,
  height = 360,
  boxWidth = 40,
  margin = { top: 40, right: 35, bottom: 65, left: 85 };

let tooltip;

class BoxPlot extends Component {
  state = {
    quantiles: {},
  };
  componentDidMount() {
    const { data } = this.props;

    tooltip = d3
      .select(this.refs.tooltipRef)
      .append('div')
      .attr('opacity', 0);

    let dataRanges = [];
    function computeQuantiles(values) {
      let q1, q3, median, interQuantileRange, min, max;
      q1 = d3.quantile(values.sort(d3.ascending), 0.25);
      median = d3.quantile(values.sort(d3.ascending), 0.5);
      q3 = d3.quantile(values.sort(d3.ascending), 0.75);
      interQuantileRange = q3 - q1;
      min = q1 - 1.5 * interQuantileRange;
      max = q3 + 1.5 * interQuantileRange;
      dataRanges.push(min);
      dataRanges.push(max);
      return {
        q1: q1,
        median: median,
        q3: q3,
        interQuantileRange: interQuantileRange,
        min: min,
        max: max,
      };
    }
    let quantiles = [...data].map(obj => {
      return { key: obj[0], value: computeQuantiles(obj[1]) };
    });

    let svg = d3
      .select(this._rootNode)
      .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
    const chart_width = width - margin.left - margin.right;
    const chart_height = height - margin.top - margin.bottom;
    let yLabelPos = -margin.left / 2 - 20;
    var x = d3
      .scaleBand()
      .range([0, chart_width])
      .domain([...data.keys()])
      .paddingInner(1)
      .paddingOuter(0.5);
    let xAxis = svg
      .append('g')
      .attr('class', 'axis')
      .attr('transform', 'translate(0,' + chart_height + ')')
      .call(d3.axisBottom(x));
    xAxis
      .append('text')
      .attr('class', 'axisLabel')
      .attr('y', margin.top)
      .attr('x', width / 2 + yLabelPos)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.xLabel);

    // const flatValues = [...data.values()].flat();
    const [min, max] = d3.extent(dataRanges);
    const range = max - min;
    var y = d3
      .scaleLinear()
      .domain([min - range * 0.1, max + range * 0.1])
      .range([chart_height, 0]);

    let yAxis = svg
      .append('g')
      .attr('class', 'axis')
      .call(d3.axisLeft(y).ticks(6));
    yAxis
      .append('text')
      .attr('class', 'axisLabel')
      .attr('transform', 'rotate(-90)')
      .attr('x', -height / 2 + 40)
      .attr('y', yLabelPos)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.yLabel);
    svg
      .append('g')
      .attr('class', 'grid')
      .call(
        d3
          .axisLeft(y)
          .ticks(6)
          .tickSize(-width + margin.right + margin.left)
      );

    // main vertical line
    svg
      .selectAll('vertLines')
      .data(quantiles)
      .enter()
      .append('line')
      .attr('x1', function(d) {
        return x(d.key);
      })
      .attr('x2', function(d) {
        return x(d.key);
      })
      .attr('y1', function(d) {
        return y(d.value.min);
      })
      .attr('y2', function(d) {
        return y(d.value.max);
      })
      .attr('stroke', '#444');

    // rectangle for the main box
    svg
      .selectAll('boxes')
      .data(quantiles)
      .enter()
      .append('rect')
      .attr('x', function(d) {
        return x(d.key) - boxWidth / 2;
      })
      .attr('y', function(d) {
        return y(d.value.q3);
      })
      .attr('height', function(d) {
        return y(d.value.q1) - y(d.value.q3);
      })
      .attr('width', boxWidth)
      .style('fill', '#5fe6c9')
      .on('mouseover', function(d) {
        const reducer = (accumulator, currentValue) =>
          accumulator + currentValue;
        tooltip
          .transition()
          .duration(200)
          .style('opacity', 0.95);

        tooltip
          .attr('class', 'tooltip')
          .html(
            Object.keys(d.value)
              .map(function(key) {
                return `<span class='tooltip-label'> ${key}:</span> <span class='tooltip-value'> ${withScientificNotationOrRounded(d.value[key])} </span><br/>`;
              })
              .reduce(reducer)
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

    // line median
    svg
      .selectAll('medianLines')
      .data(quantiles)
      .enter()
      .append('line')
      .attr('x1', function(d) {
        return x(d.key) - boxWidth / 2;
      })
      .attr('x2', function(d) {
        return x(d.key) + boxWidth / 2;
      })
      .attr('y1', function(d) {
        return y(d.value.median);
      })
      .attr('y2', function(d) {
        return y(d.value.median);
      })
      .attr('stroke', '#444');
  }
  componentDidUpdate() {
    const { data } = this.props;
    let updatedDataRange = [];
    function computeQuantiles(values) {
      let q1, q3, median, interQuantileRange, min, max;
      q1 = d3.quantile(values.sort(d3.ascending), 0.25);
      median = d3.quantile(values.sort(d3.ascending), 0.5);
      q3 = d3.quantile(values.sort(d3.ascending), 0.75);
      interQuantileRange = q3 - q1;
      min = q1 - 1.5 * interQuantileRange;
      max = q3 + 1.5 * interQuantileRange;
      updatedDataRange.push(min);
      updatedDataRange.push(max);
      return {
        q1: q1,
        median: median,
        q3: q3,
        interQuantileRange: interQuantileRange,
        min: min,
        max: max,
      };
    }
    let quantiles = [...data].map(obj => {
      return { key: obj[0], value: computeQuantiles(obj[1]) };
    });

    d3.select(this._rootNode)
      .selectAll('g')
      .remove();

    let svg = d3
      .select(this._rootNode)
      .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
    const chart_width = width - margin.left - margin.right;
    const chart_height = height - margin.top - margin.bottom;
    let yLabelPos = -margin.left / 2 - 20;
    var x = d3
      .scaleBand()
      .range([0, chart_width])
      .domain([...data.keys()])
      .paddingInner(1)
      .paddingOuter(0.5);
    let xAxis = svg
      .append('g')
      .attr('class', 'axis')
      .attr('transform', 'translate(0,' + chart_height + ')')
      .call(d3.axisBottom(x));
    xAxis
      .append('text')
      .attr('class', 'axisLabel')
      .attr('y', margin.top)
      .attr('x', width / 2 + yLabelPos)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.xLabel);

    // const flatValues = [...data.values()].flat();
    const [min, max] = d3.extent(updatedDataRange);
    const range = max - min;
    var y = d3
      .scaleLinear()
      .domain([min - range * 0.1, max + range * 0.1])
      .range([chart_height, 0]);

    let yAxis = svg
      .append('g')
      .attr('class', 'axis')
      .call(d3.axisLeft(y).ticks(6));
    yAxis
      .append('text')
      .attr('class', 'axisLabel')
      .attr('transform', 'rotate(-90)')
      .attr('x', -height / 2 + 40)
      .attr('y', yLabelPos)
      .style('text-anchor', 'middle')
      .style('fill', '#444')
      .text(this.props.yLabel);
    svg
      .append('g')
      .attr('class', 'grid')
      .call(
        d3
          .axisLeft(y)
          .ticks(6)
          .tickSize(-width + margin.right + margin.left)
      );

    // main vertical line
    svg
      .selectAll('vertLines')
      .data(quantiles)
      .enter()
      .append('line')
      .attr('x1', function(d) {
        return x(d.key);
      })
      .attr('x2', function(d) {
        return x(d.key);
      })
      .attr('y1', function(d) {
        return y(d.value.min);
      })
      .attr('y2', function(d) {
        return y(d.value.max);
      })
      .attr('stroke', '#444');

    // rectangle for the main box
    svg
      .selectAll('boxes')
      .data(quantiles)
      .enter()
      .append('rect')
      .attr('x', function(d) {
        return x(d.key) - boxWidth / 2;
      })
      .attr('y', function(d) {
        return y(d.value.q3);
      })
      .attr('height', function(d) {
        return y(d.value.q1) - y(d.value.q3);
      })
      .attr('width', boxWidth)
      .style('fill', '#5fe6c9')
      .on('mouseover', function(d) {
        const reducer = (accumulator, currentValue) =>
          accumulator + currentValue;
        tooltip
          .transition()
          .duration(200)
          .style('opacity', 0.95);

        tooltip
          .attr('class', 'tooltip')
          .html(
            Object.keys(d.value)
              .map(function(key) {
                return `<span class='tooltip-label'> ${key}:</span> <span class='tooltip-value'> ${withScientificNotationOrRounded(d.value[key])} </span><br/>`;
              })
              .reduce(reducer)
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

    // line median
    svg
      .selectAll('medianLines')
      .data(quantiles)
      .enter()
      .append('line')
      .attr('x1', function(d) {
        return x(d.key) - boxWidth / 2;
      })
      .attr('x2', function(d) {
        return x(d.key) + boxWidth / 2;
      })
      .attr('y1', function(d) {
        return y(d.value.median);
      })
      .attr('y2', function(d) {
        return y(d.value.median);
      })
      .attr('stroke', '#444');
  }

  _setRef(componentNode) {
    this._rootNode = componentNode;
  }

  render() {
    return (
      <div>
        <div>
          <React.Fragment>
            <svg
              className={'aggregatedBoxChart'}
              width={width}
              height={height}
              ref={this._setRef.bind(this)}
            />
            <div ref="tooltipRef" />
          </React.Fragment>
        </div>
      </div>
    );
  }
}

export default BoxPlot;
