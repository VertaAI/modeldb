import React from 'react';

import { formatBytes } from 'shared/utils/mapperConverters';
import { TextWithCopyTooltip } from 'shared/view/elements/TextWithCopyTooltip/TextWithCopyTooltip';

const PathSize = ({
  size,
  className,
}: {
  size: number;
  className?: string;
}) => (
  <TextWithCopyTooltip withEllipsis={true} copyText={formatBytes(size)}>
    <span className={className} title={formatBytes(size)}>
      {formatBytes(size)}
    </span>
  </TextWithCopyTooltip>
);

export default PathSize;
