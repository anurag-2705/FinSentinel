import json
from pathlib import Path

import joblib
import pandas as pd

from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    roc_auc_score,
    average_precision_score
)
from sklearn.model_selection import train_test_split


BASE_DIR = Path(__file__).resolve().parents[1]
DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"
MODEL_DIR = BASE_DIR / "models"

MODEL_PATH = MODEL_DIR / "fraud_model.joblib"
METRICS_PATH = MODEL_DIR / "evaluation_metrics.json"

FEATURES = [
    "amount",
    "transaction_hour",
    "is_weekend",
    "transactions_last_1h",
    "failed_txns_24h",
    "new_device",
    "new_payee",
    "geo_distance_km",
    "account_age_days",
    "is_international",
    "average_amount_30d",
    "amount_to_average_ratio"
]

TARGET = "is_fraud"


def train():
    if not DATA_PATH.exists():
        raise FileNotFoundError(f"Dataset not found: {DATA_PATH}")

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )

    print("Training distribution:")
    print(y_train.value_counts())

    model = RandomForestClassifier(
        n_estimators=500,
        max_depth=10,
        min_samples_split=20,
        min_samples_leaf=8,
        class_weight="balanced_subsample",
        max_features="sqrt",
        random_state=42,
        n_jobs=-1
    )

    print("\nTraining class-weighted Random Forest...")

    model.fit(X_train, y_train)

    probabilities = model.predict_proba(X_test)[:, 1]

    threshold = 0.50
    predictions = (probabilities >= threshold).astype(int)

    report = classification_report(
        y_test,
        predictions,
        output_dict=True,
        zero_division=0
    )

    metrics = {
        "model": "RandomForestClassifier",
        "threshold": threshold,
        "training_samples": len(X_train),
        "testing_samples": len(X_test),
        "fraud_samples_test": int(y_test.sum()),
        "precision": report["1"]["precision"],
        "recall": report["1"]["recall"],
        "f1_score": report["1"]["f1-score"],
        "roc_auc": roc_auc_score(y_test, probabilities),
        "average_precision": average_precision_score(
            y_test,
            probabilities
        ),
        "confusion_matrix": confusion_matrix(
            y_test,
            predictions
        ).tolist()
    }

    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    joblib.dump(
        {
            "model": model,
            "features": FEATURES,
            "threshold": threshold
        },
        MODEL_PATH
    )

    with open(METRICS_PATH, "w", encoding="utf-8") as file:
        json.dump(metrics, file, indent=4)

    print("\nMODEL EVALUATION")
    print("=" * 40)

    print(classification_report(
        y_test,
        predictions,
        zero_division=0
    ))

    print("Classification threshold:", threshold)
    print("ROC-AUC:", round(metrics["roc_auc"], 4))
    print(
        "Average Precision:",
        round(metrics["average_precision"], 4)
    )

    print("\nConfusion Matrix:")
    print(metrics["confusion_matrix"])

    print("\nModel saved:", MODEL_PATH)
    print("Metrics saved:", METRICS_PATH)


if __name__ == "__main__":
    train()