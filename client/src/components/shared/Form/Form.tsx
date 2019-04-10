import * as React from 'react';

import styles from './Form.module.css';
import FormItem from './FormItem/FormItem';

interface ILocalProps {
  children: React.ReactNode;
}

class Form extends React.Component<ILocalProps> {
  public static Item = FormItem;

  public render() {
    return <div className={styles.form}>{this.props.children}</div>;
  }
}

export default Form;
