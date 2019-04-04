import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import Button from 'components/shared/Button/Button';
import User from 'models/User';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import { authenticateUser, selectCurrentUser } from 'store/user';

import logo from './images/logo.svg';
import styles from './Login.module.css';

interface IPropsFromState {
  user?: User | null;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class Login extends React.Component<AllProps> {
  public render() {
    return (
      <div className={styles.content}>
        <div className={styles.logo}>
          <img src={logo} height={61} width={254} />
        </div>
        <div className={styles.login_slogan}>
          Models are the new code. Let’s show them some{' '}
          <i className="fa fa-heart" style={{ opacity: 0.5 }} />
        </div>
        <div className={styles.form_login}>
          <Button
            size="large"
            textTransform="none"
            icon="github"
            onClick={this.authenticateViaGithub}
          >
            Login with Github
          </Button>
        </div>
      </div>
    );
  }

  @bind
  private authenticateViaGithub() {
    this.props.dispatch(authenticateUser());
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  user: selectCurrentUser(state),
});

export default connect<IPropsFromState, {}, {}, IApplicationState>(
  mapStateToProps
)(Login);
