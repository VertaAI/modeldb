import Tag from 'components/TagBlock/Tag';
import tag_styles from 'components/TagBlock/TagBlock.module.css';
import * as React from 'react';
import styles from './ColumnDefs.module.css';

class SummaryColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    return (
      <div className={styles.summary_cell}>
        <div style={{ marginBottom: '10px' }}>
          Name: <div className={styles.highlight_summary}>{modelRecord.name}</div>
        </div>
        {modelRecord.codeVersion && (
          <div style={{ marginBottom: '10px' }}>
            Code Version: <div className={styles.highlight_summary}>{modelRecord.codeVersion}</div>
          </div>
        )}
        {modelRecord.tags && (
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
