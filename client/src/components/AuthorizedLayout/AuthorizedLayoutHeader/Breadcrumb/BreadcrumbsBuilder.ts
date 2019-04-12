import { IRoute } from 'routes';

interface IBreadcrumbs {
  checkLoaded: () => boolean;
  map<T>(f: (item: IBreadcrumbItem<any>) => T): T[];
}

interface IBreadcrumbsBuilder {
  then: <T>(itemSetting: IBreadcrumItemSetting<T>) => IBreadcrumbsBuilder;
  build: (url: string) => IBreadcrumbs;
}

interface IBreadcrumbItem<T> {
  redirectPath: string;
  checkLoaded: () => boolean;
  getName: () => string;
}

interface IBreadcrumItemSetting<T> {
  routes: Array<IRoute<T>>;
  checkLoaded?: (params: T) => boolean;
  getName: (params: T) => string;
}

export default function BreadcrumbsBuilder(
  itemsSetting: Array<IBreadcrumItemSetting<any>> = []
): IBreadcrumbsBuilder {
  return {
    then: itemSetting => BreadcrumbsBuilder(itemsSetting.concat(itemSetting)),
    build: pathname => {
      const breadcrumbs: Array<IBreadcrumbItem<any>> = itemsSetting
        .map(itemSetting => {
          const appropriateRoute = itemSetting.routes.find(({ getMatch }) =>
            Boolean(getMatch(pathname, false))
          )!;

          if (!appropriateRoute) {
            return null;
          }

          const params = appropriateRoute.getMatch(pathname, false);

          const item: IBreadcrumbItem<any> = {
            redirectPath: appropriateRoute.getRedirectPath(params!),
            getName: () => itemSetting.getName(params),
            checkLoaded: () =>
              itemSetting.checkLoaded ? itemSetting.checkLoaded(params) : true,
          };
          return item;
        })
        .filter(Boolean) as Array<IBreadcrumbItem<any>>;

      return makeBreadcrumbs(breadcrumbs);
    },
  };
}

function makeBreadcrumbs(items: Array<IBreadcrumbItem<any>>): IBreadcrumbs {
  return {
    checkLoaded: () => items.every(item => item.checkLoaded()),
    map: f => items.map(item => f(item)),
  };
}
