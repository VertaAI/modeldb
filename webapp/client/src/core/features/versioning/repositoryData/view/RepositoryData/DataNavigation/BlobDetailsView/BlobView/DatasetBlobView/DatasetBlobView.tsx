import React from 'react';

import {
  IDatasetBlob,
  IPathDatasetComponentBlob,
} from 'core/shared/models/Versioning/Blob/DatasetBlob';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import matchType from 'core/shared/utils/matchType';

import PathDatasetComponents from '../shared/PathDatasetComponents/PathDatasetComponents';
import styles from './DatasetBlobView.module.css';
import BlobTitle from 'core/shared/view/domain/Versioning/Blob/BlobTitle/BlobTitle';

interface ILocalProps {
  blob: IDatasetBlob;
}

const DatasetBlobView: React.FC<ILocalProps> = ({ blob }) => {
  const pathDatasetComponents: IPathDatasetComponentBlob[] = (() => {
    switch (blob.type) {
      case 's3':
        return blob.components.map(({ path }) => path);
      case 'path':
        return blob.components;
      default:
        return exhaustiveCheck(blob, '');
    }
  })();

  const title = matchType(
    {
      s3: () => 'S3 Dataset',
      path: () => 'Path Dataset',
    },
    blob.type
  );

  return (
    <div className={styles.root}>
      <div className={styles.title}>
        <BlobTitle title={title} />
      </div>
      <div className={styles.table}>
        <PathDatasetComponents data={pathDatasetComponents} />
      </div>
    </div>
  );
};

export default DatasetBlobView;
