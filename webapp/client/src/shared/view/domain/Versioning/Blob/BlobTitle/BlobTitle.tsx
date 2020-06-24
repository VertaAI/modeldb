import * as React from 'react';

import { PageHeader } from 'shared/view/elements/PageComponents';
import capitalize from 'shared/utils/capitalize';

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
