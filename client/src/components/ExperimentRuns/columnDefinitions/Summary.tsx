import * as React from 'react';

import Tag from 'components/shared/TagBlock/Tag';
import tag_styles from 'components/shared/TagBlock/TagBlock.module.css';

import styles from './ColumnDefs.module.css';

class SummaryColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    return (
      <div className={styles.summary_cell}>
        {modelRecord.experimentId && (
          <div className={styles.expId_block}>
            Experiment Id:{' '}
            <div className={styles.highlight_summary}>
              {modelRecord.experimentId}
            </div>
          </div>
        )}
        {modelRecord.codeVersion && (
          <div className={styles.codeVersion_block}>
            Code Version:{' '}
            <div className={styles.highlight_summary}>
              {modelRecord.codeVersion}
            </div>
          </div>
        )}
        {modelRecord.tags && modelRecord.tags.length > 0 && (
          <div className={tag_styles.tag_block}>
            <div>Tags:</div>
            <ul className={tag_styles.tags}>
              {modelRecord.tags.map((tag: string, i: number) => {
                return (
                  <li key={i}>
                    <Tag tag={tag} />
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </div>
    );
  }
}

export default SummaryColDef;
