import * as React from 'react';
import { Link } from 'react-router-dom';

import Tag from 'components/TagBlock/Tag';
import tag_styles from 'components/TagBlock/TagBlock.module.css';
import routes from 'routes';

import styles from './ColumnDefs.module.css';

class SummaryColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    console.log(modelRecord);
    return (
      <div className={styles.summary_cell}>
        {modelRecord.experimentId && (
          <div>
            <this.parmaLink
              label="Experiment:"
              value={modelRecord.experimentId.slice(0, 6)}
            />
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

  public parmaLink = (props: { label: string; value: string; link?: any }) => {
    const { label, value } = props;
    return (
      <div className={styles.experiment_link}>
        <span className={styles.parma_link_label}>{label}</span>{' '}
        <span className={styles.parma_link_value}>{value.slice(0, 6)}</span>
      </div>
    );
  };
}

export default SummaryColDef;
