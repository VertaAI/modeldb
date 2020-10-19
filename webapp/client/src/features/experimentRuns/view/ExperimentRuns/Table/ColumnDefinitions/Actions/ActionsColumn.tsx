import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import { ICommunication } from 'shared/utils/redux/communication';
import DeleteFAIWithLabel from 'shared/view/elements/DeleteFaiWithLabel/DeleteFaiWithLabel';
import GroupFai from 'shared/view/elements/GroupFai/GroupFai';
import { ShowCommentsButtonWithAuthor } from 'features/comments';
import {
  deleteExperimentRun,
  selectDeletingExperimentRun,
} from 'features/experimentRuns/store';
import { IApplicationState } from 'setup/store/store';

import { IRow } from '../types';
import styles from './Actions.module.css';
import CompareClickActionView from 'features/compareModels/view/CompareEntities/CompareClickAction/CompareClickAction';

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
        experimentRun: { id, projectId, name },
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
                <CompareClickActionView
                  containerId={projectId}
                  enitityId={id}
                  {...requiredProps}
                />
              ),
              (requiredProps) => (
                <ShowCommentsButtonWithAuthor
                  buttonType="faiWithLabel"
                  entityInfo={{ id, name }}
                  {...requiredProps}
                />
              ),
              (requiredProps) => (
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
