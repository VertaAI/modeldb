import React, { CSSProperties } from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { handleUserAuthentication } from '../../store/user';
import loader from '../images/loader.gif';
import styles from './Callback.module.css';

type AllProps = IConnectedReduxProps & RouteComponentProps;

class Callback extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(handleUserAuthentication());
  }

  public render() {
    return (
      <div className={styles.center}>
        <img src={loader} alt="loading" />
      </div>
    );
  }
}

const mapStateToProps = () => ({});

export default connect(mapStateToProps)(Callback);
