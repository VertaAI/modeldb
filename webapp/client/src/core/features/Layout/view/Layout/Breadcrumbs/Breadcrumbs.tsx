import cn from 'classnames';
import { UnregisterCallback } from 'history';
import React from 'react';
import { Link, RouteComponentProps, withRouter } from 'react-router-dom';

import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';

import styles from './Breadcrumbs.module.css';
import { IBreadcrumbsBuilder } from './BreadcrumbsBuilder';

interface ILocalProps {
  breadcrumbBuilder: IBreadcrumbsBuilder;
}

interface ILocalState {
  pathname: string;
}

type AllProps = ILocalProps & RouteComponentProps;

class Breadcrumbs extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    pathname: this.props.history.location.pathname,
  };

  private unlistenCallback: UnregisterCallback | undefined = undefined;

  public componentDidMount() {
    this.unlistenCallback = this.props.history.listen((location, action) => {
      this.setState({
        ...this.state,
        pathname: location.pathname,
      });
    });
  }

  public componentWillUnmount() {
    if (this.unlistenCallback) {
      this.unlistenCallback();
    }
  }

  public render() {
    const breadcrumbs = this.props.breadcrumbBuilder.build(this.state.pathname);
    const breadcrumbsArray = breadcrumbs.toArray();

    if (breadcrumbsArray.length > 0 && breadcrumbsArray[0].isActive) {
      return null;
    }

    return (
      <div className={styles.root}>
        {breadcrumbs.toArray().map((item, i, items) =>
          !item.checkLoaded() ? (
            <div
              key={i}
              style={{ height: '19px', display: 'flex', position: 'relative' }}
            >
              <Preloader variant="dots" />
            </div>
          ) : (
            <div key={i}>
              <Link
                className={cn(styles.breadcrumb_item, {
                  [styles.breadcrumb_item_active]: item.isActive,
                })}
                to={item.redirectPath}
              >
                {item.getName()}
              </Link>
              {items.length - 1 !== i && (
                <Icon className={styles.arrow} type="arrow-right" />
              )}
            </div>
          )
        )}
      </div>
    );
  }
}

export type IBreadcrumbProps = AllProps;
export { Breadcrumbs as Breadcrumb };
export default withRouter(Breadcrumbs);
