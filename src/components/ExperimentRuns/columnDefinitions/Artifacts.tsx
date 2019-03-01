import * as React from 'react';
import styles from './ColumnDefs.module.css';

class ArtifactsColDef extends React.Component<any> {
  public render() {
    const artifactList = this.props.value;
    return (
      <div>
        {artifactList.map((artifact: any, i: number) => {
          return (
            <div key={i} className={styles.artifact_wrapper}>
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
          );
        })}
      </div>
    );
  }
}

export default ArtifactsColDef;
