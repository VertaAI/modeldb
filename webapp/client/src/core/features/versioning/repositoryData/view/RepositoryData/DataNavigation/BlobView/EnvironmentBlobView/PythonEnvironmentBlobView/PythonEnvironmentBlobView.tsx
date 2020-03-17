import * as React from 'react';

import {
  IPythonEnvironmentBlob,
  versionEnvironmentToString,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import PythonVersion from 'core/shared/view/domain/Repository/Blob/EnvironmentBlob/PythonBlob/PythonVersion/PythonVersion';
import {
  PageHeader,
  RecordInfo,
} from 'core/shared/view/elements/PageComponents';

import styles from './PythonEnvironmentBlobView.module.css';
import PythonRequirementEnvironment from 'core/shared/view/domain/Repository/Blob/EnvironmentBlob/PythonBlob/PythonRequirementEnvironment/PythonRequirementEnvironment';

interface ILocalProps {
  blob: IPythonEnvironmentBlob;
}

const PythonEnvironmentBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <div className={styles.root}>
      <PageHeader title="Python details" size="small" withoutSeparator={true} />
      <div className={styles.content}>
        {data.pythonVersion && (
          <RecordInfo label="Python">
            <PythonVersion pythonVersion={data.pythonVersion} />
          </RecordInfo>
        )}
        {data.requirements && (
          <RecordInfo label="Requirements">
            {data.requirements.map((c, i) => (
              <PythonRequirementEnvironment
                pythonRequirementEnvironment={c}
                key={i}
              />
            ))}
          </RecordInfo>
        )}
        {data.constraints && (
          <RecordInfo label="Constaints">
            {data.constraints.map((c, i) => (
              <PythonRequirementEnvironment
                pythonRequirementEnvironment={c}
                key={i}
              />
            ))}
          </RecordInfo>
        )}
      </div>
    </div>
  );
};

export default PythonEnvironmentBlobView;
