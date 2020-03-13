import { Grid } from '@devexpress/dx-react-grid';
import * as React from 'react';

import styles from './Root.module.css';

const Root = (props: Grid.RootProps) => (
  <div className={styles.root} {...props} />
);

export default Root;
