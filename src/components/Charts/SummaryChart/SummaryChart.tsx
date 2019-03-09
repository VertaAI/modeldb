import * as d3 from 'd3';
import React from 'react';
import ModelRecord from '../../../models/ModelRecord';

const width = 650;
const height = 400;
const margin = { top: 20, right: 5, bottom: 20, left: 35 };

interface ILocalProps {
  chartData: ModelRecord[] | undefined;
}

export default class SummaryChart extends React.Component<ILocalProps> {
  public xAxis: any;
  // = d3.axisBottom().tickFormat(d3.timeFormat('%b'));
  public yAxis: any;
  public extentDate: Date[] | number[] = [0, 0];
  // = d3.axisLeft().tickFormat(d => `${d}℉`);

  // public componentWillReceiveProps() {
  //   const { chartData } = this.props;
  //   if (!chartData) return {};

  //   if (chartData !== undefined) {
  //     this.extentDate = d3.extent(chartData, d => d.dateCreated);
  //   }

  //   const xScale = d3
  //     .scaleTime()
  //     .domain(extent)
  //     .range([margin.left, width - margin.right]);

  //   const [min, max] = d3.extent(data, d => d.high);
  //   const yScale = d3
  //     .scaleLinear()
  //     .domain([Math.min(min, 0), max])
  //     .range([height - margin.bottom, margin.top]);

  //   const colorExtent = d3.extent(data, d => d.avg).reverse();
  //   const colorScale = d3
  //     .scaleSequential()
  //     .domain(colorExtent)
  //     .interpolator(d3.interpolateRdYlBu);

  //   const marks = data.map(d => {
  //     const isColored = !range.length || (range[0] <= d.date && d.date <= range[1]);
  //     return {
  //       x: xScale(d.date),
  //       y: yScale(d.high),
  //       r: 12,
  //       fill: isColored ? colorScale(d.avg) : '#ccc'
  //     };
  //   });

  //   return { marks, xScale, yScale };
  // }

  // componentDidMount() {
  //   this.brush = d3
  //     .brushX()
  //     .extent([[margin.left, margin.top], [width - margin.right, height - margin.bottom]])
  //     .on('end', this.brushEnd);
  //   d3.select(this.refs.brush).call(this.brush);
  // }

  // componentDidUpdate() {
  //   this.xAxis.scale(this.state.xScale);
  //   d3.select(this.refs.xAxis).call(this.xAxis);
  //   this.yAxis.scale(this.state.yScale);
  //   d3.select(this.refs.yAxis).call(this.yAxis);
  // }

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
      console.log(this.props.chartData);
    }
    return (
      <div> ccd </div>
      // <svg width={width} height={height}>
      //   {this.state.chartData.map((d, i) => (
      //     <circle key={i} x={d.x} y={d.y} r={12} fill={d.fill} />
      //   ))}
      //   <g>
      //     <g ref="xAxis" transform={`translate(0, ${height - margin.bottom})`} />
      //     <g ref="yAxis" transform={`translate(${margin.left}, 0)`} />
      //     <g ref="brush" />
      //   </g>
      // </svg>
    );
  }
}
