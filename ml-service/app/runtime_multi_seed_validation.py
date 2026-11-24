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
    average_precision_score,
    roc_auc_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix
)

from imblearn.pipeline import Pipeline as ImbPipeline
from imblearn.over_sampling import SMOTE

from xgboost import XGBClassifier


BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "runtime_compatible_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "runtime_multi_seed_results.csv"

SUMMARY_PATH = BASE_DIR / "models" / "runtime_multi_seed_summary.csv"

RANDOM_SEEDS = [21, 42, 100, 123, 2026]

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


def create_models(seed, y_train):

    negative_cases = len(y_train) - int(y_train.sum())
    positive_cases = int(y_train.sum())

    scale_weight = negative_cases / positive_cases

    return {
        "RandomForest_Baseline": RandomForestClassifier(
            n_estimators=500,
            max_depth=10,
            min_samples_split=20,
            min_samples_leaf=8,
            class_weight="balanced_subsample",
            max_features="sqrt",
            random_state=seed,
            n_jobs=-1
        ),

        "RandomForest_SMOTE": ImbPipeline([
            (
                "smote",
                SMOTE(
                    random_state=seed,
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
                    random_state=seed,
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
                random_state=seed
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
            random_state=seed,
            n_jobs=-1
        )
    }


def select_threshold(y_true, probabilities):

    best_threshold = 0.50
    best_f1 = -1

    for threshold in np.arange(0.01, 0.51, 0.01):

        predictions = (
            probabilities >= threshold
        ).astype(int)

        current_f1 = f1_score(
            y_true,
            predictions,
            zero_division=0
        )

        if current_f1 > best_f1:
            best_f1 = current_f1
            best_threshold = round(float(threshold), 2)

    return best_threshold, best_f1


def evaluate_model(model, threshold, X_test, y_test):

    probabilities = model.predict_proba(X_test)[:, 1]

    predictions = (
        probabilities >= threshold
    ).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_test,
        predictions,
        labels=[0, 1]
    ).ravel()

    return {
        "pr_auc": average_precision_score(
            y_test, probabilities
        ),

        "roc_auc": roc_auc_score(
            y_test, probabilities
        ),

        "precision": precision_score(
            y_test, predictions, zero_division=0
        ),

        "recall": recall_score(
            y_test, predictions, zero_division=0
        ),

        "f1": f1_score(
            y_test, predictions, zero_division=0
        ),

        "true_positives": int(tp),
        "false_positives": int(fp),
        "true_negatives": int(tn),
        "false_negatives": int(fn)
    }


def main():

    print("\n" + "=" * 85)
    print("FINSENTINEL - RUNTIME-COMPATIBLE MULTI-SEED VALIDATION")
    print("=" * 85)

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET INFORMATION")
    print("Total samples:", len(dataset))
    print("Features:", len(FEATURES))
    print("Fraud cases:", int(y.sum()))
    print("Fraud percentage:", round(y.mean() * 100, 2), "%")

    print("\nFEATURES:")
    for feature in FEATURES:
        print("-", feature)

    print("\nRANDOM SEEDS:", RANDOM_SEEDS)

    all_results = []

    for seed in RANDOM_SEEDS:

        print("\n" + "=" * 85)
        print("RANDOM SEED:", seed)
        print("=" * 85)

        X_train, X_temp, y_train, y_temp = train_test_split(
            X,
            y,
            test_size=0.30,
            random_state=seed,
            stratify=y
        )

        X_validation, X_test, y_validation, y_test = train_test_split(
            X_temp,
            y_temp,
            test_size=0.50,
            random_state=seed,
            stratify=y_temp
        )

        print("\nDATA SPLIT")
        print("Training:", len(X_train), "| Fraud:", int(y_train.sum()))
        print("Validation:", len(X_validation), "| Fraud:", int(y_validation.sum()))
        print("Testing:", len(X_test), "| Fraud:", int(y_test.sum()))

        models = create_models(seed, y_train)

        for model_name, model in models.items():

            print("\n" + "-" * 65)
            print("TRAINING:", model_name)
            print("-" * 65)

            model.fit(X_train, y_train)

            validation_probabilities = model.predict_proba(
                X_validation
            )[:, 1]

            validation_pr_auc = average_precision_score(
                y_validation,
                validation_probabilities
            )

            threshold, validation_f1 = select_threshold(
                y_validation,
                validation_probabilities
            )

            print("Validation PR-AUC:", round(validation_pr_auc, 5))
            print("Selected threshold:", threshold)
            print("Validation F1:", round(validation_f1, 5))

            test_metrics = evaluate_model(
                model,
                threshold,
                X_test,
                y_test
            )

            result = {
                "model": model_name,
                "seed": seed,
                "threshold": threshold,
                "validation_pr_auc": float(validation_pr_auc),
                "validation_f1": float(validation_f1),
                **test_metrics
            }

            all_results.append(result)

            print("\nUNTOUCHED TEST RESULTS")

            for metric, value in test_metrics.items():
                print(f"{metric}: {value:.5f}" if isinstance(value, (float, np.floating)) else f"{metric}: {value}")

    results_df = pd.DataFrame(all_results)

    RESULT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    results_df.to_csv(
        RESULT_PATH,
        index=False
    )

    metric_columns = [
        "pr_auc",
        "roc_auc",
        "precision",
        "recall",
        "f1",
        "threshold",
        "false_positives",
        "false_negatives"
    ]

    summary = results_df.groupby("model")[
        metric_columns
    ].agg(["mean", "std"])

    print("\n" + "=" * 100)
    print("RUNTIME-COMPATIBLE MULTI-SEED SUMMARY")
    print("=" * 100)

    print(summary.round(4).to_string())

    summary.to_csv(SUMMARY_PATH)

    print("\nMODEL RANKING BY MEAN TEST PR-AUC")

    ranking = results_df.groupby("model")["pr_auc"].mean().sort_values(
        ascending=False
    )

    print(ranking.round(5).to_string())

    print("\nRESULT FILES")
    print("Individual results:", RESULT_PATH)
    print("Summary:", SUMMARY_PATH)

    print("\nIMPORTANT NOTES")
    print("- Five random seeds were evaluated.")
    print("- All four models used identical splits within each seed.")
    print("- Threshold selection used validation data only.")
    print("- Each test set remained untouched during its run.")
    print("- Repeated splits reuse the same underlying dataset.")
    print("- Results are experimental, not external validation.")
    print("- Fraud labels are synthetic.")
    print("- No production model was modified.")


if __name__ == "__main__":
    main()
