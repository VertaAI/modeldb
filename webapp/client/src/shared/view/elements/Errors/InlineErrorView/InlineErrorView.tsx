import cn from 'classnames';
import * as React from 'react';

import styles from './InlineErrorView.module.css';

interface ILocalProps {
  error: Exclude<React.ReactNode, null | undefined>;
  additionalClassname?: string;
  dataTest?: string;
}

class InlineErrorView extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <div
        className={cn(styles.root, this.props.additionalClassname)}
        data-test={this.props.dataTest || 'error'}
      >
        {this.props.error}
      </div>
    );
  }
}

export default InlineErrorView;
