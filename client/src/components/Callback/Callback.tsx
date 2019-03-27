import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import GlobalPreloader from 'components/GlobalPreloader/GlobalPreloader';
import { IConnectedReduxProps } from 'store/store';
import { handleUserAuthentication } from 'store/user';

type AllProps = IConnectedReduxProps & RouteComponentProps;

class Callback extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(handleUserAuthentication());
  }

  public render() {
    return <GlobalPreloader />;
  }
}

const mapStateToProps = () => ({});

export default connect(mapStateToProps)(Callback);
