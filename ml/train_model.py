from keras_tuner import Hyperband
from tensorflow.keras.callbacks import EarlyStopping
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dropout, Dense
import numpy as np

# Загрузка и подготовка данных
X_train = np.load("X_train.npy")
X_test  = np.load("X_test.npy")
y_train = to_categorical(np.load("y_train.npy"))
y_test  = to_categorical(np.load("y_test.npy"))

num_classes = y_train.shape[1]
input_shape = (X_train.shape[1], X_train.shape[2])

# Колбэк для ранней остановки
early_stop = EarlyStopping(monitor="val_loss", patience=5, restore_best_weights=True)

# Функция построения модели
def build_model(hp):
    model = Sequential([
        LSTM(hp.Int("lstm1", 32, 128, step=32),
             return_sequences=True, input_shape=input_shape),
        Dropout(hp.Float("drop1", 0.1, 0.5, step=0.1)),
        LSTM(hp.Int("lstm2", 32, 128, step=32)),
        Dropout(hp.Float("drop2", 0.1, 0.5, step=0.1)),
        Dense(hp.Int("dense", 16, 64, step=16), activation="relu"),
        Dense(num_classes, activation="softmax")
    ])
    model.compile(
        optimizer=Adam(hp.Float("lr", 1e-4, 1e-2, sampling="log")),
        loss="categorical_crossentropy",
        metrics=["accuracy"]
    )
    return model

# Инициализация Hyperband
tuner = Hyperband(
    build_model,
    objective="val_accuracy",
    max_epochs=100,
    factor=3,
    directory="tuner_es",
    project_name="gesture_es"
)

# Создаём гиперпараметр batch_size отдельно
hp = tuner.oracle.hyperparameters
batch_size = hp.Choice("batch_size", [16, 32, 64], default=32)

# Запускаем поиск
tuner.search(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=100,               # максимум — 100, но EarlyStopping остановит раньше
    callbacks=[early_stop],
    batch_size=batch_size     # передаём уже готовую переменную
)

# Сохраняем лучшую модель
best_model = tuner.get_best_models(num_models=1)[0]
best_model.save("animal_gesture.h5")
print("Модель сохранена")
tuner.results_summary()
