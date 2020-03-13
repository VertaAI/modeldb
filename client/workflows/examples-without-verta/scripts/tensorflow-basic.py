"""Fully-Connected Network (TensorFlow)"""

import os

import numpy as np

import tensorflow as tf
from tensorflow import keras


# load data from CSV file into a NumPy mapping
data = np.load(os.path.join("..", "data", "mnist", "mnist.npz"))

# gather indices to split training data into training and validation sets
data_train = (data['x_train'], data['y_train'])
shuffled_idxs = np.random.permutation(data['x_train'].shape[0])
idxs_train = shuffled_idxs[len(shuffled_idxs)//10:]  # last 90%
idxs_val = shuffled_idxs[:len(shuffled_idxs)//10]  # first 10%

# split and load data into NumPy arrays
x_train, y_train = data['x_train'][idxs_train], data['y_train'][idxs_train]
x_val, y_val = data['x_train'][idxs_val], data['y_train'][idxs_val]
x_test, y_test = data['x_test'], data['y_test']

# squeeze pixel values into from ints [0, 255] to reals [0, 1]
x_train, x_val, x_test = x_train/255, x_val/255, x_test/255


# build model and specify training procedure
model = keras.models.Sequential()
model.add(keras.layers.Flatten())
model.add(keras.layers.Dense(512, activation=tf.nn.relu))
model.add(keras.layers.Dropout(0.2))
model.add(keras.layers.Dense(10, activation=tf.nn.softmax))

model.compile(optimizer='adam',
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])


# train model
history = model.fit(x_train, y_train, epochs=5, validation_data=(x_val, y_val))


print(f"Training accuracy: {model.evaluate(x_train, y_train)[1]}")
print(f"Testing accuracy: {model.evaluate(x_test, y_test)[1]}")


# save model to disk
keras.models.save_model(model, os.path.join("..", "output", "tensorflow-basic.hdf5"))
