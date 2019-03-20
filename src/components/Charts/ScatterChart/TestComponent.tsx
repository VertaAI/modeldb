import * as d3 from 'd3';
import { select } from 'd3-selection';
import * as React from 'react';

// var Chart = React.createClass({
//     render: function() {
//         return (
//              <svg width={this.props.width}
//                  height={this.props.height} >
//               {this.props.children}
//             </svg>
//         );
//     }
// });

// // extends React.Component<IProps> {

// // some basic data
// var all = [/* some data */];
// var filtered = [/* some data */];

// var App = React.createClass({
//     getDefaultProps: function() {
//         return {
//           width: 500,
//           height: 500
//         }
//     },

//     getInitialState: function() {
//         return {
//           data: all
//         }
//     },

//     showAll: function() {
//       this.setState({data : all})
//     },

//     filter: function() {
//       this.setState({data: filtered});
//     },

//     render: function() {
//         return (
//           <div>
//             <div className="selection">
//               <ul>
//                 <li onClick={this.showAll}>All</li>
//                 <li onClick={this.filter}>Filter</li>
//               </ul>
//             </div>
//             <hr/>
//             <Chart width={this.props.width}
//                    height={this.props.height}>
//               <Bar data={this.state.data}
//                           width={this.props.width}
//                           height={this.props.height} />
//             </Chart>
//           </div>
//         );
//     }
// });

// React.render(
//     <App /> ,
//     document.getElementById('app')
// );
