import React from 'react';

import { IConfigBlob } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import HyperparameterItem from 'core/shared/view/domain/Repository/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Repository/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';
import {
  PageHeader,
  RecordInfo,
} from 'core/shared/view/elements/PageComponents';

import styles from './ConfigBlobView.module.css';

interface ILocalProps {
  blob: IConfigBlob;
}

const ConfigBlobView: React.FC<ILocalProps> = ({ blob }) => (
  <div className={styles.root}>
    <PageHeader title="Config" size="small" />

    <RecordInfo label="Hyperparameters">
      {blob.data.hyperparameters.map(hp => (
        <HyperparameterItem key={hp.name} hyperparameter={hp} />
      ))}
    </RecordInfo>

    <RecordInfo label="Hyperparameters set">
      {blob.data.hyperparameterSet.map(hp => (
        <HyperparameterSetItem key={hp.name} hyperparameterSetItem={hp} />
      ))}
    </RecordInfo>
  </div>
);

export default ConfigBlobView;
