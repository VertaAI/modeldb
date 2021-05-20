"""LSTM Recurrent Network (TensorFlow)"""

import os
import json

import numpy as np

import tensorflow as tf
from tensorflow import keras


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

# load word-index mapping
with open(os.path.join("..", "data", "imdb", "imdb_word_index.json")) as f:
    word_index = json.load(f)
# add special tokens
word_index = {word: index+3 for word, index in word_index.items()}
word_index["<PAD>"] = 0
word_index["<START>"] = 1
word_index["<UNK>"] = 2  # unknown
word_index["<UNUSED>"] = 3


# pad input sequences
x_train = keras.preprocessing.sequence.pad_sequences(x_train,
                                                     value=word_index["<PAD>"],
                                                     padding='post',
                                                     maxlen=300)
x_val = keras.preprocessing.sequence.pad_sequences(x_val,
                                                   value=word_index["<PAD>"],
                                                   padding='post',
                                                   maxlen=300)
x_test = keras.preprocessing.sequence.pad_sequences(x_test,
                                                    value=word_index["<PAD>"],
                                                    padding='post',
                                                    maxlen=300)


# build model and specify training procedure
model = keras.Sequential()
model.add(keras.layers.Embedding(max(word_index.values())+1, 16))
model.add(keras.layers.LSTM(32, return_sequences=True))
model.add(keras.layers.Dropout(0.2))
model.add(keras.layers.LSTM(32))
model.add(keras.layers.Dense(1, activation=tf.nn.sigmoid))

model.compile(optimizer=tf.train.AdamOptimizer(learning_rate=0.01),
              loss='binary_crossentropy',
              metrics=['accuracy'])


# train model
history = model.fit(x_train, y_train, epochs=10, batch_size=512, validation_data=(x_val, y_val))


print(f"Training accuracy: {model.evaluate(x_train, y_train)[1]}")
print(f"Testing accuracy: {model.evaluate(x_test, y_test)[1]}")


# save model to disk
keras.models.save_model(model, os.path.join("..", "output", "tensorflow-lstm.hdf5"))
