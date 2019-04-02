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
import styles from './DeployDataChart.module.css';

interface ILocalProps {
  height: number;
  width: number;
  marginLeft: number;
  marginTop: number;
  marginRight: number;
  marginBottom: number;
  //statistics: IDataStatistics;
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

class DeployDataChart extends React.PureComponent<AllProps> {
  ref!: SVGSVGElement;

  componentDidMount() {
    const width =
      this.props.width - this.props.marginLeft - this.props.marginRight;
    const height =
      this.props.height - this.props.marginTop - this.props.marginBottom;

    const fake_data = {
      feature1: {
        count: Array(10)
          .fill(1)
          .map((x, y) => (x + y) * 2),
        boundaries: Array(11)
          .fill(1)
          .map((x, y) => x + y - 1),
      },
    };

    const chart = d3
      .select(this.ref)
      .append('g')
      .attr(
        'transform',
        'translate(' + this.props.marginLeft + ',' + this.props.marginTop + ')'
      );

    const boundary = fake_data.feature1.boundaries;
    const count = fake_data.feature1.count;

    const x = d3.scaleLinear().range([0, width]);
    const d_boundary = boundary[1] - boundary[0];

    x.domain([
      boundary[0] + d_boundary / 2,
      boundary[boundary.length - 1] + d_boundary / 2,
    ]);

    var xAxis = d3.axisBottom(x);

    chart
      .append('g')
      .attr('transform', 'translate(0,' + height + ')')
      .call(xAxis);

    const yMax = Math.max(...count);
    var y = d3
      .scaleLinear()
      .domain([0, yMax])
      .range([height, 0]);

    const indices = Array(count.length)
      .fill(1)
      .map((x, y) => x + y - 1);
    const data = indices.map(i => {
      const mid_x = (boundary[i] + boundary[i + 1]) / 2;
      const val = count[i];
      return {
        x: mid_x,
        y: val,
      };
    });
    console.log('indices', indices);

    const color = '#6863ff';
    var colorScale = d3
      .scaleLinear<d3.RGBColor>()
      .domain([0, yMax])
      .range([d3.rgb(color).brighter(), d3.rgb(color).darker()]);

    var bar = chart
      .selectAll('.bar')
      .data(data)
      .enter()
      .append('g')
      .attr('class', 'bar')
      .attr('transform', function(d) {
        return 'translate(' + x(d.x) + ',' + y(d.y) + ')';
      });

    bar
      .append('rect')
      .attr('x', 1)
      .attr('width', width / count.length)
      .attr('height', d => height - y(d.y))
      .attr('fill', d => colorScale(d.y).toString());

    var formatCount = d3.format(',.0f');
    bar
      .append('text')
      .attr('dy', '.75em')
      .attr('y', -12)
      .attr('x', width / (count.length * 2))
      .attr('text-anchor', 'middle')
      .attr('fill', '#999999')
      .attr('font', 'sans-serif')
      .attr('font-size', '10px')
      .text(d => formatCount(d.y));
  }

  render() {
    console.log('here');
    return (
      <React.Fragment>
        <svg
          className={`container ${styles.chart}`}
          ref={(ref: SVGSVGElement) => (this.ref = ref)}
          shapeRendering={'optimizeQuality'}
          width={this.props.width}
          height={this.props.height}
        />
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

export default connect(mapStateToProps)(DeployDataChart);
