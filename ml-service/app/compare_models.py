
import pandas as pd
import numpy as np

from pathlib import Path

from sklearn.model_selection import train_test_split
from sklearn.ensemble import (
    RandomForestClassifier,
    HistGradientBoostingClassifier
)

from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    average_precision_score,
    confusion_matrix
)

from imblearn.pipeline import Pipeline as ImbPipeline
from imblearn.over_sampling import SMOTE

from xgboost import XGBClassifier


# ============================================================
# PATH CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "model_comparison.csv"


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
# THRESHOLD EVALUATION
# ============================================================

def evaluate_model(name, model, X_test, y_test):

    probabilities = model.predict_proba(X_test)[:, 1]

    threshold_results = []

    thresholds = np.arange(0.05, 1.00, 0.05)

    roc_auc = roc_auc_score(
        y_test,
        probabilities
    )

    pr_auc = average_precision_score(
        y_test,
        probabilities
    )

    for threshold in thresholds:

        predictions = (
            probabilities >= threshold
        ).astype(int)

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

        matrix = confusion_matrix(
            y_test,
            predictions,
            labels=[0, 1]
        )

        threshold_results.append({

            "model": name,

            "threshold": round(float(threshold), 2),

            "precision": precision,

            "recall": recall,

            "f1_score": f1,

            "roc_auc": roc_auc,

            "pr_auc": pr_auc,

            "true_negatives": int(matrix[0][0]),

            "false_positives": int(matrix[0][1]),

            "false_negatives": int(matrix[1][0]),

            "true_positives": int(matrix[1][1])

        })

    return threshold_results


# ============================================================
# MAIN TRAINING FUNCTION
# ============================================================

def main():

    print("\n" + "=" * 70)
    print("FINSENTINEL - ML MODEL COMPARISON")
    print("=" * 70)

    # --------------------------------------------------------
    # LOAD DATASET
    # --------------------------------------------------------

    if not DATA_PATH.exists():

        raise FileNotFoundError(
            f"Dataset not found: {DATA_PATH}"
        )

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]

    y = dataset[TARGET]

    # --------------------------------------------------------
    # TRAIN TEST SPLIT
    # --------------------------------------------------------

    X_train, X_test, y_train, y_test = train_test_split(

        X,

        y,

        test_size=0.20,

        random_state=42,

        stratify=y

    )

    print("\nDATASET INFORMATION")

    print("Dataset shape:", dataset.shape)

    print("Training samples:", len(X_train))

    print("Testing samples:", len(X_test))

    print("Training fraud cases:", int(y_train.sum()))

    print("Testing fraud cases:", int(y_test.sum()))

    # --------------------------------------------------------
    # MODEL DEFINITIONS
    # --------------------------------------------------------

    negative_cases = len(y_train) - int(y_train.sum())

    positive_cases = int(y_train.sum())

    scale_weight = negative_cases / positive_cases

    models = {

        "RandomForest_Baseline": RandomForestClassifier(

            n_estimators=500,

            max_depth=10,

            min_samples_split=20,

            min_samples_leaf=8,

            class_weight="balanced_subsample",

            max_features="sqrt",

            random_state=42,

            n_jobs=-1

        ),

        "RandomForest_SMOTE": ImbPipeline([

            (
                "smote",

                SMOTE(

                    random_state=42,

                    sampling_strategy=0.5

                )

            ),

            (

                "model",

                RandomForestClassifier(

                    n_estimators=500,

                    max_depth=10,

                    min_samples_split=20,

                    min_samples_leaf=8,

                    max_features="sqrt",

                    random_state=42,

                    n_jobs=-1

                )

            )

        ]),

        "HistGradientBoosting": HistGradientBoostingClassifier(

            max_iter=200,

            learning_rate=0.08,

            max_leaf_nodes=15,

            l2_regularization=1.0,

            random_state=42

        ),

        "XGBoost": XGBClassifier(

            n_estimators=400,

            max_depth=4,

            learning_rate=0.05,

            subsample=0.8,

            colsample_bytree=0.8,

            scale_pos_weight=scale_weight,

            eval_metric="aucpr",

            random_state=42,

            n_jobs=-1

        )

    }

    # --------------------------------------------------------
    # TRAIN AND EVALUATE
    # --------------------------------------------------------

    all_results = []

    for name, model in models.items():

        print("\n" + "=" * 70)

        print("TRAINING MODEL:", name)

        print("=" * 70)

        model.fit(

            X_train,

            y_train

        )

        threshold_results = evaluate_model(

            name,

            model,

            X_test,

            y_test

        )

        all_results.extend(threshold_results)

        print("\nROC-AUC:", round(
            threshold_results[0]["roc_auc"], 4
        ))

        print("PR-AUC:", round(
            threshold_results[0]["pr_auc"], 4
        ))

        print("\nTHRESHOLD-WISE RESULTS")

        threshold_df = pd.DataFrame(threshold_results)

        print(

            threshold_df[

                [

                    "threshold",

                    "precision",

                    "recall",

                    "f1_score",

                    "false_positives",

                    "false_negatives",

                    "true_positives"

                ]

            ].round(4).to_string(index=False)

        )

    # --------------------------------------------------------
    # SAVE RESULTS
    # --------------------------------------------------------

    results_df = pd.DataFrame(all_results)

    results_df = results_df.sort_values(

        by=["model", "threshold"]

    )

    RESULT_PATH.parent.mkdir(

        parents=True,

        exist_ok=True

    )

    results_df.to_csv(

        RESULT_PATH,

        index=False

    )

    # --------------------------------------------------------
    # BEST THRESHOLD PER MODEL
    # --------------------------------------------------------

    print("\n\n" + "=" * 90)

    print("BEST F1 THRESHOLD FOR EACH MODEL")

    print("=" * 90)

    best_results = (

        results_df

        .sort_values("f1_score", ascending=False)

        .groupby("model", as_index=False)

        .first()

    )

    print(

        best_results[

            [

                "model",

                "threshold",

                "precision",

                "recall",

                "f1_score",

                "roc_auc",

                "pr_auc",

                "false_positives",

                "false_negatives"

            ]

        ].round(4).to_string(index=False)

    )

    # --------------------------------------------------------
    # FINAL RESULTS
    # --------------------------------------------------------

    print("\n\nRESULTS SAVED SUCCESSFULLY")

    print(RESULT_PATH)

    print("\nTotal evaluation records:", len(results_df))

    print("\nIMPORTANT:")

    print(
        "Thresholds are evaluated on the same held-out test set."
    )

    print(
        "Do not select a final production threshold using test results alone."
    )

    print(
        "Use cross-validation or a separate validation set for model selection."
    )

    print(
        "These results are based on synthetic data, not real banking transactions."
    )


# ============================================================
# ENTRY POINT
# ============================================================

if __name__ == "__main__":

    main()
