import * as React from 'react';
import { connect } from 'react-redux';
import * as d3 from 'd3';

import { IApplicationState, IConnectedReduxProps } from 'store/store';
import {
  IDataStatistics,
  IServiceDataFeature,
  IServiceStatistics,
} from 'models/Deploy';

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
}

interface IPropsFromState {}

type AllProps = ILocalProps & IPropsFromState;

class DeployServiceChart extends React.PureComponent<AllProps> {
  ref!: SVGSVGElement;

  componentDidMount() {
    const metrics = this.props.metrics;
    const convertTime = (v: number) => {
      var t = new Date(1970, 0, 1);
      t.setSeconds(v);
      return t;
    };
    var points: Point[] = [];
    for (var i = 0; i < metrics.time.length; i++) {
      points.push({
        time: convertTime(metrics.time[i]),
        throughput: metrics.throughput[i * 2], // TODO: figure out why
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

    const x = d3
      .scaleTime()
      //.domain([1910, 2010])
      .range([0, width]);

    x.domain([points[0].time, points[points.length - 1].time]);
    //x.domain(d3.extent(points, d => d.time))

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

    const y = d3
      .scaleLinear()
      .domain([0, 40000000])
      .range([height, 0]);

    // Add axes
    const yAxis = d3.axisLeft(y).tickFormat(d3.format('.2s'));
    chart.append('g').call(yAxis);

    // Add title
    chart
      .append('text')
      .html('State Population Over Time')
      .attr('x', 200);
  }

  render() {
    return (
      <svg
        className="container"
        ref={(ref: SVGSVGElement) => (this.ref = ref)}
        width={this.props.width}
        height={this.props.height}
      />
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
