import torch
from sklearn import datasets


def load_mnist():
    data = datasets.load_digits()
    X = data['data']
    y = data['target']
    return (torch.tensor(X, dtype=torch.float),
            torch.tensor(y, dtype=torch.long))
