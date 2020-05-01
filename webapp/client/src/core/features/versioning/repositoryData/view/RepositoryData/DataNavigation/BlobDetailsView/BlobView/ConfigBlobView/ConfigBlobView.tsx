import React from 'react';

import { IConfigBlob } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import HyperparameterItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';
import { RecordInfo } from 'core/shared/view/elements/PageComponents';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import makePropertiesTableComponents from 'core/shared/view/domain/Versioning/Blob/PropertiesTable/PropertiesTable';

interface ILocalProps {
  blob: IConfigBlob;
}

const tableComponents = makePropertiesTableComponents<IConfigBlob['data']>();

const ConfigBlobView: React.FC<ILocalProps> = ({ blob }) => (
  <BlobDataBox title="Config">
    <tableComponents.Table data={blob.data}>
      <tableComponents.PropDefinition
        title="Hyperparameters"
        render={() =>
          blob.data.hyperparameters.map(hp => (
            <HyperparameterItem key={hp.name} hyperparameter={hp} />
          ))
        }
      />
      <tableComponents.PropDefinition
        title="Hyperparameters set"
        render={() =>
          blob.data.hyperparameterSet.map(hp => (
            <HyperparameterSetItem key={hp.name} hyperparameterSetItem={hp} />
          ))
        }
      />
    </tableComponents.Table>
  </BlobDataBox>
);

export default ConfigBlobView;
