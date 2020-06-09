import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import CompareClickAction from 'features/compareEntities/view/CompareEntities/CompareClickAction/CompareClickAction';
import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import { ICommunication } from 'core/shared/utils/redux/communication';
import DeleteFAIWithLabel from 'core/shared/view/elements/DeleteFaiWithLabel/DeleteFaiWithLabel';
import GroupFai from 'core/shared/view/elements/GroupFai/GroupFai';
import { ShowCommentsButton } from 'features/comments';
import {
  deleteExperimentRun,
  selectDeletingExperimentRun,
} from 'features/experimentRuns/store';
import { IApplicationState } from 'store/store';

import { IRow } from '../types';
import styles from './Actions.module.css';

interface ILocalProps {
  row: IRow;
}

interface IPropsFromState {
  deletingExperimentRun: ICommunication;
}

interface IActionProps {
  deleteExperimentRun: typeof deleteExperimentRun;
}

type AllProps = ILocalProps & IActionProps & IPropsFromState;

class ActionsColumn extends React.PureComponent<AllProps> {
  private refObject = React.createRef<HTMLDivElement>();

  public componentDidUpdate(prevProps: AllProps) {
    if (
      !prevProps.deletingExperimentRun.isRequesting &&
      this.props.deletingExperimentRun.isRequesting
    ) {
      this.refObject.current!.closest('div')!.classList.add(styles.deleting);
    }

    if (
      prevProps.deletingExperimentRun.isRequesting &&
      !this.props.deletingExperimentRun.isRequesting
    ) {
      this.refObject.current!.closest('div')!.classList.remove(styles.deleting);
    }
  }

  public render() {
    const {
      row: {
        experimentRun: { id, projectId, name, artifacts },
      },
    } = this.props;

    return (
      <div
        className={styles.root}
        ref={this.refObject}
        data-test="experiment-run"
      >
        <div className={styles.group_fai_block}>
          <GroupFai
            groupFai={[
              requiredProps => (
                <CompareClickAction
                  containerId={projectId}
                  enitityId={id}
                  {...requiredProps}
                />
              ),
              requiredProps => (
                <ShowCommentsButton
                  buttonType="faiWithLabel"
                  entityInfo={{ id, name }}
                  {...requiredProps}
                />
              ),
              requiredProps => (
                <WithCurrentUserActionsAccesses
                  entityId={id}
                  entityType="experimentRun"
                  actions={['delete']}
                >
                  {({ actionsAccesses }) =>
                    actionsAccesses.delete && (
                      <DeleteFAIWithLabel
                        theme="blue"
                        confirmText="Are you sure?"
                        dataTest="delete-experiment-run-button"
                        onDelete={this.deleteExperimentRun}
                        {...requiredProps}
                      />
                    )
                  }
                </WithCurrentUserActionsAccesses>
              ),
            ]}
          />
        </div>
      </div>
    );
  }

  @bind
  private deleteExperimentRun() {
    this.props.deleteExperimentRun(
      this.props.row.experimentRun.projectId,
      this.props.row.experimentRun.id
    );
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    deletingExperimentRun: selectDeletingExperimentRun(
      state,
      localProps.row.experimentRun.id
    ),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators({ deleteExperimentRun }, dispatch);
};

const connectedActionsColumn = connect(
  mapStateToProps,
  mapDispatchToProps
)(ActionsColumn) as any;

export default connectedActionsColumn;
