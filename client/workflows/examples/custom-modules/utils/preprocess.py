import torch.nn as nn


class Flatten(nn.Module):
    def forward(self, x):
        """Flatten non-batch dimensions."""
        return x.view(x.shape[0], -1)
