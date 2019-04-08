import React, { Component } from 'react';
import * as d3 from 'd3';
const width = 800;
const height = 480;
const margin = { top: 45, right: 35, bottom: 25, left: 45 };

class ParallelCoordinates extends Component {
  state = {
    parallel: [],
  };

  getDerivedStateFromProps(nextProps, prevState) {
    const { data } = nextProps;
    if (!data) return {};
    return { parallel: data };
  }
  componentDidMount() {
    const data = this.props.data;
    let svg = d3
      .select(this._rootNode)
      .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
    const chart_width = width - margin.left - margin.right;
    const chart_height = height - margin.top - margin.bottom;

    // var x = d3.scaleOrdinal().rangePoints([0, chart_width], 1),
    var x = d3
        .scalePoint()
        .range([0, chart_width])
        .padding(0.1),
      y = {},
      dragging = {};

    var line = d3.line(),
      axis = d3.axisLeft(),
      background,
      foreground;

    // Extract the list of dimensions and create a scale for each.
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
    x.domain(
      (dimensions = d3.keys(data[0]).filter(function(d) {
        return (y[d] = d3
          .scaleLinear()
          .domain(
            d3.extent(data, function(p) {
              return +p[d];
            })
          )
          .range([chart_height, 0]));
      }))
    );

    console.log(this.props.data);
    console.log(dimensions);
    console.log(x.domain);

    // Add grey background lines for context.
    background = svg
      .append('g')
      .attr('class', 'background')
      .selectAll('path')
      .data(data)
      .enter()
      .append('path')
      .attr('d', path);

    // Add blue foreground lines for focus.
    foreground = svg
      .append('g')
      .attr('class', 'foreground')
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
            background.attr('visibility', 'hidden');
          })
          .on('drag', function(d) {
            dragging[d] = Math.min(width, Math.max(0, d3.event.x));
            foreground.attr('d', path);
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
            transition(foreground).attr('d', path);
            background
              .attr('d', path)
              .transition()
              .delay(500)
              .duration(0)
              .attr('visibility', null);
          })
      );
    g.append('text')
      .style('text-anchor', 'middle')
      .attr('y', -9)
      .text(function(d) {
        return d;
      });

    // Add an axis and title.
    g.append('g')
      .attr('class', 'axis')
      .each(function(d) {
        d3.select(this).call(d3.axisLeft(y[d]));
      })
      .append('text')
      .style('text-anchor', 'middle')
      .attr('y', -9)
      .text(function(d) {
        return d;
      });

    // Add and store a brush for each axis.
    // g.append('g')
    //   .attr('class', 'brush')
    //   .each(function(d) {
    //     d3.select(this).call(
    //       (y[d].brush = d3.svg
    //         .brush()
    //         .y(y[d])
    //         .on('brushstart', brushstart)
    //         .on('brush', brush))
    //     );
    //   })
    //   .selectAll('rect')
    //   .attr('x', -8)
    //   .attr('width', 16);

    // Add and store a brush for each axis.
    //   g.append("g")
    //   .attr("class", "brush")
    //   .each(function(d) {
    //     d3.select(this).call(y[d].brush = d3.svg.brush().y(y[d]).on("brushstart", brushstart).on("brush", brush));
    //   })
    // .selectAll("rect")
    //   .attr("x", -8)
    //   .attr("width", 16);
    // });

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

    // Handles a brush event, toggling the display of foreground lines.
    function brush() {
      var actives = dimensions.filter(function(p) {
          return !y[p].brush.empty();
        }),
        extents = actives.map(function(p) {
          return y[p].brush.extent();
        });
      foreground.style('display', function(d) {
        return actives.every(function(p, i) {
          return extents[i][0] <= d[p] && d[p] <= extents[i][1];
        })
          ? null
          : 'none';
      });
    }

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
