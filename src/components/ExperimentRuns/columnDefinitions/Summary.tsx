import * as React from 'react';
import styles from './ColumnDefs.module.css';

class SummaryColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    return (
      <div className={styles.summary_cell}>
        <p>
          Model Name:{' '}
          <strong style={{ color: '#666' }}>
            <span>{modelRecord.name}</span>
          </strong>
        </p>
        <p>
          Model Desc:{' '}
          <strong style={{ color: '#666' }}>
            <span>.....</span>
          </strong>
        </p>
        <p>
          Code Version:{' '}
          <strong style={{ color: '#666' }}>
            <span>{modelRecord.codeVersion}</span>
          </strong>
        </p>
      </div>
    );
  }
}

export default SummaryColDef;
