import { ReactWrapper } from 'enzyme';

export const createWaitForPred = (
  pred: (rootComponent: ReactWrapper) => boolean,
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

      if (pred(rootComponent.update())) {
        clearInterval(intervalId);
        return resolve(rootComponent);
      }

      remainingTime = remainingTime - interval;
    }, interval);
  });
};
