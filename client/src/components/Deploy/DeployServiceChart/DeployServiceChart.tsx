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
import ServiceFactory from 'services/ServiceFactory';
import { bind } from 'decko';

interface ILocalProps {
  height: number;
  width: number;
  marginLeft: number;
  marginTop: number;
  marginRight: number;
  marginBottom: number;
  modelId: string;
}

interface IPoint {
  time: Date;
  throughput: number;
  averageLatency: number;
  p50Latency: number;
  p90Latency: number;
  p99Latency: number;
}

interface ILocalState {
  metrics: IServiceStatistics;
}

interface IPropsFromState {}

type AllProps = ILocalProps & IPropsFromState;
let tipboxNode: any;

class DeployServiceChart extends React.Component<AllProps, ILocalState> {
  public ref!: SVGSVGElement;
  public timeout: any;

  public constructor(props: AllProps) {
    super(props);

    this.state = {
      metrics: {
        averageLatency: [],
        p50Latency: [],
        p90Latency: [],
        p99Latency: [],
        throughput: [],
        time: [],
      } as IServiceStatistics,
    };
  }

  public convertTime(v: number) {
    const date = new Date(0);
    date.setUTCSeconds(v);
    return date;
  }

  public transposePoints() {
    const metrics = this.state.metrics;
    const points: IPoint[] = [];
    for (let i = 0; i < metrics.time.length; i++) {
      points.push({
        averageLatency: metrics.averageLatency[i],
        p50Latency: metrics.p50Latency[i],
        p90Latency: metrics.p90Latency[i],
        p99Latency: metrics.p99Latency[i],
        throughput: metrics.throughput[i * 2], // TODO: figure out why
        time: this.convertTime(metrics.time[i]),
      });
    }
    return points;
  }

  @bind
  public dataRefresh() {
    ServiceFactory.getDeployService()
      .getServiceStatistics(this.props.modelId)
      .then(stat => {
        this.setState({
          metrics: stat.data,
        });
      });
  }

  public componentDidMount() {
    this.timeout = setInterval(this.dataRefresh, 5 * 1000);
    this.dataRefresh();
  }

  public componentWillUnmount() {
    clearInterval(this.timeout);
  }

  public fullRebuild() {
    if (this.state.metrics.time.length === 0) {
      return;
    }
    const points = this.transposePoints();
    const width =
      this.props.width - this.props.marginLeft - this.props.marginRight;
    const height =
      this.props.height - this.props.marginTop - this.props.marginBottom;

    d3.select(this.ref)
      .selectAll('g')
      .remove();

    const chart = d3
      .select(this.ref)
      .append('g')
      .attr(
        'transform',
        `translate(${this.props.marginLeft}, ${this.props.marginTop})`
      );

    // X axis
    const x = d3
      .scaleTime()
      // .domain([1910, 2010])
      .range([0, width]);

    x.domain([points[0].time, points[points.length - 1].time]);

    const xAxis = d3
      .axisBottom(x)
      .tickFormat(v => d3.timeFormat('%m/%d %H:%M:%S')(v as Date));

    chart
      .append('g')
      .attr('transform', `translate(0, ${height})`)
      .call(xAxis)
      .selectAll('text')
      .style('text-anchor', 'end')
      .attr('dx', '-.8em')
      .attr('dy', '.15em')
      .attr('transform', 'rotate(-55)');

    // Y axis
    const yLatency = d3.scaleLinear().range([height, 0]);
    yLatency.domain([0, 1.2 * Math.max(...points.map(p => p.p99Latency))]);
    const yLatencyAxis = d3.axisLeft(yLatency).tickFormat(d3.format('.2s'));
    chart.append('g').call(yLatencyAxis);

    // text label for the y axis
    chart
      .append('text')
      .attr('transform', 'rotate(-90)')
      .attr('y', 0 - this.props.marginLeft)
      .attr('x', 0 - height / 2)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .style('font-size', '14px')
      .text('Latency');

    const yThroughput = d3.scaleLinear().range([height, 0]);
    yThroughput.domain([0, 1.2 * Math.max(...points.map(p => p.throughput))]);
    const yThroughputAxis = d3
      .axisRight(yThroughput)
      .tickFormat(d3.format('.2s'));
    chart
      .append('g')
      .attr('transform', `translate(${width}, 0)`)
      .call(yThroughputAxis);

    // text label for the y axis
    chart
      .append('text')
      .attr('transform', 'rotate(90)')
      .attr('y', -width - this.props.marginRight)
      .attr('x', 0 + height / 2)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .style('font-size', '14px')
      .text('Throughput');

    const drawLine = (
      extractor: (p: IPoint) => number,
      color: Color,
      yScale: (value: number) => number
    ) => {
      const line = d3
        .line<IPoint>()
        .x(p => x(p.time))
        .y(p => yScale(extractor(p)));

      chart
        .selectAll()
        .data([points])
        .enter()
        .append('path')
        .attr('fill', 'none')
        .attr('stroke', color)
        .attr('stroke-width', 1)
        .attr('d', line);
    };

    drawLine(p => p.p99Latency, 'red', yLatency);
    drawLine(p => p.p90Latency, '#BDB76B', yLatency);
    drawLine(p => p.p50Latency, 'darkgreen', yLatency);
    drawLine(p => p.throughput, 'black', yThroughput);

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

    const convertTime = this.convertTime;

    function drawTooltip() {
      try {
        tipboxNode = tipBox.node();
      } catch {}

      const closestTime = x.invert(d3.mouse(tipboxNode as SVGRectElement)[0]);
      const closestSecond =
        Math.floor((closestTime.getTime() + 2500) / 5000) * 5;
      const closestRoundedDate = convertTime(closestSecond);

      tooltipLine
        .attr('stroke', '#6863ff')
        .attr('x1', x(closestRoundedDate))
        .attr('x2', x(closestRoundedDate))
        .attr('y1', 0)
        .attr('y2', height);

      tooltip
        .style('left', d3.event.pageX + 20)
        .style('top', d3.event.pageY - 20);

      const closestPoint = points
        .map(point => {
          return {
            point,
            timeDiff: Math.abs(
              point.time.getTime() - closestRoundedDate.getTime()
            ),
          };
        })
        .sort((a, b) => {
          return a.timeDiff - b.timeDiff;
        })[0].point;

      tooltip
        .html('')
        .style('display', 'block')
        .style('left', `${d3.event.pageX + 20}  px`)
        .style('top', `${d3.event.pageY - 20} px`)
        .selectAll()
        .data([
          {
            title: 'Latency p99:',
            value: closestPoint.p99Latency,
            unit: 'ms',
            scale: 1000,
            fixed: 0,
          },
          {
            title: 'Latency p90:',
            value: closestPoint.p90Latency,
            unit: 'ms',
            scale: 1000,
            fixed: 0,
          },
          {
            title: 'Latency p50:',
            value: closestPoint.p50Latency,
            unit: 'ms',
            scale: 1000,
            fixed: 0,
          },
          {
            title: 'Throughput:',
            value: closestPoint.throughput,
            unit: ' qps',
            scale: 1,
            fixed: 3,
          },
        ])
        .enter()
        .append('div')
        .html(
          d => d.title + ' ' + (d.value * d.scale).toFixed(d.fixed) + d.unit
        );
    }
  }

  public render() {
    this.fullRebuild();
    return (
      <React.Fragment>
        <svg
          className={`container ${styles.chart}`}
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
