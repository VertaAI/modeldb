import * as React from 'react';

import styles from './ColumnDefs.module.css';

class ObservationsColDef extends React.Component<any> {
  public render() {
    const observations = this.props.value;
    return (
      <p>
        Observations:{' '}
        {observations.map((observation: any, i: number) => {
          return (
            <div key={i}>{observation.timestamp.toLocaleDateString()}</div>
          );
        })}
      </p>
    );
  }
}

export default ObservationsColDef;
