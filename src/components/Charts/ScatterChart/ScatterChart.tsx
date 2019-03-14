import * as d3 from 'd3';
import { select } from 'd3-selection';
import * as React from 'react';

const chartWidth = 650;
const chartHeight = 400;
const chartMargin = { top: 20, right: 5, bottom: 20, left: 35 };
const defaultRadius = 10;

interface IProps {
  data: any[];
  // width: number = chartWidth;
  // height: number = chartHeight;
  // margin: object = chartMargin;
}

export default class ScatterChart extends React.Component<IProps> {
  public extentDate: any[] = [0, 0];
  private svgRef?: SVGElement | null;

  public componentDidMount() {
    const { data } = this.props; //  width, height, margin
    this.drawScatterChart(data);
  }

  public componentWillReceiveProps(nextProps: IProps) {
    if (nextProps.data !== this.props.data) {
      const { data } = this.props;
      this.drawScatterChart(data);
    }
  }

  public drawScatterChart(data: any[]) {
    // width: number, height: number, margin: object
    const svg = select(this.svgRef!);
    const axisLayer = svg.append('g').classed('axis', true);
    const marksLayer = svg.append('g').classed('marks', true);

    if (data !== undefined) {
      this.extentDate = d3.extent(data, d => d.date);
    }
    // console.log(this.extentDate[0]);
    // console.log(this.extentDate[0].getMonth());
    // console.log(this.extentDate[0].getMonth() - 1);

    // const Val1 = this.extentDate[0].setMonth(this.extentDate[0].getMonth() - 1);
    // const Val2 = this.extentDate[1].setMonth(this.extentDate[1].getMonth() + 1);

    const xScale = d3
      .scaleTime()
      .domain(this.extentDate)
      .range([chartMargin.left, chartWidth - chartMargin.right]);

    const yScale = d3
      .scaleLinear()
      .domain([0, 0.5])
      .range([chartHeight - chartMargin.bottom, chartMargin.top]);

    const yAxis = d3.axisLeft(yScale);
    // const yAxis = d3.axisLeft(yScale);
    // .tickFormat(tickFormatX);
    const xAxis = d3.axisBottom(xScale);

    axisLayer
      .append('g')
      .attr('class', 'yaxis')
      .attr('transform', `translate(${chartMargin.left},0)`)
      .call(yAxis);

    axisLayer
      .append('g')
      .attr('class', 'xaxis')
      .attr('transform', `translate(0,${chartHeight - chartMargin.bottom})`)
      .call(xAxis);

    const marks = marksLayer.selectAll('.dot').data(data);
    marks
      .enter()
      .append('circle')
      .classed('dot', true)
      .attr('r', defaultRadius)
      .attr('fill', '#6863ff')
      .style('opacity', 0.6)
      .attr('cx', d => xScale(d.date))
      .attr('cy', d => yScale(d.val_acc));

    const marks2 = marksLayer.selectAll('.dot2').data(data);
    marks2
      .enter()
      .append('circle')
      .classed('dot2', true)
      .attr('r', defaultRadius)
      .attr('fill', '#5fe6c9')
      .style('opacity', 0.6)
      .attr('cx', d => xScale(d.date))
      .attr('cy', d => 1.15 * yScale(d.val_acc));
  }

  public render() {
    return <svg width={chartWidth} height={chartHeight} ref={ref => (this.svgRef = ref)} style={{ marginLeft: '40px' }} />;
  }
}
