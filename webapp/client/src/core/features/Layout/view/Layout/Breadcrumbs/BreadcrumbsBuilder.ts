import { IRoute } from 'core/shared/routes/makeRoute';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

interface IBreadcrumbs {
  checkLoaded: () => boolean;
  toArray(): Array<IBreadcrumbItem<any>>;
}

interface IBreadcrumbItem<T> {
  redirectPath: string;
  isActive: boolean;
  checkLoaded: () => boolean;
  getName: () => string;
}

export interface IBreadcrumbsBuilder {
  then: <T>(itemSetting: IThenBreadcrumItemSetting<T>) => IBreadcrumbsBuilder;
  thenOr: (
    itemSettings: Array<IThenOrBreadcrumItemSetting<any>>
  ) => IBreadcrumbsBuilder;
  build: (url: string) => IBreadcrumbs;
}

type IThenBreadcrumItemSetting<T> =
  | Omit<ICommonBreadcrumItemSetting<T>, 'core/shared/routes'> & {
      type: 'alwaysDisplayed';
      route: IRoute<T, any, any>;
    }
  | ICommonBreadcrumItemSetting<T> & {
      type: 'multiple';
      routes: Array<IRoute<T, any, any>>;
      redirectTo: IRoute<T, any, any>;
    }
  | ICommonBreadcrumItemSetting<T> & {
      type: 'single';
      route: IRoute<T, any, any>;
    };

type IThenOrBreadcrumItemSetting<T> = ICommonBreadcrumItemSetting<T> & {
  type: 'single';
  route: IRoute<T, any, any>;
};

interface ICommonBreadcrumItemSetting<T> {
  getName: (params: T) => string;
  checkLoaded?: (params: T) => boolean;
}

type BreadcrumbsBuilderItem =
  | { type: 'then'; value: IThenBreadcrumItemSetting<any> }
  | { type: 'thenOr'; value: Array<IThenOrBreadcrumItemSetting<any>> };

export default function BreadcrumbsBuilder(
  items: BreadcrumbsBuilderItem[] = []
): IBreadcrumbsBuilder {
  return {
    then: itemSetting =>
      BreadcrumbsBuilder(items.concat({ type: 'then', value: itemSetting })),
    thenOr: itemsSetting =>
      BreadcrumbsBuilder(items.concat({ type: 'thenOr', value: itemsSetting })),
    build: pathname => {
      // TODO REFACTOR IT
      const breadcrumbs: Array<IBreadcrumbItem<any>> = items
        .map(item => {
          const res = (() => {
            if (item.type === 'then') {
              switch (item.value.type) {
                case 'single': {
                  return {
                    itemSetting: item.value,
                    appropriateRoute: item.value.route.getMatch(pathname, false)
                      ? item.value.route
                      : undefined,
                  };
                }
                case 'alwaysDisplayed': {
                  return {
                    itemSetting: item.value,
                    appropriateRoute: item.value.route,
                  };
                }
                case 'multiple': {
                  return {
                    itemSetting: item.value,
                    appropriateRoute: item.value.routes.find(({ getMatch }) =>
                      getMatch(pathname, false)
                    ),
                  };
                }
                default:
                  return exhaustiveCheck(item.value, '');
              }
            }

            const res = item.value.find(_itemSetting =>
              _itemSetting.route.getMatch(pathname, false)
            );
            if (!res) {
              return null;
            }
            return {
              itemSetting: res,
              appropriateRoute: res.route,
            };
          })();

          if (!res || !res.appropriateRoute) {
            return null;
          }
          const { appropriateRoute, itemSetting } = res;

          const params = appropriateRoute.getMatch(pathname, false);

          const breadcrumbItem: IBreadcrumbItem<any> = {
            isActive: appropriateRoute.getMatch(pathname),
            redirectPath:
              item.type === 'then' && item.value.type === 'multiple'
                ? item.value.redirectTo.getRedirectPath(params)
                : appropriateRoute.getRedirectPath(params!),
            getName: () => itemSetting.getName(params),
            checkLoaded: () =>
              itemSetting.checkLoaded ? itemSetting.checkLoaded(params) : true,
          };
          return breadcrumbItem;
        })
        .filter(Boolean) as Array<IBreadcrumbItem<any>>;

      return makeBreadcrumbs(breadcrumbs);
    },
  };
}

function makeBreadcrumbs(items: Array<IBreadcrumbItem<any>>): IBreadcrumbs {
  return {
    checkLoaded: () => items.every(item => item.checkLoaded()),
    toArray: () => items,
  };
}
