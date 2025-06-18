import base64
import numpy as np
import traceback
import tensorflow as tf
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

MODEL_PATH  = "models/animal_gesture.h5"
LABELS_PATH = "models/labels.txt"
SEQ_LEN     = 30
FEAT_DIM    = 225
TOTAL_DIM   = SEQ_LEN * FEAT_DIM  # 6750 floats

model  = tf.keras.models.load_model(MODEL_PATH)
labels = open(LABELS_PATH, encoding="utf8").read().splitlines()

class InB64(BaseModel):
    b64: str

class OutPred(BaseModel):
    animal: str
    conf: float
    probs: list[float]

app = FastAPI()

@app.post("/predict", response_model=OutPred)
def predict(inp: InB64):
    try:
        data = base64.b64decode(inp.b64)
        arr  = np.frombuffer(data, np.float32)
        if arr.size != TOTAL_DIM:
            raise ValueError(f"Expected {TOTAL_DIM} floats, got {arr.size}")
        x = arr.reshape(1, SEQ_LEN, FEAT_DIM)
        p = model.predict(x, verbose=0)[0]
        idx = int(np.argmax(p))
        return OutPred(
            animal=labels[idx],
            conf=float(p[idx]),
            probs=[float(v) for v in p]
        )
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(500, str(e))