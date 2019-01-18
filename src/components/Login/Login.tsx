import User from 'models/User';
import * as React from 'react';
import { connect } from 'react-redux';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { authenticateUser } from '../../store/user';
import logo from '../images/logo.svg';
import styles from './Login.module.css';

interface IPropsFromState {
  user?: User | undefined;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class Login extends React.Component<AllProps> {
  public constructor(props: AllProps) {
    super(props);

    this.authenticateViaGithub = this.authenticateViaGithub.bind(this);
  }

  public render() {
    return (
      <div className={styles.content}>
        <div className={styles.logo}>
          <img src={logo} height={107} width={450} />
        </div>
        <div className={styles.form_login}>
          <button className={styles.create_button} onClick={this.authenticateViaGithub}>
            Sign in with Github
          </button>
        </div>
      </div>
    );
  }

  private authenticateViaGithub(event: React.SyntheticEvent<HTMLButtonElement>) {
    this.props.dispatch(authenticateUser());
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect<IPropsFromState, {}, {}, IApplicationState>(mapStateToProps)(Login);
