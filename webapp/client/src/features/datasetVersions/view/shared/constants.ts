import { DatasetVersionPathLocationType } from 'models/DatasetVersion';

export const pathLocationLabels: Record<
  DatasetVersionPathLocationType,
  string
> = {
  s3FileSystem: 'S3_DATASET',
  localFileSystem: 'LOCAL_FILESYSTEM',
  hadoopFileSystem: 'HADOOP_FILESYSTEM',
  networkFileSystem: 'NETWORK_FILESYSTEM',
};
