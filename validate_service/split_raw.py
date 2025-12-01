"""
split_raw.py

Move/copy images from `raw/{clothing,not_clothing}` into
`dataset/{train,val,test}/{clothing,not_clothing}` with a reproducible split.

Usage:
  python split_raw.py --src ./raw --dst ./dataset --train 0.8 --val 0.15 --test 0.05 --mode copy

mode: 'copy' (default) or 'move'
"""
import argparse
import random
from pathlib import Path
import shutil


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--src", default="./raw")
    p.add_argument("--dst", default="./dataset")
    p.add_argument("--train", type=float, default=0.8)
    p.add_argument("--val", type=float, default=0.15)
    p.add_argument("--test", type=float, default=0.05)
    p.add_argument("--mode", choices=("copy", "move"), default="copy")
    p.add_argument("--seed", type=int, default=42)
    return p.parse_args()


def ensure_dirs(base_dst: Path, classes):
    for split in ("train", "val", "test"):
        for c in classes:
            d = base_dst / split / c
            d.mkdir(parents=True, exist_ok=True)


def main():
    args = parse_args()
    src = Path(args.src)
    dst = Path(args.dst)
    classes = [d.name for d in src.iterdir() if d.is_dir()]
    if not classes:
        print("No class folders found in", src)
        return

    # normalize splits
    total = args.train + args.val + args.test
    if abs(total - 1.0) > 1e-6:
        args.train = args.train / total
        args.val = args.val / total
        args.test = args.test / total

    ensure_dirs(dst, classes)
    random.seed(args.seed)

    for c in classes:
        src_dir = src / c
        files = [p for p in src_dir.iterdir() if p.is_file()]
        random.shuffle(files)
        n = len(files)
        n_train = int(n * args.train)
        n_val = int(n * args.val)
        n_test = n - n_train - n_val

        idx = 0
        def transfer(lst, split_name):
            nonlocal idx
            for p in lst:
                dst_path = dst / split_name / c / p.name
                if args.mode == "copy":
                    shutil.copy2(p, dst_path)
                else:
                    shutil.move(str(p), str(dst_path))

        train_files = files[idx: idx + n_train]
        idx += n_train
        val_files = files[idx: idx + n_val]
        idx += n_val
        test_files = files[idx: idx + n_test]

        transfer(train_files, "train")
        transfer(val_files, "val")
        transfer(test_files, "test")

        print(f"Class {c}: total={n}, train={len(train_files)}, val={len(val_files)}, test={len(test_files)}")


if __name__ == '__main__':
    main()
