import * as React from 'react';
import { connect } from 'react-redux';
import * as d3 from 'd3';

import { IApplicationState, IConnectedReduxProps } from 'store/store';

interface ILocalProps {}

interface IPropsFromState {}

type AllProps = ILocalProps;

class DeployServiceChart extends React.PureComponent<AllProps> {
  ref!: SVGSVGElement;

  componentDidMount() {
    d3.select(this.ref)
      .append('circle')
      .attr('r', 5)
      .attr('cx', 10 / 2)
      .attr('cy', 10 / 2)
      .attr('fill', 'red');
  }

  render() {
    return (
      <svg
        className="container"
        ref={(ref: SVGSVGElement) => (this.ref = ref)}
        width={10}
        height={10}
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
