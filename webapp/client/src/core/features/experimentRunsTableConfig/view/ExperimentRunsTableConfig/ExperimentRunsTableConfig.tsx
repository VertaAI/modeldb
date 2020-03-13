import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';

import Checkbox from 'core/shared/view/elements/Checkbox/Checkbox';
import ClickOutsideListener from 'core/shared/view/elements/ClickOutsideListener/ClickOutsideListener';
import { Icon } from 'core/shared/view/elements/Icon/Icon';

import {
  IColumnConfig,
  selectColumnConfig,
  toggleColumnVisibility,
  IExperimentRunsTableConfigRootState,
} from '../../store';
import styles from './ExperimentRunsTableConfig.module.css';

interface ILocalState {
  isOpened: boolean;
}

const mapStateToProps = (state: IExperimentRunsTableConfigRootState) => ({
  columnConfig: selectColumnConfig(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      toggleColumnVisibility,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;
class DashboardConfig extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = { isOpened: false };

  public componentDidMount() {
    this.setState({ isOpened: false });
  }

  public render() {
    const { columnConfig } = this.props;
    return (
      <ClickOutsideListener onClickOutside={this.handleClickOutside}>
        <div className={styles.root}>
          <div className={styles.user_bar} onClick={this.toggleMenu}>
            <Icon type="cog" className={styles.dashboard_cog} />
          </div>
          {this.state.isOpened ? (
            <div className={styles.drop_down}>
              <h4 className={styles.title}>Add/Drop Columns</h4>
              <div className={styles.menu_items}>
                {R.sortBy(({ order }) => order, R.values(columnConfig)).map(
                  columnData => (
                    <label className={styles.menu_item} key={columnData.name}>
                      <div className={styles.menu_item_checkbox}>
                        <Checkbox
                          value={columnData.isShown}
                          size="medium"
                          onChange={this.makeColumnsUpdateHandler(
                            columnData.name
                          )}
                        />
                      </div>
                      <div className={styles.menu_item_label}>
                        {columnData.label}
                      </div>
                    </label>
                  )
                )}
              </div>
            </div>
          ) : (
            ''
          )}
        </div>
      </ClickOutsideListener>
    );
  }

  @bind
  private handleClickOutside() {
    this.setState({ isOpened: false });
  }

  @bind
  private makeColumnsUpdateHandler(columnName: keyof IColumnConfig) {
    return () => {
      this.props.toggleColumnVisibility({ columnName });
    };
  }

  @bind
  private toggleMenu() {
    this.setState(prevState => ({ isOpened: !prevState.isOpened }));
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(onClickOutside(DashboardConfig));
