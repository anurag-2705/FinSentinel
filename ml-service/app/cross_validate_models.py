
import pandas as pd

from pathlib import Path

from sklearn.model_selection import RepeatedStratifiedKFold
from sklearn.ensemble import (
    RandomForestClassifier,
    HistGradientBoostingClassifier
)
from sklearn.metrics import (
    average_precision_score,
    roc_auc_score,
    precision_score,
    recall_score,
    f1_score
)


# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "cross_validation_results.csv"


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
# MODELS
# ============================================================

def get_models():

    return {

        "RandomForest": RandomForestClassifier(
            n_estimators=500,
            max_depth=10,
            min_samples_split=20,
            min_samples_leaf=8,
            class_weight="balanced_subsample",
            max_features="sqrt",
            random_state=42,
            n_jobs=-1
        ),

        "HistGradientBoosting": HistGradientBoostingClassifier(
            max_iter=200,
            learning_rate=0.08,
            max_leaf_nodes=15,
            l2_regularization=1.0,
            random_state=42
        )

    }


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)
    print("FINSENTINEL - REPEATED STRATIFIED CROSS VALIDATION")
    print("=" * 75)

    # --------------------------------------------------------
    # LOAD DATA
    # --------------------------------------------------------

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET INFORMATION")

    print("Total samples:", len(dataset))
    print("Fraud transactions:", int(y.sum()))
    print("Legitimate transactions:", int((y == 0).sum()))

    # --------------------------------------------------------
    # CROSS VALIDATION
    # --------------------------------------------------------

    cv = RepeatedStratifiedKFold(
        n_splits=5,
        n_repeats=3,
        random_state=42
    )

    models = get_models()

    all_results = []

    # --------------------------------------------------------
    # TRAIN AND EVALUATE
    # --------------------------------------------------------

    for model_name, model in models.items():

        print("\n" + "=" * 75)

        print("MODEL:", model_name)

        print("=" * 75)

        fold_number = 0

        for train_index, validation_index in cv.split(X, y):

            fold_number += 1

            X_train = X.iloc[train_index]
            X_validation = X.iloc[validation_index]

            y_train = y.iloc[train_index]
            y_validation = y.iloc[validation_index]

            model.fit(X_train, y_train)

            probabilities = model.predict_proba(
                X_validation
            )[:, 1]

            # Fixed threshold for fair comparison.
            # Threshold optimization will be a separate experiment.
            predictions = (probabilities >= 0.50).astype(int)

            metrics = {

                "model": model_name,

                "fold": fold_number,

                "pr_auc": average_precision_score(
                    y_validation,
                    probabilities
                ),

                "roc_auc": roc_auc_score(
                    y_validation,
                    probabilities
                ),

                "precision": precision_score(
                    y_validation,
                    predictions,
                    zero_division=0
                ),

                "recall": recall_score(
                    y_validation,
                    predictions,
                    zero_division=0
                ),

                "f1": f1_score(
                    y_validation,
                    predictions,
                    zero_division=0
                )

            }

            all_results.append(metrics)

            print(
                f"Fold {fold_number:02d}/15 | "
                f"PR-AUC: {metrics['pr_auc']:.4f} | "
                f"ROC-AUC: {metrics['roc_auc']:.4f} | "
                f"Precision: {metrics['precision']:.4f} | "
                f"Recall: {metrics['recall']:.4f} | "
                f"F1: {metrics['f1']:.4f}"
            )

    # --------------------------------------------------------
    # SAVE INDIVIDUAL RESULTS
    # --------------------------------------------------------

    results_df = pd.DataFrame(all_results)

    RESULT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    results_df.to_csv(
        RESULT_PATH,
        index=False
    )

    # --------------------------------------------------------
    # SUMMARY
    # --------------------------------------------------------

    metric_columns = [
        "pr_auc",
        "roc_auc",
        "precision",
        "recall",
        "f1"
    ]

    summary = results_df.groupby("model")[metric_columns].agg(
        ["mean", "std"]
    )

    print("\n" + "=" * 75)

    print("CROSS VALIDATION SUMMARY")

    print("=" * 75)

    print(summary.round(4).to_string())

    # --------------------------------------------------------
    # BEST MODEL
    # --------------------------------------------------------

    mean_pr_auc = results_df.groupby("model")["pr_auc"].mean()

    best_model = mean_pr_auc.idxmax()

    print("\nBEST MODEL BY MEAN PR-AUC:", best_model)

    print(
        "Mean PR-AUC:",
        round(mean_pr_auc.max(), 5)
    )

    print("\nResults saved to:")

    print(RESULT_PATH)

    print("\nIMPORTANT NOTES:")

    print("- Each model was evaluated on 15 validation folds.")

    print("- Both models use the same folds.")

    print("- Classification threshold is fixed at 0.50.")

    print("- No production model or existing model artifact was changed.")

    print("- Dataset contains synthetic fraud labels.")

    print("- Cross-validation folds overlap across repetitions;")
    print("  the 15 results are not 15 independent experiments.")


if __name__ == "__main__":

    main()
