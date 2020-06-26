import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import { HeaderSearch } from 'features/highLevelSearch';
import { IApplicationState } from 'setup/store/store';

import logo from './images/Verta_logo.svg';
import styles from './LayoutHeader.module.css';

interface ILocalProps {
  rightContent: React.ReactNode;
}

const mapStateToProps = (state: IApplicationState) => {
  return {};
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

class LayoutHeader extends React.PureComponent<AllProps> {
  public render() {
    const { rightContent } = this.props;
    return (
      <header className={styles.header}>
        <div className={styles.logo}>
          <Link to={'/'}>
            <img src={logo} alt="logo" />
          </Link>
        </div>
        <div className={styles.highLevelSearch}>
          <HeaderSearch />
        </div>
        {rightContent && (
          <div className={styles.rightContent}>{rightContent}</div>
        )}
      </header>
    );
  }
}

export default connect(mapStateToProps)(LayoutHeader);
