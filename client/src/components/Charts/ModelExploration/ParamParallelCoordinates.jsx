import React, { Component } from 'react';
import * as d3 from 'd3';

import { errorMessage } from 'utils/ChartHelpers';

const width = 680;
const height = 360;
const margin = { top: 75, right: 45, bottom: 35, left: 50 };

class ParallelCoordinates extends Component {
  componentDidMount() {
    const data = this.props.data;
    const metricList = this.props.metricList;
    let svg = d3
      .select(this._rootNode)
      .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
    const chart_width = width - margin.left - margin.right;
    const chart_height = height - margin.top - margin.bottom;

    if (data === undefined || data.length === 0) {
      errorMessage(
        this._rootNode,
        width,
        margin.left,
        height,
        'notAvailableMsg',
        'data not available',
        '\uf071'
      );
    }

    var x = d3
        .scalePoint()
        .range([0, chart_width])
        .padding(0.1),
      y = {},
      dragging = {};

    var line = d3.line(),
      lines;

    let dimensions = d3.keys(data[0]).filter(function(d) {
      const [min, max] = d3.extent(data, function(p) {
        return +p[d];
      });
      return (y[d] = d3
        .scaleLinear()
        .domain([min * 0.95, max * 1.05])
        .range([chart_height, 0]));
    });
    x.domain(dimensions);

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
      .attr('y', -30)
      .text(function(d) {
        return d;
      });

    g.append('rect')
      .attr('width', 90)
      .attr('height', height - margin.top)
      .attr('rx', '5')
      .attr('ry', '5')
      .attr('transform', `translate(-50,${-margin.top / 4} )`)
      .attr('class', function(d) {
        if (metricList.has(d)) {
          return 'metricAxis';
        }
        return 'hyperAxis';
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

    lines = svg
      .append('g')
      .attr('class', 'parallelLines')
      .selectAll('path')
      .data(data)
      .enter()
      .append('path')
      .attr('d', path);

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
        g.selectAll('.brush').each(function(d) {
          d3.select(this)
            .select('.selection')
            .attr('style', 'display:none');
        });
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
