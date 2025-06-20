import cv2
import mediapipe as mp
import numpy as np
import os
import time

# Инициализация MediaPipe
mp_hands = mp.solutions.hands
mp_pose = mp.solutions.pose
hands = mp_hands.Hands(min_detection_confidence=0.7, min_tracking_confidence=0.7)
pose = mp_pose.Pose(min_detection_confidence=0.7, min_tracking_confidence=0.7)

# Параметры
gesture_name = "crocodile"  # Название жеста
sequence_length = 30  # Длина последовательности (30 кадров)
save_path = "D:\PythonProjects\kursa4\LastVersion\data\\crocodile"  # Папка для сохранения данных

if not os.path.exists(save_path):
    os.makedirs(save_path)

# Открытие камеры
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1920)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 1080)
sequence = []
collecting = False
countdown = 5  # Таймер перед началом записи
start_time = time.time()  # Инициализация времени

print(f"Готовимся к сбору данных для жеста: {gesture_name}")

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break

    # Перевод изображения в RGB
    image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    hand_results = hands.process(image)
    pose_results = pose.process(image)

    # Создание нового изображения для отображения точек
    points_frame = np.zeros_like(frame)  # Создаем пустое черное изображение того же размера

    # Отображение ключевых точек на кадре
    if hand_results.multi_hand_landmarks:
        for hand_landmarks in hand_results.multi_hand_landmarks:
            mp.solutions.drawing_utils.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)
            # Отображение точек на втором окне
            for landmark in hand_landmarks.landmark:
                x, y = int(landmark.x * frame.shape[1]), int(landmark.y * frame.shape[0])
                cv2.circle(points_frame, (x, y), 3, (255, 255, 255), -1)

    if pose_results.pose_landmarks:
        mp.solutions.drawing_utils.draw_landmarks(frame, pose_results.pose_landmarks, mp_pose.POSE_CONNECTIONS)
        # Отображение точек на втором окне
        for landmark in pose_results.pose_landmarks.landmark:
            x, y = int(landmark.x * frame.shape[1]), int(landmark.y * frame.shape[0])
            cv2.circle(points_frame, (x, y), 3, (255, 255, 255), -1)

    # Отображение таймера обратного отсчета
    if not collecting:
        elapsed_time = time.time() - start_time
        countdown = max(0, 2 - int(elapsed_time))  # Таймер с уменьшением
        cv2.putText(frame, f"Starting in: {countdown}s", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2, cv2.LINE_AA)

        if countdown == 0:
            collecting = True
            start_time = time.time()  # Сброс времени
            print("Начало записи!")
        cv2.imshow("Data Collection", frame)
        cv2.imshow("Points Display", points_frame)  # Отображаем второй экран с точками
        cv2.waitKey(1)  # Пустая задержка для захвата кадров
        continue

    # Собираем координаты точек
    frame_data = []
    
    try:
        # Для рук
        if hand_results.multi_hand_landmarks:
            for hand_landmarks in hand_results.multi_hand_landmarks:
                frame_data.extend([[lmk.x, lmk.y, lmk.z] for lmk in hand_landmarks.landmark])

        # Для тела (включая ноги)
        if pose_results.pose_landmarks:
            frame_data.extend([[lmk.x, lmk.y, lmk.z] for lmk in pose_results.pose_landmarks.landmark])

        # Проверка, что данные валидны
        if frame_data:
            sequence.append(np.array(frame_data).flatten())
    except Exception as e:
        print(f"Ошибка при обработке данных: {e}")

    # Отображение сообщения "Запись" во время записи
    cv2.putText(frame, "Recording", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2, cv2.LINE_AA)

    # Если последовательность достигла нужной длины
    if len(sequence) == sequence_length:
        file_name = f"{gesture_name}_{len(os.listdir(save_path))}.npy"
        try:
            np.save(os.path.join(save_path, file_name), np.array(sequence))
            print(f"Сохранена последовательность: {file_name}")
        except Exception as e:
            print(f"Ошибка при сохранении данных: {e}")
        sequence = []
        collecting = False
        start_time = time.time()  # Перезапуск таймера

    # Отображение видео
    cv2.imshow("Data Collection", frame)
    cv2.imshow("Points Display", points_frame)  # Второе окно с точками
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
