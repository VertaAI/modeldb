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
import chartStyles from 'components/Charts/ModelSummary/ModelSummary.module.css';
import { bind } from 'decko';
import ServiceFactory from 'services/ServiceFactory';

interface ILocalProps {
  height: number;
  width: number;
  marginLeft: number;
  marginTop: number;
  marginRight: number;
  marginBottom: number;
  modelId: string;
}

interface IPropsFromState {}

interface ILocalState {
  selectedFeature: string;
  possibleFeatures: string[];
  statistics: IDataStatistics;
}

type AllProps = ILocalProps & IPropsFromState;

const is_binary = (vals: number[], boundaries: number[]) => {
  if (
    boundaries[0] == 0 &&
    boundaries[1] == 0 &&
    boundaries[boundaries.length - 2] == 1 &&
    boundaries[boundaries.length - 1] == 1
  ) {
    for (var i = 2; i < boundaries.length - 2; i++) {
      if (vals[i] > 0) {
        return false;
      }
    }
    return true;
  }
  return false;
};

class DeployDataChart extends React.Component<AllProps, ILocalState> {
  ref!: SVGSVGElement;

  public constructor(props: ILocalProps) {
    super(props);

    this.state = {
      selectedFeature: '',
      possibleFeatures: [],
      statistics: {} as IDataStatistics,
    };
  }

  timeout: any = undefined;

  @bind
  dataRefresh() {
    ServiceFactory.getDeployService()
      .getDataStatistics(this.props.modelId)
      .then(stat => {
        this.setState({
          statistics: stat.data,
          possibleFeatures: [...stat.data.keys()].sort(),
        });
        if (
          this.state.selectedFeature == '' ||
          !stat.data.has(this.state.selectedFeature)
        ) {
          this.setState({ selectedFeature: [...stat.data.keys()].sort()[0] });
        }
      });
  }

  componentDidMount() {
    this.timeout = setInterval(this.dataRefresh, 60 * 1000);
    this.dataRefresh();
  }

  componentWillUnmount() {
    clearInterval(this.timeout);
  }

  fullRebuild() {
    if (this.state.statistics.size == 0 || this.state.selectedFeature == '')
      return;

    //console.log('stats', this.props.statistics)
    const width =
      this.props.width - this.props.marginLeft - this.props.marginRight;
    const height =
      this.props.height - this.props.marginTop - this.props.marginBottom;

    //const boundary = fake_data.feature1.boundaries;
    //const count = fake_data.feature1.count;

    //const featureName = this.props.statistics.keys().next().value;
    const featureName = this.state.selectedFeature;
    const featureInfo = this.state.statistics.get(
      featureName
    ) as IServiceDataFeature;

    var boundary = featureInfo.bucketLimits;
    var count = featureInfo.count;
    var reference = featureInfo.reference;
    const binary = is_binary(count, boundary);
    if (binary) {
      boundary = [0, 0.5, 1];
      count = [
        count[0] + count[1],
        count[count.length - 2] + count[count.length - 1],
      ];
      reference = [
        reference[0] + reference[1],
        reference[reference.length - 2] + reference[reference.length - 1],
      ];
    } else {
      const d_boundary = boundary[2] - boundary[1];
      boundary[0] = boundary[1] - d_boundary;
      boundary[boundary.length - 1] =
        boundary[boundary.length - 2] + d_boundary;
    }
    var reference_center = [];
    for (var i = 0; i < count.length; i++)
      reference_center.push((boundary[i] + boundary[i + 1]) / 2);

    d3.select(this.ref)
      .selectAll('g')
      .remove();

    const chart = d3
      .select(this.ref)
      .append('g')
      .attr(
        'transform',
        'translate(' + this.props.marginLeft + ',' + this.props.marginTop + ')'
      );

    const x = d3.scaleLinear().range([0, width]);

    x.domain([boundary[0], boundary[boundary.length - 1]]);

    var xAxis = d3
      .axisBottom(x)
      .tickValues(boundary)
      .tickFormat((v, index) => {
        if (binary) {
          return String(v);
        }
        if (index == 0) return '-Inf';
        if (index == boundary.length - 1) return '+Inf';
        if (v > 100) return String(Math.floor(v as number));
        return String(Math.floor((v as number) * 1000) / 1000);
      });

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
      const mid_x = boundary[i]; //(boundary[i] + boundary[i + 1]) / 2;
      const val = count[i];
      return {
        x: mid_x,
        y: val,
      };
    });

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

    const yReference = d3.scaleLinear().range([height, 0]);
    yReference.domain([0, Math.max(...reference)]);

    const line = d3
      .line()
      .x(p => x(p[0]))
      .y(p => yReference(p[1]));

    chart
      .selectAll()
      .data([
        reference_center.map((e, i) => {
          return [e, reference[i]] as [number, number];
        }),
      ])
      .enter()
      .append('path')
      .attr('fill', 'none')
      .attr('stroke', '#5fe6c9')
      .attr('stroke-width', 2)
      .attr('d', line);

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
    //console.log('here');
    this.fullRebuild();
    return (
      <React.Fragment>
        <div /*className={styles.chart_selector}*/>
          Feature :{' '}
          <select
            name="selected-metric"
            onChange={e =>
              this.setState({
                selectedFeature: (e.target as HTMLSelectElement).value,
              })
            }
            className={styles.dropdown}
          >
            {this.state.possibleFeatures.map((feature: string, i: number) => {
              return (
                <option key={feature} value={feature}>
                  {feature}
                </option>
              );
            })}
          </select>
        </div>
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
