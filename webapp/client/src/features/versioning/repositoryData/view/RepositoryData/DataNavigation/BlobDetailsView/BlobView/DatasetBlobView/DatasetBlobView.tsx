import React from 'react';

import { IDatasetBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import matchBy from 'core/shared/utils/matchBy';
import matchType from 'core/shared/utils/matchType';
import BlobTitle from 'core/shared/view/domain/Versioning/Blob/BlobTitle/BlobTitle';

import PathDatasetComponents from '../shared/PathDatasetComponents/PathDatasetComponents';
import S3DatasetComponents from '../shared/S3DatasetComponents/S3DatasetComponents';
import styles from './DatasetBlobView.module.css';

interface ILocalProps {
  blob: IDatasetBlob;
}

const DatasetBlobView: React.FC<ILocalProps> = ({ blob }) => {
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
        {matchBy(blob, 'type')({
          path: pathDatasetBlob => (
            <PathDatasetComponents data={pathDatasetBlob.data.components} />
          ),
          s3: s3DatasetBlob => (
            <S3DatasetComponents data={s3DatasetBlob.data.components} />
          ),
        })}
      </div>
    </div>
  );
};

export default DatasetBlobView;
