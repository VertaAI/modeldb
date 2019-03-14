import * as d3 from 'd3';
import React from 'react';
import ModelRecord from '../../../models/ModelRecord';

const width = 650;
const height = 400;
const margin = { top: 20, right: 5, bottom: 20, left: 35 };

interface ILocalProps {
  chartData: any[];
}

export default class SummaryChart extends React.Component<ILocalProps> {
  public state = {
    xAxis: {},
    yAxis: {}
  };

  public xAxis: any; //  = d3.axisBottom()
  public yAxis: any;
  public xAxisName: string = 'xAxis';
  public yAxisName: string = 'yAxis';
  public extentDate: any[] = [0, 0];

  public componentDidMount() {
    const { chartData } = this.props;
    const tickFormatX: any = d3.timeFormat('%b');
    const tickFormatY: any = (d: any) => `${d}℉`;
    if (!chartData) return {};

    if (chartData !== undefined) {
      this.extentDate = d3.extent(chartData, d => d.date);
    }

    // const extent = d3.extent(chartData, d => d.date);
    const xScale = d3
      .scaleTime()
      .domain(this.extentDate)
      .range([margin.left, width - margin.right]);

    this.xAxis = d3
      .axisBottom(
        d3
          .scaleTime()
          .domain(this.extentDate)
          .range([margin.left, width - margin.right])
      )
      .tickFormat(tickFormatX);
    this.setState({ xAxis: this.xAxis });

    this.yAxis = d3
      .axisLeft(
        d3
          .scaleLinear()
          .domain([0, 1])
          .range([height - margin.bottom, margin.top])
      )
      .tickFormat(tickFormatY);
    this.setState({ yAxis: this.yAxis });
  }

  // const marks = data.map(d => {
  //   const isColored = !range.length || (range[0] <= d.date && d.date <= range[1]);
  //   return {
  //     x: xScale(d.date),
  //     y: yScale(d.high),
  //     r: 12,
  //     fill: isColored ? colorScale(d.avg) : '#ccc'
  //   };
  // });

  // return { marks, xScale, yScale };

  // componentDidMount() {
  //   this.brush = d3
  //     .brushX()
  //     .extent([[margin.left, margin.top], [width - margin.right, height - margin.bottom]])
  //     .on('end', this.brushEnd);
  //   d3.select(this.refs.brush).call(this.brush);
  // }

  public componentDidUpdate() {
    // this.xAxis.scale(this.xScale);
    d3.select(this.xAxisName).call(this.xAxis);
    d3.select(this.yAxisName).call(this.yAxis);
  }

  // brushEnd = () => {
  //   if (!d3.event.selection) {
  //     this.props.updateRange([]);
  //     return;
  //   }
  //   const [x1, x2] = d3.event.selection;
  //   const range = [this.state.xScale.invert(x1), this.state.xScale.invert(x2)];

  //   this.props.updateRange(range);
  // };

  public render() {
    {
      console.log(this.state);
      console.log(JSON.stringify(this.props.chartData));
    }
    return (
      <svg width={width} height={height}>
        {/* {this.state.chartData.map((d, i) => (
          <circle key={i} x={d.x} y={d.y} r={12} fill={d.fill} />
        ))} */}
        <g>
          <g ref={this.xAxisName} style={{ color: '#444', fill: '#444' }} transform={`translate(0, ${height - margin.bottom})`} />
          <g ref={this.yAxisName} style={{ color: '#444', fill: '#444' }} transform={`translate(${margin.left}, 0)`} />
          {/* <g ref="brush" /> */}
        </g>
      </svg>
    );
  }
}

// public static getDerivedStateFromProps(nextProps: any, prevState: any) {
//   const { data, range } = nextProps;
//   if (!data) return {};
//   console.log(data);
//   // console.log()
//   const damal = 'damal damal';
//   if (nextProps.damal !== prevState.damal) {
//     return { damal: nextProps.damal };
//   }

//   return { damal };
// }

// damal: 'mal'
