export interface IOperation {
  name: string;
  status: 'deployed' | 'retired' | 'deploying';
}
