import * as d3 from 'd3';

export function computeQuantiles(values: number[]) {
  let q1, q3, median, interQuantileRange, min, max;
  q1 = d3.quantile(values.sort(d3.ascending), 0.25);
  median = d3.quantile(values.sort(d3.ascending), 0.5);
  q3 = d3.quantile(values.sort(d3.ascending), 0.75);
  if (q1 && q3) {
    interQuantileRange = q3 - q1;
    min = q1 - 1.5 * interQuantileRange;
    max = q3 + 1.5 * interQuantileRange;
    return {
      q1,
      median,
      q3,
      interQuantileRange,
      min,
      max,
    };
  }
}
