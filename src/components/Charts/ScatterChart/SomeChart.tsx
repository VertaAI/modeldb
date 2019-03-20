import * as d3 from 'd3';
import { select } from 'd3-selection';
import * as React from 'react';
import { polygonLength } from 'd3';

const wrapper = 1000;
const chartWidth = 650;
const chartHeight = 400;
const chartMargin = { top: 20, right: 5, bottom: 20, left: 35 };
const defaultRadius = 7;
const easeType = d3.easeQuad;
const easeDuration = 700;
let marks: any;

interface IProps {
  data: any[];
  selectedMetric: string;
}

export default class ScatterChart extends React.Component<IProps> {
  public extentDate: any[] = [0, 0];
  private svgRef?: SVGElement | null;

  public componentDidMount() {
    const { data, selectedMetric } = this.props;
    this.drawScatterChart(data, selectedMetric);
  }

  public componentWillReceiveProps(nextProps: IProps) {
    if (nextProps.data !== this.props.data) {
      const { data, selectedMetric } = nextProps;
      this.drawScatterChart(data, selectedMetric);
    }
  }

  public drawScatterChart(data: any[], selectedMetric: string) {
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
      .domain([0, 1])
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

    // if (marks !== undefined) {
    //   marks = marks.selectAll('.dot').remove();
    //   console.log(marks);
    //   const el = document.getElementsByClassName('dot')[0];
    //   console.log(el.parentElement);
    //   if (el && el !== null) {
    //     el.parentNode.removeChild(el);
    //     console.log(el.parentNode);
    //   }
    // }

    if (marks !== undefined) {
      console.log(marks);
      console.log(marks.selectAll('dot'));
      marks.exit().remove();
    }

    marks = marksLayer.selectAll('.dot').data(data);
    // marks.transition().remove();
    // marks.exit().remove();
    marks
      .enter()
      .append('circle')
      .classed('dot', true)
      .attr('r', defaultRadius)
      .attr('fill', '#6863ff')
      .style('opacity', 0.8)
      .attr('cx', (d: any) => xScale(d.date))
      .attr('cy', (d: any) => yScale(d[selectedMetric]));

    // marks
    //   .transition()
    //   .ease(easeType)
    //   .attr('cx', (d: any) => xScale(d.date))
    //   .attr('cy', (d: any) => yScale(d[selectedMetric]));

    // indha opoochi ya pathi marandhuru da
    // / tapunu poi indha problem solve panita nama real destination reach panirama la
    // adha solren paru pathu panu

    // idhua yena da kpodumai ya iruku
    // yedho periya array and reverse order la vardhu polygonLength
    //  mkm
  }

  public render() {
    return <svg width={wrapper} height={chartHeight} ref={ref => (this.svgRef = ref)} style={{ marginLeft: '40px' }} />;
  }
}
