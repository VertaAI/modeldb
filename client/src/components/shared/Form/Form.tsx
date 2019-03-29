import * as React from 'react';

import FormItem from './FormItem/FormItem';
import styles from './Form.module.css';

interface IProps {
  children: any[];
}

class Form extends React.Component<IProps> {
  static Item = FormItem;

  public render() {
    return <div className={styles.form}>{this.props.children}</div>;
  }
}

export { FormItem };
export default Form;
