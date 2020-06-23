import * as React from 'react';

import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import { GenericNotFound } from 'shared/view/elements/NotFoundComponents/GenericNotFound/GenericNotFound';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';

interface ILocalProps {
  error?: any;
}

class NotFoundPage extends React.PureComponent<ILocalProps> {
  public render() {
    const { error } = this.props;
    return (
      <AuthorizedLayout>
        {error ? <PageCommunicationError error={error} /> : <GenericNotFound />}
      </AuthorizedLayout>
    );
  }
}

export default NotFoundPage;
