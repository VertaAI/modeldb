import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 800;
const height = 360;
const margin = { top: 65, right: 35, bottom: 35, left: 45 };

class ParallelCoordinates extends Component {
  componentDidMount() {
    const data = this.props.data;
    let svg = d3
      .select(this._rootNode)
      .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
    const chart_width = width - margin.left - margin.right;
    const chart_height = height - margin.top - margin.bottom;

    var x = d3
        .scalePoint()
        .range([0, chart_width])
        .padding(0.1),
      y = {},
      dragging = {};

    var line = d3.line(),
      lines;

    let dimensions = d3.keys(data[0]).filter(function(d) {
      return (y[d] = d3
        .scaleLinear()
        .domain(
          d3.extent(data, function(p) {
            return +p[d];
          })
        )
        .range([chart_height, 0]));
    });
    x.domain(dimensions);

    lines = svg
      .append('g')
      .attr('class', 'parallelLines')
      .selectAll('path')
      .data(data)
      .enter()
      .append('path')
      .attr('d', path);

    // Add a group element for each dimension.
    var g = svg
      .selectAll('.dimension')
      .data(dimensions)
      .enter()
      .append('g')
      .attr('class', 'dimension')
      .attr('transform', function(d) {
        return 'translate(' + x(d) + ')';
      })
      .call(
        d3
          .drag()
          .subject(function(d) {
            return { x: x(d) };
          })
          .on('start', function(d) {
            dragging[d] = x(d);
          })
          .on('drag', function(d) {
            dragging[d] = Math.min(width, Math.max(0, d3.event.x));
            lines.attr('d', path);
            dimensions.sort(function(a, b) {
              return position(a) - position(b);
            });
            x.domain(dimensions);
            g.attr('transform', function(d) {
              return 'translate(' + position(d) + ')';
            });
          })
          .on('end', function(d) {
            delete dragging[d];
            transition(d3.select(this)).attr(
              'transform',
              'translate(' + x(d) + ')'
            );
            transition(lines).attr('d', path);
          })
      );
    g.append('text')
      .style('text-anchor', 'middle')
      .attr('y', -20)
      .text(function(d) {
        return d;
      });

    // Add axis
    g.append('g')
      .attr('class', 'axis')
      .each(function(d) {
        d3.select(this).call(d3.axisLeft(y[d]).ticks(6));
      });

    // Add and store a brush for each axis.
    g.append('g')
      .attr('class', 'brush')
      .each(function(d) {
        d3.select(this).call(
          (y[d].brush = d3
            .brushY()
            .extent([[-8, y[d].range()[1]], [8, y[d].range()[0]]])
            .on('start', brushstart)
            .on('brush', brush)
            .on('end', brushend))
        );
      })
      .selectAll('rect')
      .attr('x', -8)
      .attr('width', 16);

    function position(d) {
      var v = dragging[d];
      return v == null ? x(d) : v;
    }

    function transition(g) {
      return g.transition().duration(500);
    }

    // Returns the path for a given data point.
    function path(d) {
      return line(
        dimensions.map(function(p) {
          return [position(p), y[p](d[p])];
        })
      );
    }

    function brushstart() {
      d3.event.sourceEvent.stopPropagation();
    }
    function brushend() {
      const sel = d3.event.selection;
      if (sel == null) {
        lines.classed('fade', false);
      }
    }

    function brush() {
      let actives = [];
      g.selectAll('.brush')
        .filter(function(d) {
          return d3.brushSelection(this);
        })
        .each(function(d) {
          actives.push({
            dimension: d,
            extent: d3.brushSelection(this),
          });
        });

      lines.classed('fade', function(d, i) {
        return !actives.every(function(active) {
          var dim = active.dimension;
          return (
            active.extent[0] <= y[dim](d[dim]) &&
            y[dim](d[dim]) <= active.extent[1]
          );
        });
      });
    }
  }

  _setRef(componentNode) {
    this._rootNode = componentNode;
  }

  render() {
    return (
      <div>
        <svg
          className="parallelChart"
          width={width}
          height={height}
          ref={this._setRef.bind(this)}
        />
      </div>
    );
  }
}

export default ParallelCoordinates;

// svg
//   .append('text')
//   .attr('id', 'chart-title')
//   .text('Skills Aquired Over My Graduate Study')
//   .style('font-size', 24)
//   .style('font-weight', 'bold')
//   .attr('fill', '#e1e1e1')
//   .attr('transform', 'translate(' + 400 + ',' + 35 + ')');

// let x = d3
//     .scaleBand()
//     .range([0, 2 * pi])
//     .align(0),
//   y = d3.scaleLinear().range([innerRadius, outerRadius]),
//   z = d3
//     .scaleOrdinal()
//     .range([
//       '#882d1f',
//       '#d0743c',
//       '#c9e0ed',
//       '#6098ba',
//       '#104170',
//       '#185085',
//     ]);

// x.domain(data.map(d => d.Date));
// z.domain(data_columns);

// g.append('circle')
//   .attr('r', outerRadius)
//   .on('mouseover', d => {
//     svg.selectAll('.annot-markers').attr('opacity', 0);
//     svg.selectAll('.annot-text').attr('opacity', 0);
//   })
//   .on('mouseout', d => {
//     svg.selectAll('.annot-markers').attr('opacity', 1);
//     svg.selectAll('.annot-text').attr('opacity', 1);
//   })
//   .attr('fill', 'none');

// g.append('g')
//   .on('mouseover', d => {
//     svg.selectAll('.annot-markers').attr('opacity', 0);
//     svg.selectAll('.annot-text').attr('opacity', 0);
//     svg.selectAll('.annot-yrs').attr('opacity', 0);
//   })
//   .on('mouseout', d => {
//     svg.selectAll('.annot-markers').attr('opacity', 1);
//     svg.selectAll('.annot-text').attr('opacity', 1);
//     svg.selectAll('.annot-yrs').attr('opacity', 1);
//   })
//   .selectAll('g')
//   .data(
//     d3
//       .stack()
//       .keys(data_columns)
//       .offset(d3.stackOffsetExpand)(data)
//   )
//   .enter()
//   .append('g')
//   .attr('fill', d => z(d.key))

//   .selectAll('path')
//   .data(d => d)
//   .enter()
//   .append('path')
//   .attr(
//     'd',
//     d3
//       .arc()
//       .innerRadius(d => y(d[0]))
//       .outerRadius(d => y(d[1]))
//       .startAngle(d => x(d.data.Date))
//       .endAngle(d => x(d.data.Date) + x.bandwidth())
//       .padAngle(0.05)
//       .padRadius(innerRadius)
//   )
//   .style('cursor', 'pointer')
//   .on('mouseover', d => {
//     d3.select('#arc_' + d.data.Date.slice(-6)).attr('opacity', 1);
//     d3.select('#txt_' + d.data.Date.slice(-6)).style('fill', '#444');
//     d3.select('#txtYr_' + d.data.Date.slice(-6)).attr('opacity', 1);
//     const data = Object.keys(d.data)
//       .filter(key => data_columns.includes(key))
//       .reduce((obj, key) => {
//         obj[key] = d.data[key];
//         return obj;
//       }, {});

//     g.selectAll('.lg-marker')
//       .data(Object.values(data).reverse())
//       .text(d => d + '%');
//     g.selectAll('.lg-marker').attr('opacity', 1);
//   })
//   .on('mouseout', d => {
//     d3.select('#arc_' + d.data.Date.slice(-6)).attr('opacity', 0);
//     d3.select('#txt_' + d.data.Date.slice(-6)).style('fill', '#c1c1c1');
//     d3.select('#txtYr_' + d.data.Date.slice(-6)).attr('opacity', 0);
//     g.selectAll('.lg-marker').attr('opacity', 0);
//   });

// testing
// Handles a brush event, toggling the display of foreground lines.

// var actives_set = new Set();
// function brush() {
//   // var s = d3.event.selection;
//   // console.log(this);
//   let parent = this.parentNode.children[0].innerHTML;
//   let actives = dimensions.filter(function(p) {
//       // console.log(d3.brushSelection(this));
//       // return !y[p].brush.empty();
//       // console.log(parent);
//       // console.log(p);
//       // console.log(!(parent === p));
//       return parent === p;
//     }),
//     extents = actives.map(function(p) {
//       return y[p].brush.extent();
//     });
//   actives_set.add(actives[0]);
//   // console.log(Array.from(actives_set));
//   foreground.style('display', function(d) {
//     return Array.from(actives_set).every(function(p, i) {
//       // console.log(p);
//       return extents[i][0] <= d[p] && d[p] <= extents[i][1];
//     })
//       ? null
//       : 'none';
//   });
// }
