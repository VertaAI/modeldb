import { IDeployConfig, IDeployResult } from 'models/Deploy';

export interface IDeployService {
  deploy(modelId: string, config: IDeployConfig): Promise<IDeployResult>;
}
