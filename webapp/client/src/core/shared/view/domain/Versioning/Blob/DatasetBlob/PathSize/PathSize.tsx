import React from 'react';

import { formatBytes } from 'core/shared/utils/mapperConverters';

const PathSize = ({
  size,
  className,
}: {
  size: number;
  className?: string;
}) => (
  <span className={className} title={formatBytes(size)}>
    {formatBytes(size)}
  </span>
);

export default PathSize;
