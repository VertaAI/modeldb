import { Reducer } from 'redux';

import {
  fetchModelRecordAction,
  fetchModelRecordActionTypes,
  IModelRecordState,
} from './types';
import ModelRecord from 'models/ModelRecord';

const mock: any = {
  id: '556c26a1-781b-420f-9343-77cad7c22f9e',
  projectId: 'b328df27-f286-4850-8d68-841d2e1a43ba',
  experimentId: 'b3a95aa5-42c8-4438-9713-bac96745f8fe',
  name: 'Run 11',
  codeVersion: '1.15.6',
  description:
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris',
  owner: 'github|14152628',
  dateCreated: '1970-01-18T22:56:37.426Z',
  dateUpdated: '1970-01-18T22:56:37.431Z',
  startTime: '1970-01-18T22:56:37.426Z',
  endTime: '1970-01-18T22:56:37.431Z',
  tags: ['enhancement', 'obsolete', 'debug', 'demo'],
  hyperparameters: [
    {
      key: 'hidden_size',
      value: 1024,
    },
    {
      key: 'dropout',
      value: 0.4,
    },
    {
      key: 'optimizer',
      value: 'adam',
    },
    {
      key: 'batch_size',
      value: 51257889,
    },
    {
      key: 'num_epochs',
      value: 4,
    },
    {
      key: 'validation_split',
      value: 0.1,
    },
  ],
  metrics: [
    {
      key: 'train_loss',
      value: 0.18684466613531112,
    },
    {
      key: 'train_acc',
      value: 0.9489,
    },
  ],
  artifacts: [
    {
      key: 'validation_plot',
      path: '../output/val_obs_11.png',
      type: 'IMAGE',
    },
    {
      key: 'model',
      path: '../output/tensorflow-basic_11.hdf5',
      type: 'MODEL',
    },
  ],
  datasets: [
    {
      key: 'train_data',
      path: '../data/mnist/train.npz',
      type: 'DATA',
    },
  ],
  observations: [
    {
      attribute: {
        key: 'val_train_loss',
        value: 0.9519083389706082,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_train_loss',
        value: 0.34387786463896436,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_train_loss',
        value: 0.2631756428480148,
      },
      timestamp: '1970-01-18T22:56:37.430Z',
    },
    {
      attribute: {
        key: 'val_train_loss',
        value: 0.2168384866449568,
      },
      timestamp: '1970-01-18T22:56:37.431Z',
    },
    {
      attribute: {
        key: 'val_train_acc',
        value: 0.7507777774598864,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_train_acc',
        value: 0.8991111115879483,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_train_acc',
        value: 0.9242222231229146,
      },
      timestamp: '1970-01-18T22:56:37.430Z',
    },
    {
      attribute: {
        key: 'val_train_acc',
        value: 0.9377777781486512,
      },
      timestamp: '1970-01-18T22:56:37.431Z',
    },
    {
      attribute: {
        key: 'val_loss',
        value: 0.36536899423599245,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_loss',
        value: 0.275101797580719,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_loss',
        value: 0.2304303308725357,
      },
      timestamp: '1970-01-18T22:56:37.430Z',
    },
    {
      attribute: {
        key: 'val_loss',
        value: 0.2052534782886505,
      },
      timestamp: '1970-01-18T22:56:37.431Z',
    },
    {
      attribute: {
        key: 'val_acc',
        value: 0.8839999880790711,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_acc',
        value: 0.9180000004768372,
      },
      timestamp: '1970-01-18T22:56:37.429Z',
    },
    {
      attribute: {
        key: 'val_acc',
        value: 0.9360000042915344,
      },
      timestamp: '1970-01-18T22:56:37.430Z',
    },
    {
      attribute: {
        key: 'val_acc',
        value: 0.9420000019073487,
      },
      timestamp: '1970-01-18T22:56:37.431Z',
    },
  ],
};

const modelInitialState: IModelRecordState = {
  data: mock,
  loading: false,
};

export const modelRecordReducer: Reducer<IModelRecordState> = (
  state = modelInitialState,
  action: fetchModelRecordAction
) => {
  switch (action.type) {
    case fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST: {
      return { ...state, loading: true };
    }
    case fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCCESS: {
      return { ...state, loading: false, data: action.payload || mock };
    }
    case fetchModelRecordActionTypes.FETCH_MODEL_RECORD_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};
