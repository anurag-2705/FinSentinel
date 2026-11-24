
import json
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
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "validation_results.json"


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
# METRICS
# ============================================================

def calculate_metrics(y_true, probabilities, threshold):

    predictions = (
        probabilities >= threshold
    ).astype(int)

    matrix = confusion_matrix(
        y_true,
        predictions,
        labels=[0, 1]
    )

    return {
        "threshold": float(threshold),

        "precision": float(
            precision_score(
                y_true,
                predictions,
                zero_division=0
            )
        ),

        "recall": float(
            recall_score(
                y_true,
                predictions,
                zero_division=0
            )
        ),

        "f1_score": float(
            f1_score(
                y_true,
                predictions,
                zero_division=0
            )
        ),

        "true_negatives": int(matrix[0][0]),

        "false_positives": int(matrix[0][1]),

        "false_negatives": int(matrix[1][0]),

        "true_positives": int(matrix[1][1])
    }


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)
    print("FINSENTINEL - PROPER MODEL VALIDATION")
    print("=" * 75)

    # --------------------------------------------------------
    # LOAD DATA
    # --------------------------------------------------------

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    # --------------------------------------------------------
    # 70% TRAIN, 30% TEMPORARY
    # --------------------------------------------------------

    X_train, X_temp, y_train, y_temp = train_test_split(
        X,
        y,
        test_size=0.30,
        random_state=42,
        stratify=y
    )

    # --------------------------------------------------------
    # 15% VALIDATION, 15% TEST
    # --------------------------------------------------------

    X_validation, X_test, y_validation, y_test = train_test_split(
        X_temp,
        y_temp,
        test_size=0.50,
        random_state=42,
        stratify=y_temp
    )

    print("\nDATA SPLIT")

    print("Training:", len(X_train))
    print("Validation:", len(X_validation))
    print("Testing:", len(X_test))

    print("Training fraud:", int(y_train.sum()))
    print("Validation fraud:", int(y_validation.sum()))
    print("Testing fraud:", int(y_test.sum()))

    # --------------------------------------------------------
    # MODEL CONFIGURATION
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

    all_results = []

    # --------------------------------------------------------
    # TRAIN AND VALIDATE
    # --------------------------------------------------------

    for name, model in models.items():

        print("\n" + "=" * 75)
        print("TRAINING:", name)
        print("=" * 75)

        model.fit(X_train, y_train)

        validation_probabilities = model.predict_proba(
            X_validation
        )[:, 1]

        validation_pr_auc = average_precision_score(
            y_validation,
            validation_probabilities
        )

        validation_roc_auc = roc_auc_score(
            y_validation,
            validation_probabilities
        )

        # Threshold selection happens ONLY on validation data.
        threshold_results = []

        for threshold in np.arange(0.05, 1.00, 0.05):

            metrics = calculate_metrics(
                y_validation,
                validation_probabilities,
                threshold
            )

            threshold_results.append(metrics)

        # Select threshold with highest validation F1.
        best_threshold_result = max(
            threshold_results,
            key=lambda result: result["f1_score"]
        )

        selected_threshold = best_threshold_result["threshold"]

        # ----------------------------------------------------
        # FINAL TEST EVALUATION
        # ----------------------------------------------------

        test_probabilities = model.predict_proba(
            X_test
        )[:, 1]

        test_metrics = calculate_metrics(
            y_test,
            test_probabilities,
            selected_threshold
        )

        test_pr_auc = average_precision_score(
            y_test,
            test_probabilities
        )

        test_roc_auc = roc_auc_score(
            y_test,
            test_probabilities
        )

        result = {

            "model": name,

            "validation_pr_auc": float(validation_pr_auc),

            "validation_roc_auc": float(validation_roc_auc),

            "selected_threshold": float(selected_threshold),

            "validation_precision": best_threshold_result["precision"],

            "validation_recall": best_threshold_result["recall"],

            "validation_f1": best_threshold_result["f1_score"],

            "test_precision": test_metrics["precision"],

            "test_recall": test_metrics["recall"],

            "test_f1": test_metrics["f1_score"],

            "test_pr_auc": float(test_pr_auc),

            "test_roc_auc": float(test_roc_auc),

            "test_true_negatives": test_metrics["true_negatives"],

            "test_false_positives": test_metrics["false_positives"],

            "test_false_negatives": test_metrics["false_negatives"],

            "test_true_positives": test_metrics["true_positives"]

        }

        all_results.append(result)

        print("\nVALIDATION RESULTS")

        print("PR-AUC:", round(validation_pr_auc, 4))

        print("ROC-AUC:", round(validation_roc_auc, 4))

        print("Selected threshold:", selected_threshold)

        print("Validation F1:", round(
            best_threshold_result["f1_score"], 4
        ))

        print("\nUNTOUCHED TEST RESULTS")

        print("Precision:", round(test_metrics["precision"], 4))

        print("Recall:", round(test_metrics["recall"], 4))

        print("F1:", round(test_metrics["f1_score"], 4))

        print("PR-AUC:", round(test_pr_auc, 4))

        print("ROC-AUC:", round(test_roc_auc, 4))

        print("\nConfusion Matrix:")

        print([
            [
                test_metrics["true_negatives"],
                test_metrics["false_positives"]
            ],
            [
                test_metrics["false_negatives"],
                test_metrics["true_positives"]
            ]
        ])

    # --------------------------------------------------------
    # SUMMARY
    # --------------------------------------------------------

    results_df = pd.DataFrame(all_results)

    results_df = results_df.sort_values(
        by="validation_pr_auc",
        ascending=False
    )

    print("\n\n" + "=" * 100)
    print("FINAL VALIDATION COMPARISON")
    print("=" * 100)

    print(results_df.to_string(index=False))

    # --------------------------------------------------------
    # SAVE RESULTS
    # --------------------------------------------------------

    RESULT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    with open(RESULT_PATH, "w", encoding="utf-8") as file:

        json.dump(
            all_results,
            file,
            indent=4
        )

    print("\nResults saved to:")

    print(RESULT_PATH)

    print("\nIMPORTANT:")

    print("Model selection and threshold selection used validation data.")

    print("Final metrics were calculated on the separate test set.")

    print("The dataset contains synthetic transactions only.")

    print("No existing production model was modified.")


if __name__ == "__main__":

    main()
