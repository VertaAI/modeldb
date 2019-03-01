import * as React from 'react';
import { Link } from 'react-router-dom';
import Tag from '../../TagBlock/Tag';
import tag_styles from '../../TagBlock/TagBlock.module.css';
import styles from './ColumnDefs.module.css';

class ModelRecordColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    return (
      <div className={styles.param_cell}>
        <Link className={styles.model_link} to={`/project/${modelRecord.ProjectId}/exp-run/${modelRecord.Id}`}>
          <strong>Model ID</strong>
        </Link>
        <a className={styles.experiment_link}>Experiment ID</a>
        <p>
          Model Name: <span>{modelRecord.name}</span>
        </p>
        {modelRecord.artifacts.length > 0 && (
          <p>
            Artifact:{' '}
            {modelRecord.artifacts.map((artifact: any, i: number) => {
              return (
                <span key={i}>
                  {modelRecord.artifacts[0].path
                    .split('/')
                    .slice(-1)
                    .pop()}
                </span>
              );
            })}
          </p>
        )}
        {modelRecord.tags && (
          <span>
            {/* // dragabble did not work when a TagBlock Component was inserted */}
            <p>Tags:</p>
            <ul className={tag_styles.tags}>
              {modelRecord.tags.map((tag: string, i: number) => {
                return (
                  <li key={i}>
                    <Tag tag={tag} />
                  </li>
                );
              })}
            </ul>
          </span>
        )}
      </div>
    );
  }
}

export default ModelRecordColDef;
