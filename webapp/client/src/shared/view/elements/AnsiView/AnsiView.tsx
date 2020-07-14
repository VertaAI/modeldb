import * as React from 'react';
import ReactAnsi from 'react-ansi';

import styles from './AnsiView.module.css';

interface ILocalProps {
  data: string;
}

const AnsiView = (props: ILocalProps) => {
  return (
    <div className={styles.root}>
      <ReactAnsi log={props.data} />
    </div>
  );
};

export default AnsiView;
