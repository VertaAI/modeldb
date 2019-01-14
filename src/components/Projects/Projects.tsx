import * as React from 'react';
import styles from './Projects.module.css';

export interface IProjectsProps {
  something: any;
}

export default class Projects extends React.Component<IProjectsProps, {}> {
  public render() {
    return (
      <div>
        <div className={styles.headPanel}>
          <div>Projects</div>
          <div />
          <div>
            <button>Create</button>
          </div>
        </div>
      </div>
    );
  }
}
