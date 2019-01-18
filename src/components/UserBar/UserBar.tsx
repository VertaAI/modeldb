import Project from 'models/Project';
import * as React from 'react';
import Avatar from 'react-avatar';
import { connect } from 'react-redux';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './UserBar.module.css';

interface IOwnProps {
  isOpened: boolean;
}

type AllProps = IConnectedReduxProps;

class UserBar extends React.Component<AllProps, IOwnProps> {
  public render() {
    return (
      <div>
        <div className={styles.user_bar}>
          <Avatar
            name="Anton Vasin"
            color="white"
            fgColor="black"
            round={true}
            size="36"
            textSizeRatio={36 / 16}
            style={{ fontFamily: 'Roboto', fontWeight: '400' }}
          />
          <div className={styles.menu_arrow} onClick={() => this.toggleMenu()}>
            <i className="fa fa-caret-down" />
          </div>
          <div className={styles.drop_down}>
            <div>Settings</div>
            <div>Log Out</div>
          </div>
        </div>
        {/* {this.state.isOpened ? (
          <div className={styles.drop_down}>
            <div>Settings</div>
            <div>Log Out</div>
          </div>
        ) : (
          ''
        )} */}
      </div>
    );
  }

  public componentDidMount() {
    this.setState({ isOpened: false });
  }

  private toggleMenu(): void {
    console.log(this);
    this.setState(prevState => ({ isOpened: !prevState.isOpened }));
  }
}

const mapStateToProps = ({ projects }: IApplicationState) => ({
  loading: projects.loading
});

export default connect<{}, {}, {}, IApplicationState>(mapStateToProps)(UserBar);
