import * as React from 'react';
import { Link } from 'react-router-dom';
import styles from './Footer.module.css';
import facebook_logo from './images/facebook_logo.svg';
import github_logo from './images/github_logo.svg';
import linkedIN_logo from './images/linkedIN_logo.svg';
import twitter_logo from './images/twitter_logo.svg';

export default class Footer extends React.PureComponent {
  public render() {
    return (
      <footer className={styles.container}>
        <div className={styles.footer_site_map}>
          <span>© 2019 Verta</span>
          <Link to={'/'}>Terms</Link>
          <Link to={'/'}>Privacy</Link>
        </div>
        <div className={styles.social_media}>
          <Link to={'/'}>
            <img src={twitter_logo} />
          </Link>
          <Link to={'/'}>
            <img src={facebook_logo} />
          </Link>
          <Link to={'/'}>
            <img src={linkedIN_logo} />
          </Link>
          <Link to={'/'}>
            <img src={github_logo} />
          </Link>
        </div>
      </footer>
    );
  }
}
