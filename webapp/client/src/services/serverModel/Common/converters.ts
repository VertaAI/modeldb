import { IEntityWithLogging as IEntityWithLoggedDates } from 'core/shared/models/Common';

export const convertServerEntityWithLoggedDates = ({
  date_created,
  date_updated,
  time_updated,
  time_created,
}: any): IEntityWithLoggedDates => {
  const parseData = (date: string) => new Date(Number(date));
  const serverDateCreated =
    date_created || time_created || date_updated || time_updated;
  const serverDateUpdated =
    date_updated || time_updated || date_created || time_created;
  return {
    dateCreated: serverDateCreated ? parseData(serverDateCreated) : new Date(),
    dateUpdated: serverDateUpdated ? parseData(serverDateUpdated) : new Date(),
  };
};
