import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  ILayoutRootState,
  selectIsCollapsedSidebar,
  toggleCollapsingSidebar,
} from '../../store';
import Breadcrumbs from './Breadcrumbs/Breadcrumbs';
import { IBreadcrumbsBuilder } from './Breadcrumbs/BreadcrumbsBuilder';
import styles from './Layout.module.css';
import LayoutHeader from './LayoutHeader/LayoutHeader';
import Sidebar from './Sidebar/Sidebar';

type ILocalProps = {
  breadcrumbsBuilder?: IBreadcrumbsBuilder;
  children: Exclude<React.ReactNode, null | undefined>;
  additionalSidebar?: React.ReactNode;
  rightContent?: React.ReactNode;
} & Pick<
  React.ComponentProps<typeof Sidebar>,
  'filterBarSettings' | 'mainNavigationRoutes'
>;

const mapStateToProps = (state: ILayoutRootState) => ({
  isCollapsedSidebar: selectIsCollapsedSidebar(state),
});

const actionProps = { toggleCollapsingSidebar };

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  typeof actionProps;

class AuthorizedLayout extends React.Component<AllProps> {
  public render() {
    const {
      filterBarSettings,
      isCollapsedSidebar,
      breadcrumbsBuilder,
      additionalSidebar,
      mainNavigationRoutes,
      children,
      rightContent,
    } = this.props;
    return (
      <div
        className={cn(styles.layout, {
          [styles.collapsedSidebar]: isCollapsedSidebar,
          [styles.withAdditionalSidebar]: Boolean(additionalSidebar),
        })}
      >
        <div className={styles.header}>
          <LayoutHeader rightContent={rightContent} />
        </div>
        <div className={styles.content_area}>
          <div className={styles.sidebar}>
            <Sidebar
              filterBarSettings={filterBarSettings}
              isCollapsed={isCollapsedSidebar}
              mainNavigationRoutes={mainNavigationRoutes}
              onToggleCollapsingSidebar={this.onToggleCollapsingSidebar}
            />
          </div>
          {additionalSidebar && (
            <div className={styles.additionalSidebar}>{additionalSidebar}</div>
          )}
          <div className={styles.contentWithBreadcrumbs}>
            {breadcrumbsBuilder && (
              <div className={styles.breadcrumbs}>
                <Breadcrumbs breadcrumbBuilder={breadcrumbsBuilder} />
              </div>
            )}
            <div className={styles.content}>{children}</div>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private onToggleCollapsingSidebar() {
    this.props.toggleCollapsingSidebar();
  }
}

export type IAuthorizedLayoutLocalProps = ILocalProps;
export default connect(
  mapStateToProps,
  actionProps
)(AuthorizedLayout);
