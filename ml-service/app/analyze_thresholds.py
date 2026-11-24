
import numpy as np
import pandas as pd

from pathlib import Path

from sklearn.base import clone
from sklearn.model_selection import RepeatedStratifiedKFold
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.metrics import (
    average_precision_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix
)


# ============================================================
# PATH CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "threshold_analysis.csv"


# ============================================================
# FEATURES
# ============================================================

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


# ============================================================
# MODEL
# ============================================================

MODEL = HistGradientBoostingClassifier(
    max_iter=200,
    learning_rate=0.08,
    max_leaf_nodes=15,
    l2_regularization=1.0,
    random_state=42
)


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)
    print("FINSENTINEL - FRAUD THRESHOLD ANALYSIS")
    print("=" * 75)

    # --------------------------------------------------------
    # LOAD DATA
    # --------------------------------------------------------

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET].to_numpy()

    print("\nDATASET INFORMATION")

    print("Total transactions:", len(dataset))
    print("Fraud transactions:", int(y.sum()))
    print("Legitimate transactions:", int((y == 0).sum()))

    # --------------------------------------------------------
    # REPEATED STRATIFIED CROSS VALIDATION
    # --------------------------------------------------------

    cv = RepeatedStratifiedKFold(
        n_splits=5,
        n_repeats=3,
        random_state=42
    )

    probability_sum = np.zeros(len(X))
    prediction_count = np.zeros(len(X))

    print("\nGenerating out-of-fold predictions...")

    fold_number = 0

    for train_index, validation_index in cv.split(X, y):

        fold_number += 1

        X_train = X.iloc[train_index]
        X_validation = X.iloc[validation_index]

        y_train = y[train_index]

        model = clone(MODEL)

        model.fit(X_train, y_train)

        probabilities = model.predict_proba(
            X_validation
        )[:, 1]

        probability_sum[validation_index] += probabilities

        prediction_count[validation_index] += 1

        print(f"Completed fold {fold_number}/15")

    # --------------------------------------------------------
    # AVERAGE OOF PROBABILITIES
    # --------------------------------------------------------

    if not np.all(prediction_count == 3):
        raise RuntimeError(
            "Expected exactly 3 out-of-fold predictions per sample."
        )

    oof_probabilities = probability_sum / prediction_count

    pr_auc = average_precision_score(
        y,
        oof_probabilities
    )

    print("\nOverall OOF PR-AUC:", round(pr_auc, 5))

    print(
        "Average predicted fraud probability:",
        round(float(oof_probabilities.mean()), 5)
    )

    print(
        "Maximum predicted fraud probability:",
        round(float(oof_probabilities.max()), 5)
    )

    print(
        "Minimum predicted fraud probability:",
        round(float(oof_probabilities.min()), 5)
    )

    # --------------------------------------------------------
    # THRESHOLD ANALYSIS
    # --------------------------------------------------------

    print("\nEvaluating thresholds from 0.01 to 0.50...")

    results = []

    thresholds = np.arange(0.01, 0.501, 0.01)

    for threshold in thresholds:

        predictions = (
            oof_probabilities >= threshold
        ).astype(int)

        tn, fp, fn, tp = confusion_matrix(
            y,
            predictions,
            labels=[0, 1]
        ).ravel()

        precision = precision_score(
            y,
            predictions,
            zero_division=0
        )

        recall = recall_score(
            y,
            predictions,
            zero_division=0
        )

        f1 = f1_score(
            y,
            predictions,
            zero_division=0
        )

        results.append({

            "threshold": round(float(threshold), 2),

            "precision": precision,

            "recall": recall,

            "f1": f1,

            "true_positives": int(tp),

            "false_positives": int(fp),

            "true_negatives": int(tn),

            "false_negatives": int(fn)

        })

    results_df = pd.DataFrame(results)

    # --------------------------------------------------------
    # BEST THRESHOLD BY F1
    # --------------------------------------------------------

    best_index = results_df["f1"].idxmax()

    best_result = results_df.loc[best_index]

    print("\n" + "=" * 75)

    print("BEST THRESHOLD BY F1")

    print("=" * 75)

    print(best_result.to_string())

    # --------------------------------------------------------
    # THRESHOLD COMPARISON
    # --------------------------------------------------------

    print("\n" + "=" * 75)

    print("SELECTED THRESHOLD COMPARISON")

    print("=" * 75)

    selected_thresholds = [
        0.01,
        0.02,
        0.03,
        0.05,
        0.10,
        0.15,
        0.20,
        0.30,
        0.40,
        0.50
    ]

    comparison = results_df[
        results_df["threshold"].isin(selected_thresholds)
    ]

    print(comparison.round(4).to_string(index=False))

    # --------------------------------------------------------
    # HIGH RECALL THRESHOLD
    # --------------------------------------------------------

    high_recall = results_df[
        results_df["recall"] >= 0.50
    ]

    print("\nTHRESHOLDS WITH AT LEAST 50% RECALL")

    if high_recall.empty:

        print("No threshold achieved 50% recall.")

    else:

        print(
            high_recall[
                [
                    "threshold",
                    "precision",
                    "recall",
                    "f1",
                    "false_positives",
                    "false_negatives"
                ]
            ].round(4).to_string(index=False)
        )

    # --------------------------------------------------------
    # SAVE RESULTS
    # --------------------------------------------------------

    RESULT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    results_df.to_csv(
        RESULT_PATH,
        index=False
    )

    print("\nResults saved to:")

    print(RESULT_PATH)

    # --------------------------------------------------------
    # IMPORTANT NOTES
    # --------------------------------------------------------

    print("\nIMPORTANT NOTES")

    print("- Predictions are out-of-fold predictions.")

    print("- Each transaction receives 3 predictions.")

    print("- The final probability is their average.")

    print("- Threshold selection is exploratory.")

    print("- These results are not independent final test results.")

    print("- Dataset contains synthetic fraud labels.")

    print("- No production model was modified.")


if __name__ == "__main__":

    main()
