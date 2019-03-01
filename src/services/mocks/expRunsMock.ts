export const expRunsMocks = [
  {
    id: '25c0e2d8-992f-4e32-860c-6d2dd13b03b2',
    project_id: '6a95fea8-5167-4046-ab0c-ef44ce229a78',
    experiment_id: '963c948c-386e-46d8-b42b-74caae4e91d7',
    name: 'Exp Run 1',
    code_version: '0.1.0',
    tags: ['exploratory'],
    description: '...',
    date_created: '1551397426',
    date_updated: '1551397431',
    start_time: '1551397426',
    end_time: '1551397431',
    owner: 'github|14152628',
    hyperparameters: [
      {
        key: 'C',
        value: '100'
      },
      {
        key: 'solver',
        value: 'lbfgs'
      },
      {
        key: 'max_iter',
        value: '100'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple1.gz',
        type: 'IMAGE'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.77'
      }
    ],
    datasets: [{ key: 'train_data', path: '../data/mnist/train.npz', type: 'DATA' }],
    observations: [{ attribute: { key: 'val_train_loss', value: '0.9519083389706082' }, timestamp: '1551397429' }],
    features: [
      {
        name: 'income'
      },
      {
        name: 'marital status'
      },
      {
        name: 'residency'
      },
      {
        name: 'birth year'
      }
    ]
  },
  {
    id: 'a721427c-7b29-4271-ab7d-fcd17209bdd3',
    project_id: '6a95fea8-5167-4046-ab0c-ef44ce229a78',
    experiment_id: '963c948c-386e-46d8-b42b-74caae4e91d7',
    name: 'Exp Run 2',
    code_version: '0.1.0',
    tags: ['outlier-detect'],
    description: '...',
    date_created: '1551397426',
    date_updated: '1551397431',
    start_time: '1551397426',
    end_time: '1551397431',
    owner: 'github|14152628',
    hyperparameters: [
      {
        key: 'C',
        value: '10'
      },
      {
        key: 'solver',
        value: 'lbfgs'
      },
      {
        key: 'max_iter',
        value: '200'
      }
    ],
    datasets: [{ key: 'train_data', path: '../data/mnist/train.npz', type: 'DATA' }],
    observations: [{ attribute: { key: 'val_train_loss', value: '0.9519083389706082' }, timestamp: '1551397429' }],
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple2.gz',
        type: 'MODEL'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.82'
      }
    ],
    features: [
      {
        name: 'income'
      },
      {
        name: 'marital status'
      },
      {
        name: 'residency'
      },
      {
        name: 'birth year'
      }
    ]
  },
  {
    id: '65da02bf-bceb-4a97-b1bd-1b1b2ac89181',
    project_id: '6a95fea8-5167-4046-ab0c-ef44ce229a78',
    experiment_id: '963c948c-386e-46d8-b42b-74caae4e91d7',
    name: 'Exp Run 3',
    code_version: '0.1.2',
    tags: ['outlier-detect'],
    description: '...',
    date_created: '1551397426',
    date_updated: '1551397431',
    start_time: '1551397426',
    end_time: '1551397431',
    owner: 'github|14152628',
    hyperparameters: [
      {
        key: 'C',
        value: '1000'
      },
      {
        key: 'solver',
        value: 'sgd'
      },
      {
        key: 'max_iter',
        value: '10000'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple3.gz',
        type: 'MODEL'
      }
    ],
    datasets: [{ key: 'train_data', path: '../data/mnist/train.npz', type: 'DATA' }],
    observations: [{ attribute: { key: 'val_train_loss', value: '0.9519083389706082' }, timestamp: '1551397429' }],
    metrics: [
      {
        key: 'accuracy',
        value: '0.93'
      }
    ],
    features: [
      {
        name: 'income'
      },
      {
        name: 'marital status'
      },
      {
        name: 'residency'
      },
      {
        name: 'birth year'
      }
    ]
  }
];
