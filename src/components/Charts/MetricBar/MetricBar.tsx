import * as d3 from 'd3';
import { select } from 'd3-selection';
import * as React from 'react';

const chartWidth = 115;
const chartHeight = 300;
const chartMargin = { top: 20, right: 5, bottom: 30, left: 35 };
const defaultRadius = 3;

interface IProps {
  data: any;
}

export default class ScatterChart extends React.Component<IProps> {
  public extentDate: any[] = [0, 0];
  private svgRef?: SVGElement | null;

  public componentDidMount() {
    const { data } = this.props;
    this.drawScatterChart(data);
  }

  public drawScatterChart(param: string) {
    const svg = select(this.svgRef!);
    const axisLayer = svg.append('g').classed('axis', true);
    const marksLayer = svg.append('g').classed('marks', true);
    const data = { key: param, value: Math.random() };

    const xScale = d3
      .scaleLinear()
      .domain([0, 1])
      .range([chartMargin.left, chartWidth - chartMargin.right]);

    const yScale = d3
      .scaleLinear()
      .domain([0, 1])
      .range([chartHeight - chartMargin.bottom, chartMargin.top]);

    const yAxis = d3.axisLeft(yScale);
    const xAxis = d3.axisBottom(xScale).ticks(3);

    axisLayer
      .append('g')
      .attr('class', 'yaxis')
      .attr('transform', `translate(${chartMargin.left},0)`)
      .call(yAxis);

    const xTicks = axisLayer.append('g').attr('class', 'bar-xaxis');
    xTicks.attr('transform', `translate(0,${chartHeight - chartMargin.bottom})`).call(xAxis);
    xTicks.selectAll('text').style('fill', '#fff');

    axisLayer
      .append('text')
      .attr('transform', `translate(${chartWidth / 2 + chartMargin.left / 2},${chartHeight - 10})`)
      .style('text-anchor', 'middle')
      .style('font-size', '12px')
      .text(param);

    marksLayer
      .append('rect')
      .attr('fill', '#5fe6c9')
      .attr('x', xScale(0.5) - 10)
      .attr('y', yScale(data.value) - 30)
      .attr('width', 20)
      .attr('height', chartHeight - yScale(data.value));
  }

  public render() {
    return <svg width={chartWidth} height={chartHeight} ref={ref => (this.svgRef = ref)} />;
  }
}
