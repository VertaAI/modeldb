import * as React from 'react';

import { PageHeader } from 'core/shared/view/elements/PageComponents';
import capitalize from 'core/shared/utils/capitalize';

interface ILocalProps {
  title: string;
}

const BlobTitle = (props: ILocalProps) => {
  return (
    <PageHeader
      title={capitalize(props.title.toLocaleLowerCase())}
      withoutSeparator={true}
      size="small"
    />
  );
};

export default BlobTitle;
