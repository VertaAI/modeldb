import { History } from 'history';

const checkURLSearchParams = (
  history: History,
  expectedURLSearchParams: Record<string, string | null>
) => {
  const urlSearchParams = new URLSearchParams(history.location.search);
  Object.entries(expectedURLSearchParams).forEach(([paramName, paramValue]) => {
    expect(urlSearchParams.get(paramName)).toBe(paramValue);
  });
};

export default checkURLSearchParams;
