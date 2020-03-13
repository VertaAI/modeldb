import * as React from 'react';

import PagesTabs from 'core/shared/view/pages/PagesTabs/PagesTabs';
import routes from 'routes';

interface ILocalProps {
  projectId: string;
  rightContent?: React.ReactNode;
  isDisabled?: boolean;
}

export default class ProjectPageTabs extends React.Component<ILocalProps> {
  public render() {
    const { projectId, isDisabled = false, rightContent } = this.props;

    return (
      <PagesTabs
        tabs={[
          {
            label: 'Summary',
            to: routes.projectSummary.getRedirectPathWithCurrentWorkspace({
              projectId,
            }),
          },
          {
            label: 'Experiments',
            to: routes.experiments.getRedirectPathWithCurrentWorkspace({
              projectId,
            }),
          },
          {
            label: 'Experiment Runs',
            to: routes.experimentRuns.getRedirectPathWithCurrentWorkspace({
              projectId,
            }),
          },
          {
            label: 'Charts',
            to: routes.charts.getRedirectPathWithCurrentWorkspace({
              projectId,
            }),
          },
        ]}
        isDisabled={isDisabled}
        rightContent={rightContent}
      />
    );
  }
}

export type IProjectPageTabsLocalProps = ILocalProps;
