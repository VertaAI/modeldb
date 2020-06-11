import * as React from 'react';

import { IPythonEnvironmentBlob } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import PythonRequirementEnvironment from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonRequirementEnvironment/PythonRequirementEnvironment';
import PythonVersion from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonVersion/PythonVersion';

import PropertiesTable from '../../shared/PropertiesTable/PropertiesTable';
import styles from './PythonEnvironmentBlobView.module.css';

interface ILocalProps {
  blob: IPythonEnvironmentBlob;
}

const PythonEnvironmentBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <BlobDataBox title="Python details">
      <div className={styles.content}>
        <PropertiesTable
          data={data}
          propDefinitions={[
            {
              title: 'Python',
              render: ({ pythonVersion }) =>
                pythonVersion ? (
                  <PythonVersion pythonVersion={pythonVersion} />
                ) : null,
            },
            {
              title: 'Requirements',
              render: ({ requirements }) =>
                (requirements || []).map((c, i) => (
                  <PythonRequirementEnvironment
                    pythonRequirementEnvironment={c}
                    key={i}
                  />
                )),
            },
            {
              title: 'Constraints',
              render: ({ constraints }) =>
                (constraints || []).map((c, i) => (
                  <PythonRequirementEnvironment
                    pythonRequirementEnvironment={c}
                    key={i}
                  />
                )),
            },
          ]}
        />
      </div>
    </BlobDataBox>
  );
};

export default PythonEnvironmentBlobView;
