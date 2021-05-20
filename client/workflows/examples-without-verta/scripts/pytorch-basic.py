"""Fully-Connected Network (PyTorch)"""

import os

import numpy as np

import torch
import torch.nn as nn
import torch.nn.functional as func
import torch.optim as optim
import torch.utils.data as data_utils


# load data from CSV file into a NumPy mapping
data = np.load(os.path.join("..", "data", "mnist", "mnist.npz"))

# gather indices to split training data into training and validation sets
data_train = (data['x_train'], data['y_train'])
shuffled_idxs = np.random.permutation(data['x_train'].shape[0])
idxs_train = shuffled_idxs[len(shuffled_idxs)//10:]  # last 90%
idxs_val = shuffled_idxs[:len(shuffled_idxs)//10]  # first 10%

# split and load data into PyTorch Tensors
x_train, y_train = (torch.tensor(data['x_train'][idxs_train], dtype=torch.float),
                    torch.tensor(data['y_train'][idxs_train], dtype=torch.long))
x_val, y_val = (torch.tensor(data['x_train'][idxs_val], dtype=torch.float),
                torch.tensor(data['y_train'][idxs_val], dtype=torch.long))
x_test, y_test = (torch.tensor(data['x_test'], dtype=torch.float),
                  torch.tensor(data['y_test'], dtype=torch.long))

# squeeze pixel values into from ints [0, 255] to reals [0, 1]
x_train, x_val, x_test = x_train/255, x_val/255, x_test/255


# create Dataset object to support batch training
class TrainingDataset(data_utils.Dataset):
    def __init__(self, features, labels):
        self.features = features
        self.labels = labels

    def __len__(self):
        return len(self.labels)

    def __getitem__(self, idx):
        return (self.features[idx], self.labels[idx])


# build model
class Net(nn.Module):
    def __init__(self, num_features=28*28, hidden_size=512):
        super().__init__()
        self.fc      = nn.Linear(num_features, hidden_size)
        self.dropout = nn.Dropout(0.2)
        self.output  = nn.Linear(hidden_size, 10)

    def forward(self, x):
        x = x.view(x.shape[0], -1)  # flatten non-batch dimensions
        x = func.relu(self.fc(x))
        x = self.dropout(x)
        x = func.softmax(self.output(x), dim=-1)
        return x


# specify training procedure
model = Net()

criterion = torch.nn.CrossEntropyLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.005)

num_epochs = 5
batch_size = 32


# enable batching of training data
dataset = TrainingDataset(x_train, y_train)
dataloader = data_utils.DataLoader(dataset, batch_size=batch_size, shuffle=True)

# train model
for i_epoch in range(num_epochs):
    for i_batch, (x_batch, y_batch) in enumerate(dataloader):
        model.zero_grad()  # reset model gradients

        output = model(x_batch)  # conduct forward pass

        loss = criterion(output, y_batch)  # compare model output w/ ground truth

        print(f"\repoch {i_epoch+1}/{num_epochs} | "
              f"iteration {i_batch+1}/{len(dataloader)} | "
              f"epoch loss avg: {loss.item()}", end='')

        loss.backward()  # backpropogate loss to calculate gradients
        optimizer.step()  # update model weights
    with torch.no_grad():  # no need to calculate gradients when assessing accuracy
        print()
        pred_train = model(x_train).numpy().argmax(axis=1)
        print(f"Training accuracy: {(pred_train == y_train.numpy()).mean()}")
        pred_val = model(x_val).numpy().argmax(axis=1)
        print(f"Validation accuracy: {(pred_val == y_val.numpy()).mean()}")


with torch.no_grad():  # no need to calculate gradients when assessing accuracy
    pred_train = model(x_train).numpy().argmax(axis=1)
    print(f"Training accuracy: {(pred_train == y_train.numpy()).mean()}")
    pred_test = model(x_test).numpy().argmax(axis=1)
    print(f"Testing accuracy: {(pred_test == y_test.numpy()).mean()}")


# save model weights to disk
torch.save(model.state_dict(), os.path.join("..", "output", "pytorch-basic.hdf5"))
