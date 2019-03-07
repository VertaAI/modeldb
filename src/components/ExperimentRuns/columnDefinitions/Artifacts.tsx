import * as React from 'react';
import styles from './ColumnDefs.module.css';
import { Link } from 'react-router-dom';

class ArtifactsColDef extends React.Component<any> {
  public render() {
    const { artifacts, id, projectId } = this.props.data;
    return (
      <div>
        {artifacts.map((artifact: any, i: number) => {
          return (
            <Link key={i} className={styles.model_link} to={`/project/${projectId}/exp-run/${id}`}>
              <div className={styles.artifact_wrapper} title="view ModelRecord">
                <div className={styles.notif}>
                  {artifact.type === 'IMAGE' ? (
                    <i className="fa fa-image" style={{ color: '#6863ff' }} />
                  ) : (
                    <i className="fa fa-codepen" style={{ color: '#6863ff' }} />
                  )}
                </div>
                <div className={styles.type}>{artifact.type} &nbsp; &nbsp; &nbsp; -> </div>
                <div className={styles.key}>{artifact.key}</div>
                <div className={styles.path}>{artifact.path}</div>
              </div>
            </Link>
          );
        })}
      </div>
    );
  }
}

export default ArtifactsColDef;
