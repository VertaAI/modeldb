import User from 'models/User';
import * as React from 'react';
import { connect } from 'react-redux';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import logo from '../images/logo.svg';

import styles from './Login.module.css';

interface IPropsFromState {
  user?: User | undefined;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class Login extends React.Component<AllProps> {
  public render() {
    return (
      <div className={styles.content}>
        <div className={styles.logo}>
          <img src={logo} height={107} width={450} />
        </div>
        <div className={styles.form_login}>
          <form>
            <button className={styles.create_button}>Sign in with Github</button>
          </form>
        </div>
      </div>
    );
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect<IPropsFromState, {}, {}, IApplicationState>(mapStateToProps)(Login);
