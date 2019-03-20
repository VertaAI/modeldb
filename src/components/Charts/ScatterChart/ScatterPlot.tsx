import * as d3 from 'd3';
import { select } from 'd3-selection';
import * as React from 'react';

const wrapper = 1000;
const chartWidth = 650;
const chartHeight = 400;
const chartMargin = { top: 20, right: 5, bottom: 20, left: 35 };
const defaultRadius = 7;

interface IProps {
  data: any[];
  paramList: any;
}

export default class ScatterChart extends React.Component<IProps> {
  public extentDate: any[] = [0, 0];
  private svgRef?: SVGElement | null;

  public componentDidMount() {
    const { data, paramList } = this.props;
    this.drawScatterChart(data, paramList);
  }

  public componentWillReceiveProps(nextProps: IProps) {
    if (nextProps.data !== this.props.data) {
      const { data, paramList } = this.props;
      this.drawScatterChart(data, paramList);
    }
  }

  public drawScatterChart(data: any[], paramList: any) {
    const svg = select(this.svgRef!);
    const axisLayer = svg.append('g').classed('axis', true);
    const marksLayer = svg.append('g').classed('marks', true);

    if (data !== undefined) {
      this.extentDate = d3.extent(data, d => d.date);
    }

    const xScale = d3
      .scaleTime()
      .domain(this.extentDate)
      .range([chartMargin.left, chartWidth - chartMargin.right]);

    const yScale = d3
      .scaleLinear()
      .domain([0, 0.5])
      .range([chartHeight - chartMargin.bottom, chartMargin.top]);

    const yAxis = d3.axisLeft(yScale);
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

    [...paramList].map((param: string, i: number) => {
      const legend = svg.append('g').attr('transform', `translate(100,60)`);
      legend
        .append('circle')
        .attr('r', defaultRadius)
        .attr('cx', chartWidth + 30)
        .attr('cy', 20 * i)
        .attr('fill', param === 'val_acc' ? '#6863ff' : '#5fe6c9');
      legend
        .append('text')
        .style('font-size', '12px')
        .attr('x', chartWidth + 40)
        .attr('y', 25 * i)
        .text(param);
    });
  }

  public render() {
    return <svg width={wrapper} height={chartHeight} ref={ref => (this.svgRef = ref)} style={{ marginLeft: '40px' }} />;
  }
}
