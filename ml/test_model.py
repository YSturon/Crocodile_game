
"""
Живое демо LSTM-модели жестов.
Кадр (225 float) составляется: Left-Hand → Right-Hand → Pose.
Недостающие координаты заполняются -1.0 только в конце.
"""

import os
import time
import cv2
import numpy as np
import mediapipe as mp
from tensorflow.keras.models import load_model
from colorsys import hsv_to_rgb

# Конфигурация
MODEL_PATH = os.getenv("MODEL_PATH", "best_gesture_es.h5")
SEQ_LEN    = 30
FRAME_DIM  = 225     # 75 landmarks × 3
CLASSES = [
    "moose","bull","elephant","rabbit","giraffe",
    "crocodile","gopher","chicken","gorilla","rhinoceros",
]
COLORS = {cls: tuple(int(c*255) for c in hsv_to_rgb(i/len(CLASSES), 0.85, 1.0))
          for i, cls in enumerate(CLASSES)}

# Модель
model = load_model(MODEL_PATH)
if model.output_shape[-1] != len(CLASSES):
    raise RuntimeError(
        f"Model outputs {model.output_shape[-1]} classes, "
        f"but CLASSES has {len(CLASSES)}.")
print(f"✅ Model '{MODEL_PATH}' loaded – {len(CLASSES)} classes")

# MediaPipe
mp_hands = mp.solutions.hands
mp_pose  = mp.solutions.pose
mp_draw  = mp.solutions.drawing_utils

hands = mp_hands.Hands(
    static_image_mode=False, max_num_hands=2,
    min_detection_confidence=0.5, min_tracking_confidence=0.5)
pose = mp_pose.Pose(
    static_image_mode=False, model_complexity=1,
    enable_segmentation=False,
    min_detection_confidence=0.5, min_tracking_confidence=0.5)

# Камера 
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
if not cap.isOpened():
    raise RuntimeError("Cannot open webcam")

sequence: list[np.ndarray] = []
start_time = time.time()

BAR_ORIGIN = (50, 150)
BAR_H = 25
BAR_W = 250

def z_range(arr: np.ndarray) -> tuple[float, float]:
    z = arr[2::3]
    return float(z.min()), float(z.max())

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    hand_res = hands.process(rgb)
    pose_res = pose.process(rgb)

    # рисуем landmarks
    draw = frame.copy()
    if hand_res.multi_hand_landmarks:
        for hlm in hand_res.multi_hand_landmarks:
            mp_draw.draw_landmarks(draw, hlm, mp_hands.HAND_CONNECTIONS)
    if pose_res.pose_landmarks:
        mp_draw.draw_landmarks(draw, pose_res.pose_landmarks, mp_pose.POSE_CONNECTIONS)

    # формируем кадр
    frame_vec: list[float] = []

    # руки: сортировка Left ->  Right
    if hand_res.multi_hand_landmarks:
        handed = list(zip(hand_res.multi_handedness, hand_res.multi_hand_landmarks))
        handed.sort(key=lambda x: x[0].classification[0].label)   # "Left" < "Right" !!!!!!!!!!!!!
        for _, hlm in handed:
            frame_vec.extend((l.x, l.y, l.z) for l in hlm.landmark)

    # поза
    if pose_res.pose_landmarks:
        frame_vec.extend((l.x, l.y, l.z) for l in pose_res.pose_landmarks.landmark)

    flat = np.array(frame_vec, dtype=np.float32).flatten()

    # добавляем -1.0 в КОНЕЦ до 225 float
    if flat.size < FRAME_DIM:
        flat = np.concatenate([flat, np.full(FRAME_DIM - flat.size, -1.0, np.float32)])
    elif flat.size > FRAME_DIM:
        flat = flat[:FRAME_DIM]

    # диапазон Z для контроля
    zmin, zmax = z_range(flat)
    print(f"Z-range  {zmin:+.3f} … {zmax:+.3f}")

    sequence.append(flat)
    if len(sequence) > SEQ_LEN:
        sequence.pop(0)

    # инференс
    if len(sequence) == SEQ_LEN:
        seq_np = np.expand_dims(np.stack(sequence), axis=0)   # (1,30,225)
        probs  = model.predict(seq_np, verbose=0)[0]
        idx    = int(np.argmax(probs))
        best   = CLASSES[idx]
        pbest  = float(probs[idx])

        cv2.putText(draw, f"{best}  {pbest:.2f}",
                    (50, 100), cv2.FONT_HERSHEY_SIMPLEX, 1.5, COLORS[best], 3)

        # бары вероятностей
        y = BAR_ORIGIN[1]
        for j in np.argsort(probs)[::-1]:
            cls, p = CLASSES[j], float(probs[j])
            cv2.rectangle(draw, (BAR_ORIGIN[0], y),
                          (BAR_ORIGIN[0] + int(p*BAR_W), y + BAR_H),
                          COLORS[cls], -1)
            cv2.putText(draw, f"{cls}: {p:.2f}",
                        (BAR_ORIGIN[0] + BAR_W + 20, y + BAR_H - 5),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255,255,255), 2)
            y += BAR_H + 6

    # FPS
    fps = len(sequence) / max(time.time() - start_time, 1e-3)
    cv2.putText(draw, f"FPS: {fps:.1f}", (50, 50),
                cv2.FONT_HERSHEY_SIMPLEX, 1, (200,200,200), 2)

    cv2.imshow("Gesture Live", draw)
    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

# cleanup
hands.close()
pose.close()
cap.release()
cv2.destroyAllWindows()
q