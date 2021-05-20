"""LSTM Recurrent Network (PyTorch)"""

import os
import json

import numpy as np

import torch
import torch.nn as nn
import torch.nn.functional as func
import torch.optim as optim
import torch.utils.data as data_utils


# load data from CSV file into a NumPy mapping
data = np.load(os.path.join("..", "data", "imdb", "imdb.npz"))

# gather indices to split training data into training and validation sets
data_train = (data['x_train'], data['y_train'])
shuffled_idxs = np.random.permutation(data['x_train'].shape[0])
idxs_train = shuffled_idxs[len(shuffled_idxs)//10:]  # last 90%
idxs_val = shuffled_idxs[:len(shuffled_idxs)//10]  # first 10%

# split and load data into NumPy arrays
x_train, y_train = data['x_train'][idxs_train], data['y_train'][idxs_train]
x_val, y_val = data['x_train'][idxs_val], data['y_train'][idxs_val]
x_test, y_test = data['x_test'], data['y_test']


# create Dataset object to support batch training
class TrainingDataset(data_utils.Dataset):
    def __init__(self, features, labels):
        self.features = features
        self.labels = labels

    def __len__(self):
        return len(self.labels)

    def __getitem__(self, idx):
        return (self.features[idx], self.labels[idx])


# load word-index mapping
with open(os.path.join("..", "data", "imdb", "imdb_word_index.json")) as f:
    word_index = json.load(f)
# add special tokens
word_index = {word: index+3 for word, index in word_index.items()}
word_index["<PAD>"] = 0
word_index["<START>"] = 1
word_index["<UNK>"] = 2  # unknown
word_index["<UNUSED>"] = 3


# truncate input sequences to max length 300
x_train = [seq[:300] if len(seq) > 300 else seq
           for seq
           in x_train]
x_val   = [seq[:300] if len(seq) > 300 else seq
           for seq
           in x_val]
x_test  = [seq[:300] if len(seq) > 300 else seq
           for seq
           in x_test]

# convert input sequences and labels into PyTorch tensors
x_train, x_val, x_test = ([torch.tensor(seq, dtype=torch.long) for seq in x_train],
                          [torch.tensor(seq, dtype=torch.long) for seq in x_val],
                          [torch.tensor(seq, dtype=torch.long) for seq in x_test])
y_train, y_val, y_test = (torch.tensor(y_train, dtype=torch.float),
                          torch.tensor(y_val, dtype=torch.float),
                          torch.tensor(y_test, dtype=torch.float))

# pad input sequences
x_train = torch.nn.utils.rnn.pad_sequence(x_train, batch_first=True, padding_value=word_index["<PAD>"])
x_val   = torch.nn.utils.rnn.pad_sequence(x_val,   batch_first=True, padding_value=word_index["<PAD>"])
x_test  = torch.nn.utils.rnn.pad_sequence(x_test,  batch_first=True, padding_value=word_index["<PAD>"])


# build model
class Net(nn.Module):
    def __init__(self):
        super().__init__()
        self.embedding = nn.Embedding(max(word_index.values())+1, 16)
        self.lstm1     = nn.LSTM(16, 32, batch_first=True)
        self.dropout   = nn.Dropout(0.2)
        self.lstm2     = nn.LSTM(32, 32, batch_first=True)
        self.output    = nn.Linear(32, 1)

    def init_hidden(self):
        self.hidden1 = torch.randn(1, 1, 32)
        self.hidden2 = torch.randn(1, 1, 32)

    def forward(self, x):
        x               = self.embedding(x)
        x, self.hidden1 = self.lstm1(x)
        x               = self.dropout(x)
        x, self.hidden2 = self.lstm2(x)
        x               = torch.sigmoid(self.output(x[:,-1,:]))
        return x.squeeze(-1)


# specify training procedure
model = Net()

criterion = torch.nn.BCELoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01)

num_epochs = 10
batch_size = 512


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
              f"loss: {loss.item()}", end='')

        loss.backward()  # backpropogate loss to calculate gradients
        optimizer.step()  # update model weights
    with torch.no_grad():  # no need to calculate gradients when assessing accuracy
        print()
        pred_train = model(x_train).numpy().round()
        print(f"Training accuracy: {(pred_train == y_train.numpy()).mean()}")
        pred_val = model(x_val).numpy().round()
        print(f"Validation accuracy: {(pred_val == y_val.numpy()).mean()}")


with torch.no_grad():  # no need to calculate gradients when assessing accuracy
    pred_train = model(x_train).numpy().round()
    print(f"Training accuracy: {(pred_train == y_train.numpy()).mean()}")
    pred_test = model(x_test).numpy().round()
    print(f"Testing accuracy: {(pred_test == y_test.numpy()).mean()}")


# save model weights to disk
torch.save(model.state_dict(), os.path.join("..", "output", "pytorch-basic.hdf5"))
