"""
Simple transfer-learning training script for clothing vs not_clothing binary classifier.

Dataset structure expected (under validate_service folder):
  dataset/
    train/
      clothing/
      not_clothing/
    val/
      clothing/
      not_clothing/

Outputs:
  - best model checkpoint: best_model.pth
  - ONNX export: model.onnx (in ./model/)

Usage (local):
  python train.py --data_dir ./dataset --epochs 6 --batch_size 32

Notes:
  - This script uses PyTorch and exports ONNX (opset 11). For SageMaker/remote adjust accordingly.
"""
import argparse
import os
from pathlib import Path
import time
import numpy as np
import json
from tqdm import tqdm

import torch
from torch import nn
from torch.utils.data import DataLoader
from torchvision import datasets, transforms, models


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--data_dir", default="./dataset")
    p.add_argument("--epochs", type=int, default=6)
    p.add_argument("--batch_size", type=int, default=32)
    p.add_argument("--lr", type=float, default=2e-4)
    p.add_argument("--out_dir", default="./model")
    p.add_argument("--img_size", type=int, default=224)
    return p.parse_args()


def get_loaders(data_dir, batch_size, img_size, num_workers=4):
    train_tf = transforms.Compose([
        transforms.RandomResizedCrop(img_size),
        transforms.RandomHorizontalFlip(),
        transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2, hue=0.02),
        transforms.ToTensor(),
        transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
    ])
    val_tf = transforms.Compose([
        transforms.Resize(256),
        transforms.CenterCrop(img_size),
        transforms.ToTensor(),
        transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
    ])

    train_dir = os.path.join(data_dir, "train")
    val_dir = os.path.join(data_dir, "val")

    train_ds = datasets.ImageFolder(train_dir, transform=train_tf)
    val_ds = datasets.ImageFolder(val_dir, transform=val_tf)

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=num_workers)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers)

    return train_loader, val_loader, train_ds, val_ds


def train_loop(model, device, loader, criterion, optimizer):
    model.train()
    running_loss = 0.0
    for imgs, labels in loader:
        imgs = imgs.to(device)
        labels = labels.float().unsqueeze(1).to(device)
        optimizer.zero_grad()
        logits = model(imgs)
        loss = criterion(logits, labels)
        loss.backward()
        optimizer.step()
        running_loss += loss.item() * imgs.size(0)
    return running_loss


def validate_loop(model, device, loader):
    model.eval()
    all_probs = []
    all_labels = []
    with torch.no_grad():
        for imgs, labels in loader:
            imgs = imgs.to(device)
            logits = model(imgs)
            probs = torch.sigmoid(logits).cpu().numpy().ravel()
            all_probs.append(probs)
            all_labels.append(labels.numpy().ravel())
    if all_probs:
        probs = np.concatenate(all_probs)
        labels = np.concatenate(all_labels)
        return probs, labels
    return np.array([]), np.array([])


def main():
    args = parse_args()
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print('Device:', device)

    train_loader, val_loader, train_ds, val_ds = get_loaders(args.data_dir, args.batch_size, args.img_size)

    # Model: ResNet18 pretrained
    model = models.resnet18(pretrained=True)
    in_feats = model.fc.in_features
    model.fc = nn.Linear(in_feats, 1)
    model = model.to(device)

    criterion = nn.BCEWithLogitsLoss()
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)
    scheduler = torch.optim.lr_scheduler.StepLR(optimizer, step_size=3, gamma=0.3)

    best_auc = 0.0
    best_path = out_dir / 'best_model.pth'

    from sklearn.metrics import roc_auc_score

    for epoch in range(1, args.epochs + 1):
        t0 = time.time()
        train_loss = train_loop(model, device, train_loader, criterion, optimizer)
        scheduler.step()

        probs, labels = validate_loop(model, device, val_loader)
        auc = 0.0
        if len(probs) > 0 and len(np.unique(labels)) > 1:
            try:
                auc = roc_auc_score(labels, probs)
            except Exception:
                auc = 0.0

        epoch_loss = train_loss / max(1, len(train_loader.dataset))
        print(f"Epoch {epoch} loss={epoch_loss:.5f} val_auc={auc:.4f} time={time.time()-t0:.1f}s")

        if auc > best_auc:
            best_auc = auc
            torch.save({'model_state_dict': model.state_dict(), 'auc': best_auc}, str(best_path))
            print('Saved best model to', best_path)

    # Export to ONNX
    print('Exporting ONNX...')
    try:
        # load best
        if best_path.exists():
            ck = torch.load(str(best_path), map_location=device)
            model.load_state_dict(ck['model_state_dict'])
        # save dataset class mapping for inference
        try:
            meta = {'class_to_idx': train_ds.class_to_idx}
            with open(out_dir / 'metadata.json', 'w') as f:
                json.dump(meta, f)
            print('Saved metadata to', out_dir / 'metadata.json')
        except Exception:
            pass
        model.eval()
        dummy = torch.randn(1, 3, args.img_size, args.img_size, device=device)
        onnx_path = out_dir / 'model.onnx'
        torch.onnx.export(model, dummy, str(onnx_path), opset_version=11, input_names=['input'], output_names=['logit'])
        print('ONNX exported to', onnx_path)
    except Exception as e:
        print('ONNX export failed:', e)


if __name__ == '__main__':
    main()
