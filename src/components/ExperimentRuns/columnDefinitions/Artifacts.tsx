import * as React from 'react';
import styles from './ColumnDefs.module.css';

class ArtifactsColDef extends React.Component<any> {
  public render() {
    const artifactList = this.props.value;
    return (
      <p>
        Artifact:{' '}
        {artifactList.map((artifact: any, i: number) => {
          return (
            <span key={i}>
              {artifact.path
                .split('/')
                .slice(-1)
                .pop()}
            </span>
          );
        })}
      </p>
    );
  }
}

export default ArtifactsColDef;
