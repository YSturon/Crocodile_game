import cv2
import numpy as np
import os

# Путь к папке с .npy файлами
data_dir = "D:\PythonProjects\kursa4\LastVersion\data\\giraffe"  

# Настройки окна
frame_size = 500
margin = 50

# Хранилище всех точек
all_points = []

for i in range(1, 28):  # номера
    filename = f"giraffe_{i}.npy"
    filepath = os.path.join(data_dir, filename)

    if not os.path.exists(filepath):
        print(f"Файл {filepath} не найден, пропускаем.")
        continue

    try:
        data = np.load(filepath)
        for frame_data in data:
            if len(frame_data) % 3 != 0:
                continue  # Пропускаем некорректный кадр
            points = np.array(frame_data).reshape((-1, 3))
            all_points.extend(points)
        print(f"Загружено {len(data)} кадров из {filename}")
    except Exception as e:
        print(f"Ошибка загрузки {filename}: {e}")

# Проверка наличия точек
if not all_points:
    print("Нет данных для отображения.")
    exit()

# Визуализация
frame = np.ones((frame_size, frame_size, 3), dtype=np.uint8) * 255

for point in all_points:
    x, y, z = point
    x_screen = int(x * (frame_size - 2 * margin) + margin)
    y_screen = int(y * (frame_size - 2 * margin) + margin)

    if 0 <= x_screen < frame_size and 0 <= y_screen < frame_size:
        cv2.circle(frame, (x_screen, y_screen), radius=3, color=(0, 0, 255), thickness=-1)

# Отображение
print("Нажмите любую клавишу для выхода.")
cv2.imshow("All Points Combined", frame)
cv2.waitKey(0)
cv2.destroyAllWindows()
