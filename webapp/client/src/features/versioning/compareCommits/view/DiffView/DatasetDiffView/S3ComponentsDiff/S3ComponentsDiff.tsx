import React, { useCallback } from 'react';

import { IS3DatasetComponentBlobDiff } from 'shared/models/Versioning/Blob/DatasetBlob';
import LastModified from 'shared/view/domain/Versioning/Blob/DatasetBlob/LastModified/LastModified';
import PathSize from 'shared/view/domain/Versioning/Blob/DatasetBlob/PathSize/PathSize';
import GroupedTable from 'shared/view/elements/Table/GroupedTable/GroupedTable';
import { TextWithCopyTooltip } from 'shared/view/elements/TextWithCopyTooltip/TextWithCopyTooltip';

import { getDatasetDiffCellStyle } from '../shared/helpers/getDatasetDiffCellStyle';
import { getDatasetDiffGroupStyle } from '../shared/helpers/getDatasetDiffGroupStyle';
import { groupDatasetDiffs } from '../shared/helpers/groupDatasetDiffs';

interface ILocalProps {
  diff: IS3DatasetComponentBlobDiff[];
}

const S3ComponentsDiff: React.FC<ILocalProps> = ({ diff }) => {
  const groups = groupDatasetDiffs(diff);

  const getRowKey = useCallback(
    (row: typeof groups[number][number], index: number) => index,
    []
  );

  const getGroupKey = useCallback(
    (group: typeof groups[number], index: number) => index,
    []
  );

  return (
    <GroupedTable
      dataGroups={groups}
      getGroupStyle={getDatasetDiffGroupStyle}
      getGroupKey={getGroupKey}
      getRowKey={getRowKey}
      columnDefinitions={[
        {
          title: 'Path',
          type: 'path',
          render: row => (
            <TextWithCopyTooltip copyText={row.path.value}>
              <span title={row.path.value}>{row.path.value}</span>
            </TextWithCopyTooltip>
          ),
          width: '23%',
          getCellStyle: getDatasetDiffCellStyle('path'),
        },
        {
          title: 'Size',
          type: 'size',
          render: row => <PathSize size={row.size.value} />,
          width: '12%',
          getCellStyle: getDatasetDiffCellStyle('size'),
        },
        {
          title: 'Last Modified',
          type: 'lastModifiedAtSource',
          render: row => (
            <LastModified
              lastModifiedAtSource={row.lastModifiedAtSource.value}
            />
          ),
          width: '18%',
          getCellStyle: getDatasetDiffCellStyle('lastModifiedAtSource'),
        },
        {
          title: 'S3 Version ID',
          type: 's3VersionId',
          render: row => (
            <TextWithCopyTooltip copyText={row.s3VersionId.value}>
              <span title={row.s3VersionId.value}>{row.s3VersionId.value}</span>
            </TextWithCopyTooltip>
          ),
          width: '17%',
          getCellStyle: getDatasetDiffCellStyle('s3VersionId'),
        },
        {
          title: 'MD5',
          type: 'md5',
          render: row => (
            <TextWithCopyTooltip copyText={row.md5.value} withEllipsis={true}>
              <span title={row.md5.value}>{row.md5.value}</span>
            </TextWithCopyTooltip>
          ),
          width: '15%',
          getCellStyle: getDatasetDiffCellStyle('md5'),
        },
        {
          title: 'SHA256',
          type: 'sha256',
          render: row => (
            <TextWithCopyTooltip
              copyText={row.sha256.value}
              withEllipsis={true}
            >
              <span title={row.sha256.value}>{row.sha256.value}</span>
            </TextWithCopyTooltip>
          ),
          width: '15%',
          getCellStyle: getDatasetDiffCellStyle('sha256'),
        },
      ]}
    />
  );
};

export default S3ComponentsDiff;
