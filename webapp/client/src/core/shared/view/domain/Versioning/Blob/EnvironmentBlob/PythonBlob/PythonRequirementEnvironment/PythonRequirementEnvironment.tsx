import * as React from 'react';

import {
  IPythonRequirementEnvironment,
  versionEnvironmentToString,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';

interface ILocalProps {
  pythonRequirementEnvironment: IPythonRequirementEnvironment;
  rootStyles?: React.CSSProperties;
}

const PythonRequirementEnvironment = ({
  pythonRequirementEnvironment: { library, constraint, version },
  rootStyles,
}: ILocalProps) => {
  return (
    <div key={library} style={rootStyles}>
      {library} {constraint || ''}{' '}
      {version ? versionEnvironmentToString(version) : ''}
    </div>
  );
};

export default PythonRequirementEnvironment;
