import * as React from 'react';

import {
  IPythonRequirementEnvironment,
  versionEnvironmentToString,
} from 'shared/models/Versioning/Blob/EnvironmentBlob';

import styles from './PythonRequirementEnvironment.module.css';

interface ILocalProps {
  pythonRequirementEnvironment: IPythonRequirementEnvironment;
  rootStyles?: React.CSSProperties;
  valueStyles?: React.CSSProperties;
}

const PythonRequirementEnvironment = ({
  pythonRequirementEnvironment: { library, constraint, version },
  rootStyles,
  valueStyles,
}: ILocalProps) => {
  return (
    <div className={styles.root} key={library} style={rootStyles}>
      <span className={styles.library}>{library} </span>&nbsp;
      <span className={styles.value} style={valueStyles}>
        {constraint || ''} {version ? versionEnvironmentToString(version) : ''}
      </span>
    </div>
  );
};

export default PythonRequirementEnvironment;
