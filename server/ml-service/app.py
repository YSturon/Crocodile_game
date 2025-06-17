import numpy as np, traceback, tensorflow as tf
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, conlist

MODEL_PATH  = "models/animal_gesture.h5"
LABELS_PATH = "models/labels.txt"
SEQ_LEN     = 30
FEAT_DIM    = 225

model  = tf.keras.models.load_model(MODEL_PATH)
labels = open(LABELS_PATH, encoding="utf8").read().splitlines()

class InSeq(BaseModel):
    seq: conlist(conlist(float, min_length=FEAT_DIM, max_length=FEAT_DIM),
                 min_length=SEQ_LEN, max_length=SEQ_LEN)

class OutPred(BaseModel):
    animal: str
    conf  : float
    probs : list[float]   # длина 10

app = FastAPI()

@app.post("/predict", response_model=OutPred)
def predict(inp: InSeq):
    try:
        x = np.asarray(inp.seq, np.float32).reshape(1, SEQ_LEN, FEAT_DIM)
        p = model.predict(x, verbose=0)[0]            # ndarray длиной 10
        idx = int(np.argmax(p))
        return OutPred(animal=labels[idx],
                       conf=float(p[idx]),
                       probs=[float(v) for v in p])
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(500, str(e))
