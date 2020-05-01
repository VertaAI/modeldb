import * as React from 'react';

import {
  IPythonRequirementEnvironment,
  versionEnvironmentToString,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';

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
    <div key={library} style={rootStyles}>
      {library}{' '}
      <span style={valueStyles}>
        {constraint || ''} {version ? versionEnvironmentToString(version) : ''}
      </span>
    </div>
  );
};

export default PythonRequirementEnvironment;
