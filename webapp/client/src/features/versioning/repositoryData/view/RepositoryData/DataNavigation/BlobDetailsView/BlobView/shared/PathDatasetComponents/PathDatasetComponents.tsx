import React, { useCallback } from 'react';

import { IPathDatasetComponentBlob } from 'shared/models/Versioning/Blob/DatasetBlob';
import LastModified from 'shared/view/domain/Versioning/Blob/DatasetBlob/LastModified/LastModified';
import PathSize from 'shared/view/domain/Versioning/Blob/DatasetBlob/PathSize/PathSize';
import Table from 'shared/view/elements/Table/Table';
import { TextWithCopyTooltip } from 'shared/view/elements/TextWithCopyTooltip/TextWithCopyTooltip';

interface ILocalProps {
  data: IPathDatasetComponentBlob[];
}

const PathDatasetComponents = (props: ILocalProps) => {
  const getRowKey = useCallback(
    (row: IPathDatasetComponentBlob) => row.sha256,
    []
  );

  return (
    <Table
      dataRows={props.data}
      getRowKey={getRowKey}
      columnDefinitions={[
        {
          title: 'Path',
          type: 'path',
          render: row => (
            <TextWithCopyTooltip copyText={row.path}>
              <span title={row.path}>{row.path}</span>
            </TextWithCopyTooltip>
          ),
          width: '24%',
          withSort: true,
          getValue: row => row.path,
        },
        {
          title: 'Size',
          type: 'size',
          render: row => <PathSize size={row.size} />,
          width: '19%',
          withSort: true,
          getValue: row => row.size,
        },
        {
          title: 'Last Modified',
          type: 'lastModifiedAtSource',
          render: row => (
            <LastModified lastModifiedAtSource={row.lastModifiedAtSource} />
          ),
          width: '19%',
          withSort: true,
          getValue: row => row.lastModifiedAtSource.valueOf(),
        },
        {
          title: 'MD5',
          type: 'md5',
          render: row => (
            <TextWithCopyTooltip copyText={row.md5} withEllipsis={true}>
              <span title={row.md5}>{row.md5}</span>
            </TextWithCopyTooltip>
          ),
          width: '19%',
          withSort: true,
          getValue: row => row.md5,
        },
        {
          title: 'SHA256',
          type: 'sha256',
          render: row => (
            <TextWithCopyTooltip copyText={row.sha256} withEllipsis={true}>
              <span title={row.sha256}>{row.sha256}</span>
            </TextWithCopyTooltip>
          ),
          width: '19%',
          withSort: true,
          getValue: row => row.sha256,
        },
      ]}
    />
  );
};

export default PathDatasetComponents;
