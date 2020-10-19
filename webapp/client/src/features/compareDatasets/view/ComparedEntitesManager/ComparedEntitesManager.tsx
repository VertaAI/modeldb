import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import IdView from 'shared/view/elements/IdView/IdView';
import {
  ComparedDatasetVersionIds,
  selectComparedEntityIds,
  unselectEntityForComparing,
} from '../../store';
import { IApplicationState } from 'setup/store/store';

import styles from './ComparedEntitesManager.module.css';
import CompareEntitiesButton from './CompareEntitiesButton/CompareEntitiesButton';

interface ILocalProps {
  containerId: string;
  getCompareUrl(comparedEntities: Required<ComparedDatasetVersionIds>): string;
}

interface IPropsFromState {
  comparedEntityIds: ComparedDatasetVersionIds;
}

interface IActionProps {
  unselectEntityForComparing: typeof unselectEntityForComparing;
}

type AllProps = IPropsFromState & ILocalProps & IActionProps;

class ComparedEntitesManager extends React.PureComponent<AllProps> {
  public render() {
    const { comparedEntityIds, getCompareUrl } = this.props;
    return (
      <div className={styles.root}>
        <div className={styles.compare_entities_button}>
          <CompareEntitiesButton
            containerId={this.props.containerId}
            getCompareUrl={getCompareUrl}
          />
        </div>
        <div className={styles.items}>
          {comparedEntityIds.map(id => (
            <div
              className={styles.item}
              key={id}
              title={id}
              data-test="entity-for-comparing"
            >
              <span
                className={styles.item_unselect}
                onClick={this.makeUnselectEntityForComparing(id!)}
              >
                x
              </span>
              <IdView value={id!} additionalClassName={styles.item_value} />
            </div>
          ))}
        </div>
      </div>
    );
  }

  @bind
  private makeUnselectEntityForComparing(modelRecordId: string) {
    return () =>
      this.props.unselectEntityForComparing({
        projectId: this.props.containerId,
        modelRecordId,
      });
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    comparedEntityIds: selectComparedEntityIds(state, localProps.containerId),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators({ unselectEntityForComparing }, dispatch);

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ComparedEntitesManager);
