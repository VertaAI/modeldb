import * as React from 'react';

import { IConfigHyperparameterValue } from 'shared/models/Versioning/Blob/ConfigBlob';

const HyperparameterValue = ({
  value,
  rootStyles,
}: {
  value: IConfigHyperparameterValue;
  rootStyles?: React.CSSProperties;
}) => {
  const displayedText =
    value.type === 'string' ? `"${value.value}"` : value.value;
  return (
    <span title={String(displayedText)} style={rootStyles}>
      {displayedText}
    </span>
  );
};

export default HyperparameterValue;
