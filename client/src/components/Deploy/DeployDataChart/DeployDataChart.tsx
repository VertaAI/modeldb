import * as React from 'react';
import { connect } from 'react-redux';
import * as d3 from 'd3';

import { IApplicationState, IConnectedReduxProps } from 'store/store';
import {
  IDataStatistics,
  IServiceDataFeature,
  IServiceStatistics,
} from 'models/Deploy';
import styles from './DeployDataChart.module.css';
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

const isBinary = (vals: number[], boundaries: number[]) => {
  if (
    boundaries[0] === 0 &&
    boundaries[1] === 0 &&
    boundaries[boundaries.length - 2] === 1 &&
    boundaries[boundaries.length - 1] === 1
  ) {
    for (let i = 2; i < boundaries.length - 2; i++) {
      if (vals[i] > 0) {
        return false;
      }
    }
    return true;
  }
  return false;
};

class DeployDataChart extends React.Component<AllProps, ILocalState> {
  public ref!: SVGSVGElement;
  public timeout: any = undefined;

  public constructor(props: ILocalProps) {
    super(props);

    this.state = {
      possibleFeatures: [],
      selectedFeature: '',
      statistics: {} as IDataStatistics,
    };
  }

  @bind
  public dataRefresh() {
    ServiceFactory.getDeployService()
      .getDataStatistics(this.props.modelId)
      .then(stat => {
        this.setState({
          possibleFeatures: [...stat.data.keys()]
            .sort()
            .filter(f => f !== '>50k'),
          statistics: stat.data,
        });
        if (
          this.state.selectedFeature === '' ||
          !stat.data.has(this.state.selectedFeature)
        ) {
          this.setState({ selectedFeature: this.state.possibleFeatures[0] });
        }
      });
  }

  public componentDidMount() {
    this.timeout = setInterval(this.dataRefresh, 60 * 1000);
    this.dataRefresh();
  }

  public componentWillUnmount() {
    clearInterval(this.timeout);
  }

  public fullRebuild() {
    if (this.state.statistics.size === 0 || this.state.selectedFeature === '') {
      return;
    }

    const width =
      this.props.width - this.props.marginLeft - this.props.marginRight;
    const height =
      this.props.height - this.props.marginTop - this.props.marginBottom;
    const featureName = this.state.selectedFeature;
    const featureInfo = this.state.statistics.get(
      featureName
    ) as IServiceDataFeature;

    let boundary = featureInfo.bucketLimits;
    let count = featureInfo.count;
    let reference = featureInfo.reference;
    const binary = isBinary(count, boundary);
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
      const dBoundary = boundary[2] - boundary[1];
      boundary[0] = boundary[1] - dBoundary;
      boundary[boundary.length - 1] = boundary[boundary.length - 2] + dBoundary;
    }
    const referenceCenter = [];
    for (let i = 0; i < count.length; i++) {
      referenceCenter.push((boundary[i] + boundary[i + 1]) / 2);
    }

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

    const x = d3.scaleLinear().range([0, width]);

    x.domain([boundary[0], boundary[boundary.length - 1]]);

    const xAxis = d3
      .axisBottom(x)
      .tickValues(boundary)
      .tickFormat((v, index) => {
        if (binary) {
          return String(v);
        }
        if (index === 0) return '-Inf';
        if (index === boundary.length - 1) return '+Inf';
        if (v > 100) return String(Math.floor(v as number));
        return String(Math.floor((v as number) * 1000) / 1000);
      });

    chart
      .append('g')
      .attr('transform', `translate(0, ${height})`)
      .call(xAxis);

    const indices = Array(count.length)
      .fill(1)
      .map((a: number, b: number) => a + b - 1);
    const data = indices.map(i => {
      const midX = boundary[i]; // (boundary[i] + boundary[i + 1]) / 2;
      const val = count[i];
      const ref = reference[i];
      return {
        ref,
        val,
        x: midX,
      };
    });
    const groupWidth = width / count.length;
    const barWidth = groupWidth * 0.4;

    function draw_bars(
      values: number[],
      color: string,
      getter: (d: any) => number,
      str: string,
      offset: number,
      addYAxis: boolean
    ) {
      const yMax = Math.max(...values);
      const y = d3
        .scaleLinear()
        .domain([0, yMax])
        .range([height, 0]);

      if (addYAxis) {
        const yAxis = d3.axisLeft(y).tickFormat(d3.format('d'));
        chart.append('g').call(yAxis);
      }

      const colorScale = d3
        .scaleLinear<d3.RGBColor>()
        .domain([0, yMax])
        .range([d3.rgb(color).brighter(), d3.rgb(color).darker()]);

      const bar = chart
        .selectAll(`.bar ${str}`)
        .data(data)
        .enter()
        .append('g')
        .attr('class', `.bar ${str}`)
        .attr('transform', d => {
          return `translate(${x(d.x) + offset * groupWidth}, ${y(getter(d))})`;
        });

      bar
        .append('rect')
        .attr('x', 1)
        .attr('width', barWidth)
        .attr('height', d => height - y(getter(d)))
        .attr('fill', d => color);
    }

    const darkColor = '#6863ff';
    const lightColor = '#5fe6c9';
    draw_bars(count, darkColor, d => d.val, '-live', 0.1, true);
    draw_bars(reference, lightColor, d => d.ref, '-ref', 0.5, false);

    const legspacing = 25;

    const legend = chart
      .selectAll('.legend')
      .data([1, 2])
      .enter()
      .append('g');

    legend
      .append('rect')
      .attr('fill', (d, i) => [darkColor, lightColor][i])
      .attr('width', 20)
      .attr('height', 20)
      .attr('y', (d, i) => {
        return 1 * legspacing - 60;
      })
      .attr('x', (d, i) => {
        return i * 220 + 80;
      });

    legend
      .append('text')
      .attr('class', 'label')
      .attr('y', (d, i) => {
        return 1 * legspacing - 46;
      })
      .attr('x', (d, i) => {
        return i * 220 + 80 + 30;
      })
      .attr('text-anchor', 'start')
      .text((d, i) => {
        return ['Live', 'Reference'][i];
      });
  }

  public render() {
    this.fullRebuild();
    return (
      <React.Fragment>
        <div className={styles.chart_selector}>
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
