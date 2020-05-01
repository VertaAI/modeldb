import * as React from 'react';

import { IPythonEnvironmentBlob } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import PythonVersion from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonVersion/PythonVersion';
import PythonRequirementEnvironment from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonRequirementEnvironment/PythonRequirementEnvironment';

import styles from './PythonEnvironmentBlobView.module.css';
import makePropertiesTableComponents from 'core/shared/view/domain/Versioning/Blob/PropertiesTable/PropertiesTable';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

interface ILocalProps {
  blob: IPythonEnvironmentBlob;
}

const tableComponents = makePropertiesTableComponents<
  IPythonEnvironmentBlob['data']
>();

const PythonEnvironmentBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <BlobDataBox title="Python details">
      <div className={styles.content}>
        <tableComponents.Table data={data}>
          <tableComponents.PropDefinition
            title="Python"
            render={({ data: { pythonVersion } }) =>
              pythonVersion && <PythonVersion pythonVersion={pythonVersion} />
            }
          />
          <tableComponents.PropDefinition
            title="Requirements"
            render={({ data: { requirements } }) =>
              (requirements || []).map((c, i) => (
                <PythonRequirementEnvironment
                  pythonRequirementEnvironment={c}
                  key={i}
                />
              ))
            }
          />
          <tableComponents.PropDefinition
            title="Constraints"
            render={({ data: { constraints } }) =>
              (constraints || []).map((c, i) => (
                <PythonRequirementEnvironment
                  pythonRequirementEnvironment={c}
                  key={i}
                />
              ))
            }
          />
        </tableComponents.Table>
      </div>
    </BlobDataBox>
  );
};

export default PythonEnvironmentBlobView;
