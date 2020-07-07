import { ReactWrapper } from 'enzyme';

export const createWaitForExpect = (
  expectation: (rootComponent: ReactWrapper) => void | Promise<void>,
  maxTime = 100,
  interval = 10
) => (rootComponent: ReactWrapper) => {
  return new Promise((resolve, reject) => {
    let remainingTime = maxTime;

    const intervalId = setInterval(() => {
      if (remainingTime < 0) {
        clearInterval(intervalId);
        return reject(
          `Expected to find element within ${maxTime}ms, but it was never found.`
        );
      }

      try {
        Promise.resolve(expectation(rootComponent))
          .then(() => resolve())
          .catch(reject);
      } catch (e) {
        reject(e);
      }

      remainingTime = remainingTime - interval;
    }, interval);
  });
};
