"""
Evaluate ONNX model on dataset/test and produce metrics + predictions CSV.

Usage:
  source venv/bin/activate
  python evaluate.py --data_dir ./dataset --model ./model/model.onnx --out ./model/predictions.csv
"""
import argparse
from pathlib import Path
from PIL import Image
import numpy as np
import onnxruntime as ort
from sklearn.metrics import roc_auc_score, precision_recall_curve, f1_score, confusion_matrix
import csv
from torchvision import datasets


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument('--data_dir', default='./dataset')
    p.add_argument('--model', default='./model/model.onnx')
    p.add_argument('--out', default='./model/predictions.csv')
    p.add_argument('--img_size', type=int, default=224)
    return p.parse_args()


def preprocess(img_path, img_size):
    img = Image.open(img_path).convert('RGB')
    img = img.resize((img_size, img_size))
    arr = np.array(img).astype(np.float32) / 255.0
    mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
    std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
    arr = (arr - mean) / std
    arr = np.transpose(arr, (2,0,1))[None, :]
    # Ensure float32 and C-contiguous for ONNXRuntime
    return np.ascontiguousarray(arr, dtype=np.float32)


def main():
    args = parse_args()
    model_path = Path(args.model)
    data_dir = Path(args.data_dir)
    out_csv = Path(args.out)

    if not model_path.exists():
        print('Model not found:', model_path)
        return

    sess = ort.InferenceSession(str(model_path), providers=['CPUExecutionProvider'])
    input_name = sess.get_inputs()[0].name

    # Read class mapping from dataset to avoid hard-coded label ordering
    class_to_idx = {}
    test_folder = data_dir / 'test'
    if test_folder.exists():
        try:
            ds = datasets.ImageFolder(str(test_folder))
            class_to_idx = ds.class_to_idx
            # class_to_idx example: {'clothing': 0, 'not_clothing': 1}
            print('Detected class_to_idx =', class_to_idx)
        except Exception:
            class_to_idx = {}

    samples = []
    for cls in ('clothing', 'not_clothing'):
        folder = data_dir / 'test' / cls
        if not folder.exists():
            continue
        for p in sorted(folder.iterdir()):
            if p.is_file():
                if class_to_idx:
                    lbl = class_to_idx.get(cls, 0)
                else:
                    # fallback: keep previous convention (clothing=1)
                    lbl = 1 if cls == 'clothing' else 0
                samples.append((p, lbl))

    if not samples:
        print('No test samples found under', data_dir / 'test')
        return

    probs = []
    labels = []
    rows = []
    for p, lbl in samples:
        inp = preprocess(p, args.img_size)
        out = sess.run(None, {input_name: inp})
        # Ensure output is treated as float (might be numpy array of float32)
        logit = float(np.array(out[0]).ravel()[0])
        prob = 1.0 / (1.0 + np.exp(-logit))
        probs.append(float(prob))
        labels.append(int(lbl))
        rows.append((str(p), int(lbl), float(prob)))

    probs = np.array(probs)
    labels = np.array(labels)

    try:
        auc = roc_auc_score(labels, probs)
    except Exception:
        auc = float('nan')

    precision, recall, thresholds = precision_recall_curve(labels, probs)
    # compute f1 for thresholds (align shapes)
    f1s = []
    thr_for_f1 = []
    for t in np.linspace(0.0,1.0,101):
        preds = (probs >= t).astype(int)
        f1 = f1_score(labels, preds, zero_division=0)
        f1s.append(f1)
        thr_for_f1.append(t)
    best_idx = int(np.argmax(f1s))
    best_threshold = thr_for_f1[best_idx]
    best_f1 = f1s[best_idx]

    preds_at_05 = (probs >= 0.5).astype(int)
    f1_at_05 = f1_score(labels, preds_at_05, zero_division=0)
    cm = confusion_matrix(labels, preds_at_05)

    # write csv
    out_csv.parent.mkdir(parents=True, exist_ok=True)
    with open(out_csv, 'w', newline='') as f:
        w = csv.writer(f)
        w.writerow(['image','label','prob'])
        for r in rows:
            w.writerow(r)

    print('N samples:', len(labels))
    print('AUC:', auc)
    print('F1 @ 0.5:', f1_at_05)
    print('Best F1:', best_f1, 'at threshold', best_threshold)
    print('Confusion matrix @0.5:\n', cm)
    print('Predictions CSV saved to', out_csv)


if __name__ == '__main__':
    main()
