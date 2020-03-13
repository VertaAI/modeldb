import { IRoute } from 'core/shared/routes/makeRoute';

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
  | Omit<ICommonBreadcrumItemSetting<T>, 'routes'> & {
      route: IRoute<T>;
      isAlwaysDisplayed: true;
    }
  | ICommonBreadcrumItemSetting<T> & { isAlwaysDisplayed?: false };

type IThenOrBreadcrumItemSetting<T> = ICommonBreadcrumItemSetting<T>;

interface ICommonBreadcrumItemSetting<T> {
  routes: Array<IRoute<T, any>>;
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
              if (item.value.isAlwaysDisplayed) {
                return {
                  itemSetting: item.value,
                  appropriateRoute: item.value.route,
                };
              }
              return {
                itemSetting: item.value,
                appropriateRoute: item.value.routes.find(({ getMatch }) =>
                  Boolean(getMatch(pathname, false))
                )!,
              };
            }
            const res = item.value.find(_itemSetting => {
              return Boolean(
                _itemSetting.routes.find(({ getMatch }) =>
                  Boolean(getMatch(pathname, false))
                )
              );
            });

            if (!res) {
              return null;
            }

            return {
              itemSetting: res,
              appropriateRoute: res.routes.find(({ getMatch }) =>
                Boolean(getMatch(pathname, false))
              )!,
            };
          })();

          if (!res || !res.appropriateRoute) {
            return null;
          }
          const { appropriateRoute, itemSetting } = res;

          const params = appropriateRoute.getMatch(pathname, false);

          const breadcrumbItem: IBreadcrumbItem<any> = {
            isActive: appropriateRoute.getMatch(pathname),
            redirectPath: appropriateRoute.getRedirectPath(params!),
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
