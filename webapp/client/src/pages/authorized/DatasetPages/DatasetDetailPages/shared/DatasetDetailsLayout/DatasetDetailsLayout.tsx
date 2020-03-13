import * as React from 'react';
import { RouteComponentProps, withRouter, Omit } from 'react-router';

import { IAuthorizedLayoutLocalProps } from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import PagesTabs, {
  IPagesTabsLocalProps,
} from 'core/shared/view/pages/PagesTabs/PagesTabs';
import routes, { GetRouteParams } from 'routes';

import DatasetsPagesLayout from '../../../shared/DatasetsPagesLayout/DatasetsPagesLayout';
import styles from './DatasetDetailsLayout.module.css';

interface ILocalProps {
  pagesTabsSettings?: Omit<IPagesTabsLocalProps, 'tabs'>;
  children: IAuthorizedLayoutLocalProps['children'];
}

type AllProps = ILocalProps &
  Pick<IAuthorizedLayoutLocalProps, 'filterBarSettings'> &
  RouteComponentProps<GetRouteParams<typeof routes.datasetSummary>>;

class DatasetDetailsLayout extends React.PureComponent<AllProps> {
  public render() {
    const {
      filterBarSettings,
      pagesTabsSettings,
      children,
      match: {
        params: { datasetId },
      },
    } = this.props;
    return (
      <DatasetsPagesLayout filterBarSettings={filterBarSettings}>
        <div className={styles.root}>
          <div className={styles.tabs}>
            <PagesTabs
              tabs={[
                {
                  label: 'Summary',
                  to: routes.datasetSummary.getRedirectPathWithCurrentWorkspace(
                    { datasetId }
                  ),
                },
                {
                  label: 'Versions',
                  to: routes.datasetVersions.getRedirectPathWithCurrentWorkspace(
                    { datasetId }
                  ),
                },
              ]}
              {...pagesTabsSettings}
            />
          </div>
          <div className={styles.content}>{children}</div>
        </div>
      </DatasetsPagesLayout>
    );
  }
}

export default withRouter(DatasetDetailsLayout);
