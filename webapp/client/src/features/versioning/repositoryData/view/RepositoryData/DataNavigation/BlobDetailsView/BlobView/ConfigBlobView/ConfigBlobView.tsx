import React from 'react';

import { IConfigBlob } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import HyperparameterItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';

import PropertiesTable from '../shared/PropertiesTable/PropertiesTable';

interface ILocalProps {
  blob: IConfigBlob;
}

const ConfigBlobView: React.FC<ILocalProps> = ({ blob }) => (
  <BlobDataBox title="Config">
    <PropertiesTable
      data={blob.data}
      propDefinitions={[
        {
          title: 'Hyperparameters',
          render: ({ hyperparameters }) =>
            hyperparameters.map(hp => (
              <HyperparameterItem key={hp.name} hyperparameter={hp} />
            )),
        },
        {
          title: 'Hyperparameters set',
          render: () =>
            blob.data.hyperparameterSet.map(hp => (
              <HyperparameterSetItem key={hp.name} hyperparameterSetItem={hp} />
            )),
        },
      ]}
    />
  </BlobDataBox>
);

export default ConfigBlobView;
