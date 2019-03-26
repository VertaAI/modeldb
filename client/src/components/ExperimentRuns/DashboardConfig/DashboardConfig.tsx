import * as React from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';

import { IDashboardConfigState, updateDashboardConfig } from 'store/dashboard-config';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import ModelRecord from 'models/ModelRecord';

import styles from './DashboardConfig.module.css';

interface ILocalState {
  isOpened: boolean;
}

interface IPropsFromState {
  dashboardConfig: IDashboardConfigState;
  experimentRuns: ModelRecord[] | undefined;
}

type AllProps = IConnectedReduxProps & IPropsFromState;
class DashboardConfig extends React.Component<AllProps, ILocalState> {
  public constructor(props: AllProps) {
    super(props);
    this.state = {
      isOpened: false
    };
  }

  public render() {
    const dashboardConfigMap = this.props.dashboardConfig.columnConfig;
    return (
      <div className={styles.root}>
        <div className={styles.user_bar} onClick={this.toggleMenu}>
          <div className={styles.dashboard_cog}>
            <i className="fa fa-cog" />
          </div>
        </div>
        {this.state.isOpened ? (
          <div className={styles.drop_down}>
            <h4 className={styles.title}> Add/Drop Columns </h4>
            <div className={styles.menu_item}>
              {Array.from(dashboardConfigMap.values()).map((element: any) => (
                <label key={element.name} style={{ display: 'block' }} className={styles.container}>
                  <input
                    className={styles.input_style}
                    type="checkbox"
                    name={element.name}
                    checked={element.checked}
                    onChange={this.handleColumnsUpdate}
                  />
                  <span className={styles.checkmark} />
                  &nbsp; &nbsp;
                  {element.label}
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

  public componentDidMount() {
    this.setState({ isOpened: false });
  }

  public handleClickOutside(ev: MouseEvent) {
    this.setState({ isOpened: false });
  }

  public handleColumnsUpdate = (ev: any) => {
    let activeColumns = this.props.dashboardConfig.columnConfig;
    const checkedElement = activeColumns.get(ev.target.name);
    if (checkedElement !== undefined) {
      checkedElement.checked = !checkedElement.checked;
      activeColumns = activeColumns.set(ev.target.name, checkedElement);
    }
    this.props.dispatch(updateDashboardConfig(activeColumns));
  };

  private toggleMenu = () => {
    this.setState(prevState => ({ isOpened: !prevState.isOpened }));
  };
}

const mapStateToProps = ({ dashboardConfig, experimentRuns }: IApplicationState) => ({
  dashboardConfig,
  experimentRuns: experimentRuns.data
});

export default connect(mapStateToProps)(onClickOutside(DashboardConfig));
