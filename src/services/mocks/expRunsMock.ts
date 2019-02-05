export const expRunsMocks = [
  {
    id: '25c0e2d8-992f-4e32-860c-6d2dd13b03b2',
    project_id: '6a95fea8-5167-4046-ab0c-ef44ce229a78',
    experiment_id: '963c948c-386e-46d8-b42b-74caae4e91d7',
    name: 'Exp Run 1',
    code_version: '0.1.0',
    tags: ['exploratory'],
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
        path: 'output/census_logreg_simple1.gz'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.77'
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
    id: 'a721427c-7b29-4271-ab7d-fcd17209bdd3',
    project_id: '6a95fea8-5167-4046-ab0c-ef44ce229a78',
    experiment_id: '963c948c-386e-46d8-b42b-74caae4e91d7',
    name: 'Exp Run 2',
    code_version: '0.1.0',
    tags: ['outlier-detect'],
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
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple2.gz'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.82'
      }
    ],
    observations: [
      {
        attribute: {
          key: 'epoch_val_acc',
          value: '0.7'
        }
      },
      {
        attribute: {
          key: 'epoch_val_acc',
          value: '0.8'
        }
      },
      {
        attribute: {
          key: 'epoch_val_acc',
          value: '0.9'
        }
      },
      {
        attribute: {
          key: 'epoch_val_acc',
          value: '0.95'
        }
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
        path: 'output/census_logreg_simple3.gz'
      }
    ],
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
