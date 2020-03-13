import * as React from 'react';

import styles from './ClientSuggestion.module.css';

interface ILocalProps {
  fieldName: string;
  clientMethod: string;
  link: string;
}

class ClientSuggestion extends React.PureComponent<ILocalProps> {
  public render() {
    const { fieldName, clientMethod, link } = this.props;
    return (
      <div className={styles.root}>
        No {fieldName} logged.{' '}
        <span>
          See{' '}
          <a href={link} target="blank" className={styles.suggestion_link}>
            {clientMethod}
          </a>
        </span>
      </div>
    );
  }
}

export default ClientSuggestion;
