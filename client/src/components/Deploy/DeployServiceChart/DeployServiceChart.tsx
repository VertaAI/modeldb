import * as React from 'react';
import { connect } from 'react-redux';
import * as d3 from 'd3';

import { IApplicationState, IConnectedReduxProps } from 'store/store';
import {
  IDataStatistics,
  IServiceDataFeature,
  IServiceStatistics,
} from 'models/Deploy';
import { Color } from 'csstype';
import styles from './DeployServiceChart.module.css';

interface ILocalProps {
  height: number;
  width: number;
  marginLeft: number;
  marginTop: number;
  maginRight: number;
  marginBottom: number;
  metrics: IServiceStatistics;
}

class Point {
  time!: Date;
  throughput!: number;
  averageLatency!: number;
  p50Latency!: number;
  p90Latency!: number;
  p99Latency!: number;
}

interface IPropsFromState {}

type AllProps = ILocalProps & IPropsFromState;

class DeployServiceChart extends React.PureComponent<AllProps> {
  ref!: SVGSVGElement;

  componentDidMount() {
    const metrics = this.props.metrics;
    const convertTime = (v: number) => {
      var t = new Date(0);
      t.setUTCSeconds(v);
      return t;
    };
    var points: Point[] = [];
    for (var i = 0; i < metrics.time.length; i++) {
      points.push({
        time: convertTime(metrics.time[i]),
        throughput: metrics.throughput[i * 2], // TODO: figure out why
        averageLatency: metrics.averageLatency[i],
        p50Latency: metrics.p50Latency[i],
        p90Latency: metrics.p90Latency[i],
        p99Latency: metrics.p99Latency[i],
      });
    }
    console.log(this.props.metrics);
    const width =
      this.props.width - this.props.marginLeft - this.props.maginRight;
    const height =
      this.props.height - this.props.marginTop - this.props.marginBottom;

    const chart = d3
      .select(this.ref)
      .append('g')
      .attr(
        'transform',
        'translate(' + this.props.marginLeft + ',' + this.props.marginTop + ')'
      );

    // X axis
    const x = d3
      .scaleTime()
      //.domain([1910, 2010])
      .range([0, width]);

    x.domain([points[0].time, points[points.length - 1].time]);

    const xAxis = d3
      .axisBottom(x)
      .tickFormat(v => d3.timeFormat('%m/%d %H:%M:%S')(v as Date));

    chart
      .append('g')
      .attr('transform', 'translate(0,' + height + ')')
      .call(xAxis)
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', 'rotate(-55)');

    // Y axis
    const y = d3.scaleLinear().range([height, 0]);

    y.domain([0, 1.2 * Math.max(...points.map(p => p.p99Latency))]);

    // Add axes
    const yAxis = d3.axisLeft(y).tickFormat(d3.format('.2s'));
    chart.append('g').call(yAxis);

    // Add title
    chart
      .append('text')
      .html('State Population Over Time')
      .attr('x', 200);

    const drawLine = (extractor: (p: Point) => number, color: Color) => {
      const line = d3
        .line<Point>()
        .x(p => x(p.time))
        .y(p => y(extractor(p)));

      chart
        .selectAll()
        .data([points])
        .enter()
        .append('path')
        .attr('fill', 'none')
        .attr('stroke', color)
        .attr('stroke-width', 1)
        .attr('d', line)
        .attr('shape-rendering', 'auto');
    };

    drawLine(p => p.p99Latency, 'red');
    drawLine(p => p.p90Latency, 'blue');
    drawLine(p => p.p50Latency, 'black');

    const tooltip = d3.select('#tooltip');
    const tooltipLine = chart.append('line');

    const tipBox = chart
      .append('rect')
      .attr('width', width)
      .attr('height', height)
      .attr('opacity', 0)
      .on('mousemove', drawTooltip)
      .on('mouseout', removeTooltip);

    function removeTooltip() {
      if (tooltip) tooltip.style('display', 'none');
      if (tooltipLine) tooltipLine.attr('stroke', 'none');
    }

    function drawTooltip() {
      //const year = Math.floor((x.invert(d3.mouse(tipBox.node())[0]) + 5) / 10) * 10;
      //const time = x.invert(d3.mouse(tipBox.node() as SVGRectElement)[0]);

      const closestTime = x.invert(
        d3.mouse(tipBox.node() as SVGRectElement)[0]
      );
      const closestSecond =
        Math.floor((closestTime.getTime() + 2500) / 5000) * 5;
      const closestRoundedDate = convertTime(closestSecond);

      //const time = convertTime(Math.floor(x.invert(d3.mouse(tipBox.node() as SVGRectElement)[0]).getTime() / 1000) * 1);
      console.log(closestRoundedDate.getTime());
      console.log(closestTime.getTime());
      console.log(x(closestTime));
      console.log(x(closestRoundedDate));
      //console.log(time.getTime())

      /*
  states.sort((a, b) => {
    return b.history.find(h => h.year == year).population - a.history.find(h => h.year == year).population;
  })  
  */

      tooltipLine
        .attr('stroke', 'black')
        .attr('x1', x(closestRoundedDate))
        .attr('x2', x(closestRoundedDate))
        .attr('y1', 0)
        .attr('y2', height);

      tooltip
        .style('left', d3.event.pageX + 20)
        .style('top', d3.event.pageY - 20);

      console.log(d3.event.pageX, d3.event.pageY);
      tooltip
        .html('')
        .style('display', 'block')
        .style('left', d3.event.pageX + 20 + 'px')
        .style('top', d3.event.pageY - 20 + 'px')
        .selectAll()
        .data([1, 2, 3])
        .enter()
        .append('div')
        .html(d => '' + d);
      //.style('color', d => d.color)
      //.html(d => d.name + ': ' + d.history.find(h => h.year == year).population);
    }

    /*
    const line = d3
      .line<Point>()
      .x(p => x(p.time))
      .y(p => y(p.p99Latency));

    chart
      .selectAll()
      .data([points])
      .enter()
      .append('path')
      .attr('fill', 'none')
      .attr('stroke', 'black')
      .attr('stroke-width', 1)
      .attr('d', line)
      .attr('shape-rendering', 'auto')*/
  }

  render() {
    return (
      <React.Fragment>
        <svg
          className="container"
          ref={(ref: SVGSVGElement) => (this.ref = ref)}
          shapeRendering={'optimizeQuality'}
          width={this.props.width}
          height={this.props.height}
        />
        <div id="tooltip" className={styles.tooltip} />
      </React.Fragment>
    );
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {};
};

export default connect(mapStateToProps)(DeployServiceChart);
