import * as R from 'ramda';
import * as React from 'react';
import { connect } from 'react-redux';

import * as EntitiesActions from 'shared/models/EntitiesActions';
import { IApplicationState } from 'store/store';

import * as Common from 'shared/models/Common';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import * as DatasetsStore from 'features/datasets/store';
import * as DatasetVersionsStore from 'features/datasetVersions/store';
import * as ExperimentRunsStore from 'features/experimentRuns/store';
import * as ExperimentsStore from 'features/experiments/store';
import * as ProjectsStore from 'features/projects/store';

export interface ILocalProps<T extends EntitiesActions.UserEntityAction[]> {
  entityType: Exclude<Common.EntityType, 'endpoint'>;
  entityId: string;
  actions: T;
  children: (opts: {
    isLoading: boolean;
    actionsAccesses: IActionAccesses<T[number]>;
  }) => React.ReactNode;
}

export type IActionAccesses<
  SpecificUserEntityAction extends EntitiesActions.UserEntityAction
> = Record<SpecificUserEntityAction, boolean>;

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps<EntitiesActions.UserEntityAction[]>
) => {
  return {};
};

type AllProps<T extends EntitiesActions.UserEntityAction[]> = ILocalProps<T> &
  ReturnType<typeof mapStateToProps>;

class WithCurrentUserActionsAccesses<
  T extends EntitiesActions.UserEntityAction[]
> extends React.PureComponent<AllProps<T>> {
  public render() {
    const { actions } = this.props;

    const actionsAccesses = R.fromPairs(
      actions.map(action => [action, true])
    ) as IActionAccesses<EntitiesActions.UserEntityAction>;

    return this.props.children({
      actionsAccesses,
      isLoading: false,
    });
  }
}

export default connect(mapStateToProps)(WithCurrentUserActionsAccesses);
