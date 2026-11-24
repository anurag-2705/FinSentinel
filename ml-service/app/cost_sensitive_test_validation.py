
import numpy as np
import pandas as pd

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


# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "cost_sensitive_test_results.csv"

SUMMARY_PATH = BASE_DIR / "models" / "cost_sensitive_test_summary.csv"


# ============================================================
# CONFIGURATION
# ============================================================

RANDOM_SEEDS = [21, 42, 100, 123, 2026]

THRESHOLDS = np.arange(0.01, 0.51, 0.01)

FP_COST = 1

FN_COST_SCENARIOS = [5, 10, 20]

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
# MODEL CREATION
# ============================================================

def create_models(seed):

    random_forest = RandomForestClassifier(
        n_estimators=500,
        max_depth=10,
        min_samples_split=20,
        min_samples_leaf=8,
        class_weight="balanced_subsample",
        max_features="sqrt",
        random_state=seed,
        n_jobs=-1
    )

    histogram_gradient_boosting = CalibratedClassifierCV(
        estimator=HistGradientBoostingClassifier(
            max_iter=200,
            learning_rate=0.08,
            max_leaf_nodes=15,
            l2_regularization=1.0,
            random_state=seed
        ),
        method="sigmoid",
        cv=5
    )

    return {
        "RandomForest": random_forest,
        "HistGradientBoosting_Sigmoid": histogram_gradient_boosting
    }


# ============================================================
# THRESHOLD SELECTION
# ============================================================

def select_cost_threshold(
    y_true,
    probabilities,
    fn_cost
):

    best_threshold = None
    best_cost = float("inf")

    for threshold in THRESHOLDS:

        predictions = (
            probabilities >= threshold
        ).astype(int)

        tn, fp, fn, tp = confusion_matrix(
            y_true,
            predictions,
            labels=[0, 1]
        ).ravel()

        total_cost = (
            FP_COST * fp
            + fn_cost * fn
        )

        if total_cost < best_cost:

            best_cost = total_cost
            best_threshold = float(threshold)

    return best_threshold, best_cost


# ============================================================
# TEST EVALUATION
# ============================================================

def evaluate_test(
    model,
    X_test,
    y_test,
    threshold,
    fn_cost
):

    probabilities = model.predict_proba(
        X_test
    )[:, 1]

    predictions = (
        probabilities >= threshold
    ).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_test,
        predictions,
        labels=[0, 1]
    ).ravel()

    total_cost = (
        FP_COST * fp
        + fn_cost * fn
    )

    return {

        "pr_auc": average_precision_score(
            y_test,
            probabilities
        ),

        "roc_auc": roc_auc_score(
            y_test,
            probabilities
        ),

        "precision": precision_score(
            y_test,
            predictions,
            zero_division=0
        ),

        "recall": recall_score(
            y_test,
            predictions,
            zero_division=0
        ),

        "f1": f1_score(
            y_test,
            predictions,
            zero_division=0
        ),

        "true_positives": int(tp),

        "false_positives": int(fp),

        "true_negatives": int(tn),

        "false_negatives": int(fn),

        "total_cost": int(total_cost)

    }


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)

    print("FINSENTINEL - COST-SENSITIVE TEST VALIDATION")

    print("=" * 75)

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET INFORMATION")

    print("Total samples:", len(dataset))
    print("Total fraud:", int(y.sum()))
    print("Fraud percentage:", round(y.mean() * 100, 2), "%")

    all_results = []

    # ========================================================
    # MULTI-SEED EXPERIMENT
    # ========================================================

    for seed in RANDOM_SEEDS:

        print("\n" + "=" * 75)

        print("RANDOM SEED:", seed)

        print("=" * 75)

        # ----------------------------------------------------
        # DATA SPLIT: 70 / 15 / 15
        # ----------------------------------------------------

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

        print("Training:", len(X_train))
        print("Validation:", len(X_validation))
        print("Testing:", len(X_test))

        print("Test fraud:", int(y_test.sum()))

        models = create_models(seed)

        # ----------------------------------------------------
        # TRAIN MODELS
        # ----------------------------------------------------

        for model_name, model in models.items():

            print("\nTraining:", model_name)

            model.fit(
                X_train,
                y_train
            )

            validation_probabilities = model.predict_proba(
                X_validation
            )[:, 1]

            test_probabilities = model.predict_proba(
                X_test
            )[:, 1]

            validation_pr_auc = average_precision_score(
                y_validation,
                validation_probabilities
            )

            test_pr_auc = average_precision_score(
                y_test,
                test_probabilities
            )

            test_roc_auc = roc_auc_score(
                y_test,
                test_probabilities
            )

            print("\nValidation PR-AUC:",
                  round(validation_pr_auc, 5))

            print("Test PR-AUC:",
                  round(test_pr_auc, 5))

            print("Test ROC-AUC:",
                  round(test_roc_auc, 5))

            # ------------------------------------------------
            # SELECT THRESHOLD ON VALIDATION
            # EVALUATE ON TEST
            # ------------------------------------------------

            for fn_cost in FN_COST_SCENARIOS:

                threshold, validation_cost = select_cost_threshold(
                    y_validation,
                    validation_probabilities,
                    fn_cost
                )

                test_result = evaluate_test(
                    model,
                    X_test,
                    y_test,
                    threshold,
                    fn_cost
                )

                result = {

                    "seed": seed,

                    "model": model_name,

                    "false_positive_cost": FP_COST,

                    "false_negative_cost": fn_cost,

                    "threshold": threshold,

                    "validation_cost": validation_cost,

                    "validation_pr_auc": validation_pr_auc,

                    "test_pr_auc": test_result["pr_auc"],

                    "test_roc_auc": test_result["roc_auc"],

                    "precision": test_result["precision"],

                    "recall": test_result["recall"],

                    "f1": test_result["f1"],

                    "true_positives": test_result["true_positives"],

                    "false_positives": test_result["false_positives"],

                    "true_negatives": test_result["true_negatives"],

                    "false_negatives": test_result["false_negatives"],

                    "test_cost": test_result["total_cost"]

                }

                all_results.append(result)

                print("\nCost scenario: FN cost =", fn_cost)

                print("Selected threshold:", threshold)

                print("Validation cost:", validation_cost)

                print("Test cost:", test_result["total_cost"])

                print("Test precision:",
                      round(test_result["precision"], 4))

                print("Test recall:",
                      round(test_result["recall"], 4))

                print("Test F1:",
                      round(test_result["f1"], 4))

                print("Test false positives:",
                      test_result["false_positives"])

                print("Test missed fraud:",
                      test_result["false_negatives"])

    # ========================================================
    # SAVE INDIVIDUAL RESULTS
    # ========================================================

    results_df = pd.DataFrame(all_results)

    RESULT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    results_df.to_csv(
        RESULT_PATH,
        index=False
    )

    # ========================================================
    # SUMMARY STATISTICS
    # ========================================================

    group_columns = [
        "model",
        "false_negative_cost"
    ]

    metric_columns = [
        "test_pr_auc",
        "test_roc_auc",
        "precision",
        "recall",
        "f1",
        "threshold",
        "false_positives",
        "false_negatives",
        "test_cost"
    ]

    summary = results_df.groupby(
        group_columns
    )[metric_columns].agg(["mean", "std"])

    summary.to_csv(SUMMARY_PATH)

    # ========================================================
    # FINAL COMPARISON
    # ========================================================

    print("\n" + "=" * 75)

    print("FINAL TEST COST COMPARISON")

    print("=" * 75)

    mean_results = results_df.groupby(
        group_columns
    )[metric_columns].mean().reset_index()

    for fn_cost in FN_COST_SCENARIOS:

        print("\n" + "-" * 75)

        print("FALSE NEGATIVE COST:", fn_cost)

        print("-" * 75)

        scenario = mean_results[
            mean_results["false_negative_cost"] == fn_cost
        ]

        for _, row in scenario.iterrows():

            print("\nModel:", row["model"])

            print(
                "Mean test cost:",
                round(row["test_cost"], 2)
            )

            print(
                "Mean test PR-AUC:",
                round(row["test_pr_auc"], 5)
            )

            print(
                "Mean test precision:",
                round(row["precision"], 4)
            )

            print(
                "Mean test recall:",
                round(row["recall"], 4)
            )

            print(
                "Mean test F1:",
                round(row["f1"], 4)
            )

            print(
                "Mean false positives:",
                round(row["false_positives"], 2)
            )

            print(
                "Mean false negatives:",
                round(row["false_negatives"], 2)
            )

    # ========================================================
    # SAVE SUMMARY
    # ========================================================

    print("\n" + "=" * 75)

    print("EXPERIMENT COMPLETED")

    print("=" * 75)

    print("\nIndividual results:")

    print(RESULT_PATH)

    print("\nSummary results:")

    print(SUMMARY_PATH)

    print("\nIMPORTANT NOTES")

    print("- Thresholds were selected using validation data only.")

    print("- Test data was not used for threshold selection.")

    print("- Test costs are evaluated on untouched test partitions.")

    print("- Cost assumptions are illustrative.")

    print("- Different seeds reuse the same synthetic dataset.")

    print("- This is not external real-world validation.")

    print("- No production model was modified.")


if __name__ == "__main__":

    main()
