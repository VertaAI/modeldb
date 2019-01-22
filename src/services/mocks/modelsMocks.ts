export const modelsMocks = [
  {
    id: 'a5099293-59c8-4829-b653-9538d0c7a2dd',
    projectId: 'cb67e2fc-a1d9-416e-ac8d-9db0470a5003',
    experimentId: 'e678697a-b589-47f1-a9e3-b6931c7f0e82',
    name: 'expt run 1',
    tags: ['tag1', 'tag2'],
    datasets: [
      {
        key: 'input_data',
        path: 'data/credit-default.csv',
        artifactType: 'DATA'
      }
    ],
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
    metrics: [
      {
        key: 'accuracy',
        value: '0.77',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple1.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: 'd43de3a8-df26-4cd4-bcca-c470a2dabc2e',
    projectId: 'cb67e2fc-a1d9-416e-ac8d-9db0470a5003',
    experimentId: 'e678697a-b589-47f1-a9e3-b6931c7f0e82',
    name: 'expt run 2',
    datasets: [
      {
        key: 'input_data',
        path: 'data/credit-default.csv',
        artifactType: 'DATA'
      }
    ],
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
        value: '1000'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.82',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple2.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: 'bbc106df-d692-4f32-9384-68019f6e50bc',
    projectId: 'cb67e2fc-a1d9-416e-ac8d-9db0470a5003',
    experimentId: 'e678697a-b589-47f1-a9e3-b6931c7f0e82',
    name: 'expt run 3',
    datasets: [
      {
        key: 'input_data',
        path: 'data/credit-default.csv',
        artifactType: 'DATA'
      }
    ],
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
    metrics: [
      {
        key: 'accuracy',
        value: '0.93',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/census_logreg_simple3.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: 'cc00c068-0fa0-49ea-bfa2-f88c9c6ec117',
    projectId: '9009ee91-a6d3-4bb7-9bc3-6bdc2ec71f6d',
    experimentId: '9de89516-a5cd-4ddd-8f36-272d140deb02',
    name: 'expt run 1',
    datasets: [
      {
        key: 'input_data',
        path: 'data/reco-default.csv',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'max_trees',
        value: '500'
      },
      {
        key: 'max_depth',
        value: '2'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.89',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/reco_rf_simple1.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: '1f041056-8910-4017-b849-02d360e8f704',
    projectId: '9009ee91-a6d3-4bb7-9bc3-6bdc2ec71f6d',
    experimentId: '9de89516-a5cd-4ddd-8f36-272d140deb02',
    name: 'expt run 2',
    datasets: [
      {
        key: 'input_data',
        path: 'data/reco-default.csv',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'max_trees',
        value: '100'
      },
      {
        key: 'max_depth',
        value: '2'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.71',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/reco_rf_simple2.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: '5e46cdae-9bc9-4b0d-a676-ec2f396e32fe',
    projectId: '9009ee91-a6d3-4bb7-9bc3-6bdc2ec71f6d',
    experimentId: '9de89516-a5cd-4ddd-8f36-272d140deb02',
    name: 'expt run 3',
    datasets: [
      {
        key: 'input_data',
        path: 'data/reco-default.csv',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'max_trees',
        value: '50'
      },
      {
        key: 'max_depth',
        value: '5'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.87',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/reco_rf_simple3.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: 'd298dd86-9e39-40dd-b8d2-58b122bcb99c',
    projectId: '9009ee91-a6d3-4bb7-9bc3-6bdc2ec71f6d',
    experimentId: '9de89516-a5cd-4ddd-8f36-272d140deb02',
    name: 'expt run 4',
    datasets: [
      {
        key: 'input_data',
        path: 'data/reco-default.csv',
        artifactType: 'DATA'
      }
    ],
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
    metrics: [
      {
        key: 'accuracy',
        value: '0.67',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/reco_logreg_simple1.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: '4e3509d0-e503-4e15-a04b-fb3ba3b46d0a',
    projectId: 'f71b4b8f-90d2-4d96-a059-8776e3e2e52a',
    experimentId: '3aaca3a4-b450-482f-af66-2d68d24143a2',
    name: 'expt run 1',
    datasets: [
      {
        key: 'input_data',
        path: 'data/imgnet.gz',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'hidden-1',
        value: '500'
      },
      {
        key: 'hidden-2',
        value: '200'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.55',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/my-simple-nn.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: '95817432-f53f-46af-abd9-056fd6a2f02d',
    projectId: 'f71b4b8f-90d2-4d96-a059-8776e3e2e52a',
    experimentId: '3aaca3a4-b450-482f-af66-2d68d24143a2',
    name: 'expt run 2',
    datasets: [
      {
        key: 'input_data',
        path: 'data/imgnet.gz',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'hidden-1',
        value: '300'
      },
      {
        key: 'hidden-2',
        value: '200'
      },
      {
        key: 'hidden-3',
        value: '350'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.65',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/my-simple-nn2.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: 'ed2f00a8-a926-46f0-9004-5502d1432df1',
    projectId: 'f71b4b8f-90d2-4d96-a059-8776e3e2e52a',
    experimentId: '3aaca3a4-b450-482f-af66-2d68d24143a2',
    name: 'expt run 3',
    datasets: [
      {
        key: 'input_data',
        path: 'data/imgnet.gz',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'hidden-1',
        value: '300'
      },
      {
        key: 'hidden-2',
        value: '1000'
      },
      {
        key: 'hidden-3',
        value: '350'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.82',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/my-simple-nn3.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: 'f2fc7a5c-7b9d-496a-99d5-3466ba3f458c',
    projectId: 'f71b4b8f-90d2-4d96-a059-8776e3e2e52a',
    experimentId: '3aaca3a4-b450-482f-af66-2d68d24143a2',
    name: 'expt run 4',
    datasets: [
      {
        key: 'input_data',
        path: 'data/imgnet.gz',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'hidden-1',
        value: '1000'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.41',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/my-simple-nn4.gz',
        artifactType: 'MODEL'
      }
    ]
  },
  {
    id: '8106934b-689d-4bbf-9215-35d8465f106e',
    projectId: 'f71b4b8f-90d2-4d96-a059-8776e3e2e52a',
    experimentId: '3aaca3a4-b450-482f-af66-2d68d24143a2',
    name: 'expt run 5',
    datasets: [
      {
        key: 'input_data',
        path: 'data/imgnet.gz',
        artifactType: 'DATA'
      }
    ],
    hyperparameters: [
      {
        key: 'hidden-1',
        value: '1000'
      },
      {
        key: 'hidden-2',
        value: '100'
      },
      {
        key: 'hidden-3',
        value: '1000'
      }
    ],
    metrics: [
      {
        key: 'accuracy',
        value: '0.81',
        valueType: 'NUMBER'
      }
    ],
    artifacts: [
      {
        key: 'model',
        path: 'output/my-simple-nn5.gz',
        artifactType: 'MODEL'
      }
    ]
  }
];
