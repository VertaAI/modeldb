import torch
import torch.nn as nn
import torch.nn.functional as func

from utils import preprocess  # ../utils/preprocess.py


class FullyConnected(nn.Module):
    def __init__(self, num_features, hidden_size, dropout):
        super(FullyConnected, self).__init__()
        self.flatten = preprocess.Flatten()
        self.fc      = nn.Linear(num_features, hidden_size)
        self.dropout = nn.Dropout(dropout)
        self.output  = nn.Linear(hidden_size, 10)

    def forward(self, x):
        x = self.flatten(x)
        x = func.relu(self.fc(x))
        x = self.dropout(x)
        x = func.softmax(self.output(x), dim=-1)
        return x
