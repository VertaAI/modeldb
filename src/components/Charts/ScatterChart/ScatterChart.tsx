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
  // width: number = chartWidth;
  // height: number = chartHeight;
  // margin: object = chartMargin;
}

export default class ScatterChart extends React.Component<IProps> {
  public extentDate: any[] = [0, 0];
  private svgRef?: SVGElement | null;

  public componentDidMount() {
    const { data, paramList } = this.props; //  width, height, margin
    this.drawScatterChart(data, paramList);
  }

  public componentWillReceiveProps(nextProps: IProps) {
    if (nextProps.data !== this.props.data) {
      const { data, paramList } = this.props;
      this.drawScatterChart(data, paramList);
    }
  }

  public drawScatterChart(data: any[], paramList: any) {
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
    // unoda yela feature wan la irundhu copy apo yena dha poi kalata poreyo therila

    // sami potu savadikra da
    // seri apo onum periya solution ila idhuku, ore vali ipo yena na poi palar nu aparadhu dhan

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

  /// makrs ipo update pananumay
  // indha poochi payan vera type adichute poitu irukan patu patu nu
  // namaku vechu seium pola iruke, end of the day namaku innum onumay mudiayala seri papom
  // apppa sona mari oofer ku poi apiru
  // adha ore vali

  public render() {
    return <svg width={wrapper} height={chartHeight} ref={ref => (this.svgRef = ref)} style={{ marginLeft: '40px' }} />;
  }
}

// public makeFlatArray = (records: ModelRecord[]) => {
//   return _.map( records, obj => {
//     const
//   })
// }

// the constraint of x axis is that it should be a category to render a y aggrigate so check for the type of it
// the four categories are :
//        -> categories in outer variables
//        -> inner vars has 2 types - numeric and categorical although some might have num as the value

// our plan was to use catigorical to summarize with aggregation
// but to ward out the numerical entity is the task,

// so num I need to gte a list of variables to be whitelisted as fine one
// they seem to have everything as a category
// now
// for dataset the path is unique item ilaya which is the categorical value
// ppppo we have two types of file types
// the problem with numeric data ie continuous data is that it cannot be plotted in charts of bar etc so yena panlam
// therilaye boss
// seri imagign the continuous is takle care of then figure out a result for other categories in the slicing and dicing
// coz you need to prepare an agggrigate for sure now or it wont work
// sema mass ipo paren
// now then ipo yena pana porom

// this computation should get bigger over time la
// apo dha idhu veliya pogum or adhu apdiye ua vandhurum
// yaxis jus assign to y variable
// and for the yaxis get the list of hyperboys
// if it is from hyperparams then ok but if they r from other
// so ipo nama or black box pana porom which can slap in the form of a module and be called as a function
// then send it dynamically the inouts at all times
// flact array os the solution

// flat array for all columns to render and rerender

// yaxis and xaxis
// y axis is the same for both the charts
// so ipo yena panlam
// now you need to aggregate all the rows in the hyperparameters
// yena panlam
// bet a better way to do data updation in d3 examples as I see there are good ways that we dont fully understand now so lets go with that
// date and keys for the values  seri papom indha pakam vandha solu nu soli vita
// ivanuga yena dha pesikranuga nu therilaye this is a flat array ilaya
//  so       / namakum flat boy irukan // indha flat vechu  yen pana if the data changes apo dhana namaku new item varum
// basically  oru new set of data ula varum la , bassed on selection the variable itself changes
// fuck looks like we have a very nice way of doing the same thisg
// sema da
// ipo ni poi tap tappu nu vangu ok va
// apo next paycheck la 5k va complete pani vechuko
// pathukala
// idhula yena matter na the value of the coin can skyrockt beacure it is capped to 21milliom
// so idhu oru nalla usecase
// yevano vithurukan but perusa vikala just namala yenamath vikran
// randomly oru amount potu buy order pani vidu sandy pathukalam
// namaku yena

// 1.349 / 0.00114    1.349 / 0.0012 paru 86 dha difference so adichu thuku

// indha coin yen yenaku pidichuruku na pathuko iva oru vidyasamanavan dha adha solren
// ivan good ra sandy so idhe vechukalam

// somehow we need to curb the number of items in the category so may be not less than 6/8 is a constraint to impose
