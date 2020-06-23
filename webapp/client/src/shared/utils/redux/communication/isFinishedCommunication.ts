import { ICommunication } from './types';

const isFinishedCommunication = (communication: ICommunication) => {
  return Boolean(communication.isSuccess || communication.error);
};

export default isFinishedCommunication;
