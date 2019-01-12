import React, { Component } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import { ThemeColors } from 'store/layout';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import './App.css';
import Dashboard from './components/Dashboard';

// Any additional component props go here.
interface IOwnProps {
  history: any;
}

interface IPropsFromState {
  theme: ThemeColors;
}

// Create an intersection type of the component props and our Redux props.
type AllProps = IPropsFromState & IConnectedReduxProps & IOwnProps;

class App extends Component<AllProps> {
  public render() {
    return (
      <Router>
        <div>
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
              <Route exact={true} path="/" component={Dashboard} />
              <Route exact={true} path="/project" component={Dashboard} />
              <Route exact={true} path="/model" component={Dashboard} />
              <Route
                exact={true}
                path="/visualizations"
                component={Dashboard}
              />
            </Switch>
          </div>
        </div>
      </Router>
    );
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  theme: layout.theme
});

export default connect<IPropsFromState, {}, IOwnProps, IApplicationState>(
  mapStateToProps
)(App);
