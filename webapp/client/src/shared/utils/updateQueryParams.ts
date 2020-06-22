import { History } from 'history';

const updateQueryParamsForHistory = <QueryParams>(
  history: History,
  params: { [P in keyof QueryParams]: Required<QueryParams>[P] | null }
) => {
  history.push({
    search: updateQueryParamsForLocation(history.location, params),
  });
};

export const updateQueryParamsForLocation = <QueryParams>(
  location: { search: string },
  params: { [P in keyof QueryParams]: Required<QueryParams>[P] | null }
): string => {
  const queryParams = new URLSearchParams(location.search);
  Object.entries(params).forEach(([name, value]) => {
    if (!value) {
      queryParams.delete(name);
    } else {
      queryParams.set(name, value as string);
    }
  });
  return queryParams.toString();
};

export default updateQueryParamsForHistory;
