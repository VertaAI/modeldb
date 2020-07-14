import { MutationUpdaterFn } from 'apollo-boost';

const onCompletedUpdate = <T>(f: (data: T | null | undefined) => void) => {
  const handleUpdate: MutationUpdaterFn<T> = ({}, { errors, data }) => {
    if (!errors || errors.length === 0) {
      return f(data);
    }
  };
  return handleUpdate;
};

export default onCompletedUpdate;
