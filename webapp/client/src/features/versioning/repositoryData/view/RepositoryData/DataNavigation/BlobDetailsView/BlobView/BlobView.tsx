import React from 'react';

import { IBlob } from 'shared/models/Versioning/Blob/Blob';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import { JsonView } from 'shared/view/elements/JsonView/JsonView';

import CodeBlobView from './CodeBlobView/CodeBlobView';
import ConfigBlobView from './ConfigBlobView/ConfigBlobView';
import DatasetBlobView from './DatasetBlobView/DatasetBlobView';
import EnvironmentBlobView from './EnvironmentBlobView/EnvironmentBlobView';

interface ILocalProps {
  blobData: IBlob['data'];
}

const BlobView: React.FC<ILocalProps> = ({ blobData }: ILocalProps) => {
  switch (blobData.category) {
    case 'dataset': {
      return <DatasetBlobView blob={blobData} />;
    }
    case 'code': {
      return <CodeBlobView blob={blobData} />;
    }
    case 'environment': {
      return <EnvironmentBlobView blob={blobData} />;
    }
    case 'config': {
      return <ConfigBlobView blob={blobData} />;
    }
    case 'unknown': {
      return <JsonView object={blobData.data} />;
    }
    default:
      return exhaustiveCheck(blobData, '');
  }
};

export default BlobView;
