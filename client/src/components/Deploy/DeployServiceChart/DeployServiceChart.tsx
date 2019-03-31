import * as React from 'react';
import { connect } from 'react-redux';
import * as d3 from 'd3';

import { IApplicationState, IConnectedReduxProps } from 'store/store';

interface ILocalProps {
  height: number;
  width: number;
  marginLeft: number;
  marginTop: number;
  maginRight: number;
  marginBottom: number;
}

interface IPropsFromState {}

type AllProps = ILocalProps;

class DeployServiceChart extends React.PureComponent<AllProps> {
  ref!: SVGSVGElement;

  componentDidMount() {
    const width =
      this.props.width - this.props.marginLeft - this.props.maginRight;
    const height =
      this.props.height - this.props.marginTop - this.props.marginBottom;
    const x = d3
      .scaleLinear()
      .domain([1910, 2010])
      .range([0, width]);
    const y = d3
      .scaleLinear()
      .domain([0, 40000000])
      .range([height, 0]);

    const chart = d3
      .select(this.ref)
      .append('g')
      .attr(
        'transform',
        'translate(' + this.props.marginLeft + ',' + this.props.marginTop + ')'
      );

    // Add axes
    const xAxis = d3.axisBottom(x).tickFormat(d3.format('.4'));
    const yAxis = d3.axisLeft(y).tickFormat(d3.format('.2s'));
    chart.append('g').call(yAxis);
    chart
      .append('g')
      .attr('transform', 'translate(0,' + height + ')')
      .call(xAxis);

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
