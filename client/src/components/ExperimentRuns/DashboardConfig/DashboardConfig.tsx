import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';

import Checkbox from 'components/shared/Checkbox/Checkbox';
import Icon from 'components/shared/Icon/Icon';
import ModelRecord from 'models/ModelRecord';
import {
  IColumnConfig,
  selectColumnConfig,
  updateDashboardConfig,
} from 'store/dashboard-config';
import { selectExperimentRuns } from 'store/experiment-runs';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import styles from './DashboardConfig.module.css';

interface ILocalState {
  isOpened: boolean;
}

interface IPropsFromState {
  columnConfig: IColumnConfig;
  experimentRuns: ModelRecord[] | null;
}

type AllProps = IConnectedReduxProps & IPropsFromState;
class DashboardConfig extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = { isOpened: false };

  public componentDidMount() {
    this.setState({ isOpened: false });
  }

  public render() {
    const { columnConfig } = this.props;
    return (
      <div className={styles.root}>
        <div className={styles.user_bar} onClick={this.toggleMenu}>
          <Icon type="cog" className={styles.dashboard_cog} />
        </div>
        {this.state.isOpened ? (
          <div className={styles.drop_down}>
            <h4 className={styles.title}>Add/Drop Columns</h4>
            <div className={styles.menu_items}>
              {Array.from(columnConfig.values()).map(element => (
                <label className={styles.menu_item} key={element.name}>
                  <div className={styles.menu_item_checkbox}>
                    <Checkbox
                      value={element.checked}
                      size="medium"
                      onChange={this.makeColumnsUpdateHandler(element.name)}
                    />
                  </div>
                  <div className={styles.menu_item_label}>{element.label}</div>
                </label>
              ))}
            </div>
          </div>
        ) : (
          ''
        )}
      </div>
    );
  }

  public handleClickOutside(ev: MouseEvent) {
    this.setState({ isOpened: false });
  }

  @bind
  public makeColumnsUpdateHandler(checkboxName: string) {
    return () => {
      let activeColumns = new Map(this.props.columnConfig);
      const checkedElement = activeColumns.get(checkboxName);
      if (checkedElement !== undefined) {
        checkedElement.checked = !checkedElement.checked;
        activeColumns = activeColumns.set(checkboxName, checkedElement);
      }
      this.props.dispatch(updateDashboardConfig(activeColumns));
    };
  }

  @bind
  private toggleMenu() {
    this.setState(prevState => ({ isOpened: !prevState.isOpened }));
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  columnConfig: selectColumnConfig(state),
  experimentRuns: selectExperimentRuns(state),
});

export default connect(mapStateToProps)(onClickOutside(DashboardConfig));
