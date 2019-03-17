import * as React from 'react';
import styles from './ColumnDefs.module.css';
import Tag from '../../TagBlock/Tag';
import tag_styles from '../../TagBlock/TagBlock.module.css';

class SummaryColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    return (
      <div className={styles.summary_cell}>
        <p>
          Name: <span className={styles.highlight_summary}>{modelRecord.name}</span>
        </p>
        <p>
          Code Version: <span className={styles.highlight_summary}>{modelRecord.codeVersion}</span>
        </p>
        <p>Tags:</p>
        <div className={tag_styles.tag_block}>
          <ul className={tag_styles.tags}>
            {modelRecord.tags &&
              modelRecord.tags.map((tag: string, i: number) => {
                return (
                  <li key={i}>
                    <Tag tag={tag} />
                  </li>
                );
              })}
          </ul>
        </div>
      </div>
    );
  }
}

export default SummaryColDef;
