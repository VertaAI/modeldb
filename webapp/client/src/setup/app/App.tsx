import React from 'react';
import { RouteComponentProps, withRouter } from 'react-router';

import Pages from 'pages/authorized';

type AllProps = RouteComponentProps;

class App extends React.PureComponent<AllProps> {
  public render() {
    return <Pages />;
  }
}

export { App };
export default withRouter(App);
