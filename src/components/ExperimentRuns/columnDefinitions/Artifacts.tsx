import * as React from 'react';
import styles from './ColumnDefs.module.css';

class ArtifactsColDef extends React.Component<any> {
  public render() {
    const artifactObject = this.props.value;
    return (
      <div className={styles.param_cell}>
        {artifactObject.map((property: any, i: number) => {
          return <a key={i}> {property.path}</a>;
        })}
      </div>
    );
  }
}

export default ArtifactsColDef;
