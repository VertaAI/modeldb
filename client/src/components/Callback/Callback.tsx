import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import { IConnectedReduxProps } from 'store/store';
import { handleUserAuthentication } from 'store/user';

import GlobalPreloader from 'components/GlobalPreloader/GlobalPreloader';

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
