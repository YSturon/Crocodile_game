import os
import numpy as np
from sklearn.model_selection import train_test_split

# Путь к папке с данными
data_path = "D:\\PythonProjects\\kursa4\\LastVersion\\data"
gestures = ["moose", "bull", "elephant", "rabbit", "giraffe", "crocodile", "gopher", "chicken", "gorilla", "rhinoceros"]  # Названия жестов

# Загрузка данных
sequences = []
labels = []

for gesture_idx, gesture_name in enumerate(gestures):
    gesture_folder = os.path.join(data_path, gesture_name)
    for file in os.listdir(gesture_folder):
        if file.endswith(".npy"):
            file_path = os.path.join(gesture_folder, file)
            try:
                sequence = np.load(file_path, allow_pickle=True)

                # Приведение object-массива к нормальной форме (если нужно)
                if sequence.ndim == 1 and isinstance(sequence[0], np.ndarray):
                    fixed = []
                    for frame in sequence:
                        # Приведение кадра к длине 225
                        if frame.shape[0] < 225:
                            pad = np.zeros(225 - frame.shape[0])
                            frame = np.concatenate([frame, pad])
                        elif frame.shape[0] > 225:
                            frame = frame[:225]
                        fixed.append(frame)
                    sequence = np.stack(fixed)

                # Дополнение до 225 столбцов (если нужно)
                if sequence.ndim == 2 and sequence.shape[1] < 225:
                    padding = np.zeros((sequence.shape[0], 225 - sequence.shape[1]))
                    sequence = np.hstack((sequence, padding))

                # Проверка формы
                if sequence.ndim != 2 or sequence.shape[1] != 225:
                    print(f"⚠️ Пропущен файл: {file_path}, неподходящая форма {sequence.shape}")
                    continue

                print(f"✅ Loaded: {file_path} — shape: {sequence.shape}")
                sequences.append(sequence)
                labels.append(gesture_idx)

            except Exception as e:
                print(f"❌ Ошибка при загрузке {file_path}: {e}")

# Преобразование в массивы
X = np.array(sequences)
y = np.array(labels)

# Разделение данных
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Сохранение
np.save("X_train.npy", X_train)
np.save("X_test.npy", X_test)
np.save("y_train.npy", y_train)
np.save("y_test.npy", y_test)

print("✅ Данные успешно подготовлены и сохранены.")
