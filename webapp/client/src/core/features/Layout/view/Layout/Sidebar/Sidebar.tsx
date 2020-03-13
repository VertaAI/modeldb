import { bind } from 'decko';
import * as React from 'react';

import { FilterManager, IFilterContext } from 'core/features/filter';
import { Icon, IconType } from 'core/shared/view/elements/Icon/Icon';

import LayoutLink from './LayoutLink/LayoutLink';
import styles from './Sidebar.module.css';

interface ILocalProps {
  isCollapsed: boolean;
  filterBarSettings?: {
    context: IFilterContext;
    placeholderText: string;
    withFilterIdsSection?: boolean;
  };
  mainNavigationRoutes: IMainNavigationRoute[];
  onToggleCollapsingSidebar(): void;
}

type AllProps = ILocalProps;

interface ILocalState {
  height: string;
  marginLeft: string;
}

export interface IMainNavigationRoute {
  to: string;
  iconType: IconType;
  text: string;
}

class Sidebar extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    height: `calc(100% - ${this.getHeaderHeight()})`,
    marginLeft: '0',
  };

  public componentDidMount() {
    window.addEventListener('scroll', this.updateStateOnScroll);
    this.updateStateOnScroll();
  }

  public componentWillUnmount() {
    window.removeEventListener('scroll', this.updateStateOnScroll);
  }

  public render() {
    const {
      filterBarSettings,
      isCollapsed,
      mainNavigationRoutes,
      onToggleCollapsingSidebar,
    } = this.props;

    return (
      <div className={styles.root} style={this.state}>
        <nav className={styles.mainNavigation}>
          {mainNavigationRoutes.map(({ to, iconType, text }, i) => (
            <LayoutLink
              to={to}
              iconType={iconType}
              isCollapsed={isCollapsed}
              key={i}
            >
              {text}
            </LayoutLink>
          ))}
        </nav>
        {filterBarSettings && (
          <div className={styles.filters}>
            <FilterManager
              {...filterBarSettings}
              isDisabled={false}
              isCollapsed={isCollapsed}
              onExpandSidebar={this.props.onToggleCollapsingSidebar}
            />
          </div>
        )}
        <div className={styles.additionalLinks}>
          <LayoutLink
            isExternal={true}
            to="https://verta.readme.io/docs"
            iconType="help-outline"
            isCollapsed={isCollapsed}
          >
            Docs
          </LayoutLink>
          <LayoutLink
            isExternal={true}
            to="https://www.verta.ai"
            iconType="link"
            isCollapsed={isCollapsed}
          >
            Verta.ai
          </LayoutLink>
        </div>
        <div
          className={styles.collapseSidebarButton}
          onClick={onToggleCollapsingSidebar}
        >
          <Icon type={isCollapsed ? 'arrow-right' : 'arrow-left'} />
        </div>
      </div>
    );
  }

  @bind
  private updateStateOnScroll() {
    this.updateHeightOnVerticalScroll();
    this.updatePosOnHorizontalScroll();
  }

  @bind
  private updateHeightOnVerticalScroll() {
    const offset = parseInt(this.getHeaderHeight()) - window.scrollY;
    this.setState({
      height: `calc(100% - ${offset < 0 ? 0 : offset}px)`,
    });
  }

  @bind
  private updatePosOnHorizontalScroll() {
    this.setState({
      marginLeft: window.scrollX > 0 ? `-${window.scrollX}px` : '0',
    });
  }

  @bind
  private getHeaderHeight() {
    return getComputedStyle(document.documentElement).getPropertyValue(
      '--header-height'
    );
  }
}

export default Sidebar;
