import { IDeployedStatusInfo } from 'models/Deploy';

export const deployedStatusInfoData: IDeployedStatusInfo['data'] = {
  token: 'token',
  uptime: 34,
  modelApi: {
    modelType: 'scikit',
    pythonVersion: 2,
    input: {
      type: 'list',
      fields: [
        { name: 'age', type: 'float' },
        { name: 'gender', type: 'float' },
        { name: 'zipcode', type: 'float' },
        { name: 'city', type: 'float' },
        { name: 'gender', type: 'float' },
        { name: 'zipcode', type: 'float' },
        { name: 'city', type: 'float' },
      ],
    },
    output: {
      name: 'class1_prob',
      type: 'float',
    },
  },
  type: 'rest',
  api: 'https://verta.io/234wfogsfas/fsfbgs',
};
