import { convertServerConfigDiff } from '../index';

describe('convertServerConfigDiff', () => {
  describe('added diff type', () => {
    it('should convert server model to client model', () => {
      const addedHyperparameters = {
        location: ['added-hyperparameters'],
        status: 'ADDED',
        config: {
          hyperparameters: [
            {
              status: 'ADDED',
              B: {
                name: 'string',
                value: {
                  int_value: '6',
                },
              },
            },
            {
              status: 'ADDED',
              B: {
                name: 'adfadg',
                value: {
                  int_value: '6134',
                },
              },
            },
          ],
        },
      };
      const addedHyperparametersSet = {
        location: ['added-hyperparameters'],
        status: 'MODIFIED',
        config: {
          hyperparameter_set: [
            {
              status: 'ADDED',
              B: {
                name: 'string',
                continuous: {
                  interval_begin: {
                    int_value: '2',
                  },
                  interval_end: {
                    int_value: '3',
                  },
                  interval_step: {
                    int_value: '4',
                  },
                },
              },
            },
          ],
          hyperparameters: [
            {
              status: 'DELETED',
              A: {
                name: 'string',
                value: {
                  int_value: '6134134',
                },
              },
            },
            {
              status: 'DELETED',
              A: {
                name: 'adfadg',
                value: {
                  int_value: '6134',
                },
              },
            },
            {
              status: 'DELETED',
              A: {
                name: 'adgfadgadgadg',
                value: {
                  int_value: '6134',
                },
              },
            },
          ],
        },
      };

      expect(
        convertServerConfigDiff(addedHyperparameters as any)
      ).toMatchSnapshot();
      expect(
        convertServerConfigDiff(addedHyperparametersSet as any)
      ).toMatchSnapshot();
    });
  });

  describe('modified diff type', () => {
    it('should convert server model to client model', () => {
      const modifiedDiff = {
        location: ['added-hyperparameters'],
        status: 'MODIFIED',
        config: {
          hyperparameters: [
            {
              status: 'MODIFIED',
              A: {
                name: 'string',
                value: {
                  int_value: '6',
                },
              },
              B: {
                name: 'string',
                value: {
                  int_value: '6134134',
                },
              },
            },
            {
              status: 'ADDED',
              B: {
                name: 'adgfadgadgadg',
                value: {
                  int_value: '6134',
                },
              },
            },
          ],
        },
      };
      expect(convertServerConfigDiff(modifiedDiff as any)).toMatchSnapshot();
    });
  });
});
