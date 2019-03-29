import * as React from 'react';

import styles from './Form.module.css';
import FormItem from './FormItem/FormItem';

interface IProps {
  children: any[];
}

class Form extends React.Component<IProps> {
  public static Item = FormItem;

  public render() {
    return <div className={styles.form}>{this.props.children}</div>;
  }
}

export { FormItem };
export default Form;
