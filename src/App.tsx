import React, {Component} from 'react';
import {BrowserRouter as Router, Link, Route, Switch} from 'react-router-dom';
import './App.css';
import Dashboard from './components/Dashboard';

class App extends Component {
  public render() {
    return (
      <Router>
        <div>
          <ul>
            <li>
              <Link to="/">Dashboard</Link>
            </li>
            <li>
              <Link to="/project">Project Dashboard</Link>
            </li>
            <li>
              <Link to="/model">Model Summary</Link>
            </li>
            <li>
              <Link to="/visualizations">Dashboard visualizations </Link>
            </li>
          </ul>

          <hr />

          <Switch>
            <Route exact path="/" component={Dashboard} />
            <Route exact path="/project" component={Dashboard} />
            <Route exact path="/model" component={Dashboard} />
            <Route exact path="/visualizations" component={Dashboard} />
          </Switch>
        </div>
      </Router>
    );
  }
}

export default App;
