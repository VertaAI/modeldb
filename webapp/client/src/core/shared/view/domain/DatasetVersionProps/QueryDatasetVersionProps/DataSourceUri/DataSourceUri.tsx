import * as React from 'react';

import ExternalLink from 'core/shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  value: string;
  additionalClassname?: string;
}

class DataSourceUri extends React.PureComponent<ILocalProps> {
  public render() {
    const { value, additionalClassname } = this.props;
    return (
      <ExternalLink
        url={value}
        text={value}
        additionalClassname={additionalClassname}
      />
    );
  }
}

export default DataSourceUri;
