import * as React from 'react';

import {
  IVersionEnvironmentBlob,
  versionEnvironmentToString,
} from 'shared/models/Versioning/Blob/EnvironmentBlob';

interface ILocalProps {
  pythonVersion: IVersionEnvironmentBlob;
  rootStyles?: React.CSSProperties;
}

const PythonVersion = ({ pythonVersion, rootStyles }: ILocalProps) => {
  return (
    <div style={rootStyles}>{versionEnvironmentToString(pythonVersion)}</div>
  );
};

export default PythonVersion;
