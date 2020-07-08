import * as React from 'react';

import { FilterManager, IFilterContext } from 'features/filter';
import { Icon, IconType } from 'shared/view/elements/Icon/Icon';
import usePlacerUnderHeader from 'shared/view/pages/usePlacerUnderHeader';

import LayoutLink from './LayoutLink/LayoutLink';
import styles from './Sidebar.module.css';

interface ILocalProps {
  isCollapsed: boolean;
  filterBarSettings?: {
    context: IFilterContext;
    title: string;
  };
  mainNavigationRoutes: IMainNavigationRoute[];
  onToggleCollapsingSidebar(): void;
}

type AllProps = ILocalProps;

export interface IMainNavigationRoute {
  to: string;
  iconType: IconType;
  text: string;
  isDisabled?: boolean;
}

const Sidebar = React.memo((props: AllProps) => {
  const {
    filterBarSettings,
    isCollapsed,
    mainNavigationRoutes,
    onToggleCollapsingSidebar,
  } = props;
  const { height, horizontalScrollOffset } = usePlacerUnderHeader({
    position: 'left',
  });

  return (
    <div
      className={styles.root}
      style={{ height, marginLeft: horizontalScrollOffset }}
    >
      <nav className={styles.mainNavigation}>
        {mainNavigationRoutes.map(
          ({ to, iconType, text, isDisabled }, i) =>
            !isDisabled && (
              <LayoutLink
                to={to}
                iconType={iconType}
                isCollapsed={isCollapsed}
                key={i}
              >
                {text}
              </LayoutLink>
            )
        )}
      </nav>
      {filterBarSettings && (
        <div className={styles.filters}>
          <FilterManager
            {...filterBarSettings}
            isCollapsed={isCollapsed}
            onExpandSidebar={onToggleCollapsingSidebar}
          />
        </div>
      )}
      <div className={styles.additionalLinks}>
        <LayoutLink
          isExternal={true}
          to="https://docs.verta.ai/"
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
});

export default Sidebar;
