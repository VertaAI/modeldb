import { IDeployConfig, IDeployStatusInfo } from 'models/Deploy';

import { IDeployRequest } from './DeployService';

export interface IDeployService {
  deploy(req: IDeployRequest): Promise<void>;
  delete(modelId: string): Promise<void>;
  loadStatus(modelId: string): Promise<IDeployStatusInfo>;
}
