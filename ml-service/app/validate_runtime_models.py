import json
import pandas as pd
import numpy as np

from pathlib import Path

from sklearn.model_selection import train_test_split
from sklearn.ensemble import (
    RandomForestClassifier,
    HistGradientBoostingClassifier
)
from sklearn.calibration import CalibratedClassifierCV
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


BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "runtime_compatible_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "runtime_compatible_validation.json"

FEATURES = [
    "amount",
    "transaction_hour",
    "is_weekend",
    "transactions_last_1h",
    "failed_txns_24h",
    "new_payee",
    "account_age_days",
    "average_amount_30d",
    "amount_to_average_ratio"
]

TARGET = "is_fraud"


def calculate_metrics(y_true, probabilities, threshold):

    predictions = (probabilities >= threshold).astype(int)

    matrix = confusion_matrix(
        y_true,
        predictions,
        labels=[0, 1]
    )

    return {
        "threshold": float(threshold),
        "precision": float(precision_score(
            y_true, predictions, zero_division=0
        )),
        "recall": float(recall_score(
            y_true, predictions, zero_division=0
        )),
        "f1_score": float(f1_score(
            y_true, predictions, zero_division=0
        )),
        "true_negatives": int(matrix[0][0]),
        "false_positives": int(matrix[0][1]),
        "false_negatives": int(matrix[1][0]),
        "true_positives": int(matrix[1][1])
    }


def main():

    print("\n" + "=" * 80)
    print("FINSENTINEL - RUNTIME-COMPATIBLE MODEL VALIDATION")
    print("=" * 80)

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET")
    print("Samples:", len(dataset))
    print("Features:", len(FEATURES))
    print("Fraud cases:", int(y.sum()))
    print("Fraud percentage:", round(y.mean() * 100, 2), "%")

    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y,
        test_size=0.30,
        random_state=42,
        stratify=y
    )

    X_validation, X_test, y_validation, y_test = train_test_split(
        X_temp, y_temp,
        test_size=0.50,
        random_state=42,
        stratify=y_temp
    )

    print("\nDATA SPLIT")
    print("Training:", len(X_train), "| Fraud:", int(y_train.sum()))
    print("Validation:", len(X_validation), "| Fraud:", int(y_validation.sum()))
    print("Testing:", len(X_test), "| Fraud:", int(y_test.sum()))

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

        "HistGradientBoosting_Sigmoid": CalibratedClassifierCV(
            estimator=HistGradientBoostingClassifier(
                max_iter=200,
                learning_rate=0.08,
                max_leaf_nodes=15,
                l2_regularization=1.0,
                random_state=42
            ),
            method="sigmoid",
            cv=5
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

    for name, model in models.items():

        print("\n" + "=" * 80)
        print("TRAINING:", name)
        print("=" * 80)

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

        threshold_results = []

        for threshold in np.arange(0.01, 0.51, 0.01):

            metrics = calculate_metrics(
                y_validation,
                validation_probabilities,
                threshold
            )

            threshold_results.append(metrics)

        best_threshold_result = max(
            threshold_results,
            key=lambda result: result["f1_score"]
        )

        selected_threshold = best_threshold_result["threshold"]

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

        print("\nVALIDATION")
        print("PR-AUC:", round(validation_pr_auc, 4))
        print("ROC-AUC:", round(validation_roc_auc, 4))
        print("Selected threshold:", selected_threshold)
        print("F1:", round(best_threshold_result["f1_score"], 4))

        print("\nUNTOUCHED TEST")
        print("Precision:", round(test_metrics["precision"], 4))
        print("Recall:", round(test_metrics["recall"], 4))
        print("F1:", round(test_metrics["f1_score"], 4))
        print("PR-AUC:", round(test_pr_auc, 4))
        print("ROC-AUC:", round(test_roc_auc, 4))

        print("\nCONFUSION MATRIX")
        print([
            [test_metrics["true_negatives"], test_metrics["false_positives"]],
            [test_metrics["false_negatives"], test_metrics["true_positives"]]
        ])

    results_df = pd.DataFrame(all_results)

    results_df = results_df.sort_values(
        by="validation_pr_auc",
        ascending=False
    )

    print("\n" + "=" * 110)
    print("RUNTIME-COMPATIBLE MODEL COMPARISON")
    print("=" * 110)

    print(results_df.to_string(index=False))

    RESULT_PATH.parent.mkdir(parents=True, exist_ok=True)

    with open(RESULT_PATH, "w", encoding="utf-8") as file:
        json.dump(all_results, file, indent=4)

    print("\nResults saved:", RESULT_PATH)

    print("\nIMPORTANT")
    print("Only nine runtime-compatible features were used.")
    print("Thresholds were selected using validation data.")
    print("Final metrics were calculated on untouched test data.")
    print("Labels are synthetic, not real banking fraud labels.")
    print("No production model was modified.")


if __name__ == "__main__":
    main()
