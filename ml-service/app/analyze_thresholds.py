import pandas as pd
import joblib

from pathlib import Path
from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix
)
from sklearn.model_selection import train_test_split


BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"
MODEL_PATH = BASE_DIR / "models" / "fraud_model.joblib"

data = pd.read_csv(DATA_PATH)

bundle = joblib.load(MODEL_PATH)

model = bundle["model"]
features = bundle["features"]

X = data[features]
y = data["is_fraud"]

_, X_test, _, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y
)

probabilities = model.predict_proba(X_test)[:, 1]

print("\nFRAUD DETECTION THRESHOLD ANALYSIS")
print("=" * 90)

print(
    f"{'Threshold':<12}"
    f"{'Precision':<14}"
    f"{'Recall':<12}"
    f"{'F1 Score':<12}"
    f"{'TP':<8}"
    f"{'FP':<8}"
    f"{'FN':<8}"
    f"{'TN':<8}"
)

for threshold in [
    0.10, 0.15, 0.20, 0.25, 0.30,
    0.35, 0.40, 0.45, 0.50, 0.60
]:

    predictions = (probabilities >= threshold).astype(int)

    precision = precision_score(
        y_test,
        predictions,
        zero_division=0
    )

    recall = recall_score(
        y_test,
        predictions,
        zero_division=0
    )

    f1 = f1_score(
        y_test,
        predictions,
        zero_division=0
    )

    tn, fp, fn, tp = confusion_matrix(
        y_test,
        predictions
    ).ravel()

    print(
        f"{threshold:<12.2f}"
        f"{precision:<14.3f}"
        f"{recall:<12.3f}"
        f"{f1:<12.3f}"
        f"{tp:<8}"
        f"{fp:<8}"
        f"{fn:<8}"
        f"{tn:<8}"
    )