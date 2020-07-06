import _ from 'lodash';
import React from 'react';
import EmptyChart from './chart/chartWithNoData';
import styles from './ChartWrapperForNoData.module.css';

interface ILocalProps {
  chartHeading: string;
  message: string;
  canvasClassName: string;
}

class ChartWrapperForNoData extends React.Component<ILocalProps> {
  public render() {
    const { chartHeading, message, canvasClassName } = this.props;
    return (
      <div className={styles.chart_section_wrapper}>
        <div className={styles.chart_header}>{chartHeading}</div>
        <EmptyChart errorMessage={message} canvasClassName={canvasClassName} />
      </div>
    );
  }
}

export default ChartWrapperForNoData;
