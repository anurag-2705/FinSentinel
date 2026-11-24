
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

RESULT_PATH = BASE_DIR / "models" / "cost_sensitive_results.csv"

SUMMARY_PATH = BASE_DIR / "models" / "cost_sensitive_summary.csv"


# ============================================================
# CONFIGURATION
# ============================================================

RANDOM_SEEDS = [21, 42, 100, 123, 2026]

THRESHOLDS = np.arange(0.01, 0.51, 0.01)

# Illustrative cost scenarios:
# One false positive has a cost of 1 unit.
# A missed fraud has a cost of 5, 10, or 20 units.

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
# MAIN EXPERIMENT
# ============================================================

def main():

    print("\n" + "=" * 75)
    print("FINSENTINEL - COST-SENSITIVE THRESHOLD ANALYSIS")
    print("=" * 75)

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET INFORMATION")

    print("Total samples:", len(dataset))
    print("Total fraud:", int(y.sum()))
    print("Fraud percentage:", round(y.mean() * 100, 2), "%")

    print("\nRANDOM SEEDS:", RANDOM_SEEDS)
    print("FALSE POSITIVE COST:", FP_COST)
    print("FALSE NEGATIVE COST SCENARIOS:", FN_COST_SCENARIOS)

    all_results = []

    # ========================================================
    # RUN EXPERIMENT FOR EACH SEED
    # ========================================================

    for seed in RANDOM_SEEDS:

        print("\n" + "=" * 75)
        print("RANDOM SEED:", seed)
        print("=" * 75)

        # 70% training, 15% validation, 15% reserved test.
        # Test data is deliberately not used in this experiment.

        X_train, X_temp, y_train, y_temp = train_test_split(
            X,
            y,
            test_size=0.30,
            random_state=seed,
            stratify=y
        )

        X_validation, X_unused_test, y_validation, y_unused_test = (
            train_test_split(
                X_temp,
                y_temp,
                test_size=0.50,
                random_state=seed,
                stratify=y_temp
            )
        )

        print("Training samples:", len(X_train))
        print("Validation samples:", len(X_validation))
        print("Reserved test samples:", len(X_unused_test))

        models = create_models(seed)

        for model_name, model in models.items():

            print("\nTraining:", model_name)

            model.fit(X_train, y_train)

            validation_probabilities = model.predict_proba(
                X_validation
            )[:, 1]

            validation_pr_auc = average_precision_score(
                y_validation,
                validation_probabilities
            )

            print(
                "Validation PR-AUC:",
                round(validation_pr_auc, 5)
            )

            # ------------------------------------------------
            # THRESHOLD ANALYSIS
            # ------------------------------------------------

            for threshold in THRESHOLDS:

                predictions = (
                    validation_probabilities >= threshold
                ).astype(int)

                tn, fp, fn, tp = confusion_matrix(
                    y_validation,
                    predictions,
                    labels=[0, 1]
                ).ravel()

                precision = precision_score(
                    y_validation,
                    predictions,
                    zero_division=0
                )

                recall = recall_score(
                    y_validation,
                    predictions,
                    zero_division=0
                )

                f1 = f1_score(
                    y_validation,
                    predictions,
                    zero_division=0
                )

                # --------------------------------------------
                # COST CALCULATION
                # --------------------------------------------

                for fn_cost in FN_COST_SCENARIOS:

                    total_cost = (
                        fp * FP_COST
                        + fn * fn_cost
                    )

                    all_results.append({

                        "seed": seed,

                        "model": model_name,

                        "threshold": round(float(threshold), 2),

                        "validation_pr_auc": validation_pr_auc,

                        "precision": precision,

                        "recall": recall,

                        "f1": f1,

                        "true_positives": int(tp),

                        "false_positives": int(fp),

                        "true_negatives": int(tn),

                        "false_negatives": int(fn),

                        "false_positive_cost": FP_COST,

                        "false_negative_cost": fn_cost,

                        "total_cost": total_cost

                    })

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
    # AGGREGATE RESULTS ACROSS SEEDS
    # ========================================================

    group_columns = [
        "model",
        "threshold",
        "false_negative_cost"
    ]

    metric_columns = [
        "validation_pr_auc",
        "precision",
        "recall",
        "f1",
        "false_positives",
        "false_negatives",
        "total_cost"
    ]

    summary = results_df.groupby(
        group_columns
    )[metric_columns].agg(["mean", "std"])

    summary.to_csv(SUMMARY_PATH)

    # ========================================================
    # BEST THRESHOLD PER COST SCENARIO
    # ========================================================

    print("\n" + "=" * 75)
    print("COST-SENSITIVE MODEL COMPARISON")
    print("=" * 75)

    mean_results = results_df.groupby(
        group_columns
    )[metric_columns].mean().reset_index()

    for fn_cost in FN_COST_SCENARIOS:

        print("\n" + "-" * 75)
        print("FALSE NEGATIVE COST:", fn_cost)
        print("FALSE POSITIVE COST:", FP_COST)
        print("-" * 75)

        scenario = mean_results[
            mean_results["false_negative_cost"] == fn_cost
        ]

        for model_name in scenario["model"].unique():

            model_results = scenario[
                scenario["model"] == model_name
            ]

            best_row = model_results.loc[
                model_results["total_cost"].idxmin()
            ]

            print("\nModel:", model_name)

            print(
                "Best threshold:",
                round(best_row["threshold"], 2)
            )

            print(
                "Mean validation cost:",
                round(best_row["total_cost"], 2)
            )

            print(
                "Mean precision:",
                round(best_row["precision"], 4)
            )

            print(
                "Mean recall:",
                round(best_row["recall"], 4)
            )

            print(
                "Mean F1:",
                round(best_row["f1"], 4)
            )

            print(
                "Mean false positives:",
                round(best_row["false_positives"], 2)
            )

            print(
                "Mean false negatives:",
                round(best_row["false_negatives"], 2)
            )

    # ========================================================
    # SAVE SUMMARY AND FINAL INFORMATION
    # ========================================================

    print("\n" + "=" * 75)
    print("EXPERIMENT COMPLETED")
    print("=" * 75)

    print("\nIndividual results:")
    print(RESULT_PATH)

    print("\nAggregated summary:")
    print(SUMMARY_PATH)

    print("\nIMPORTANT NOTES")

    print("- Thresholds were evaluated on validation data.")

    print("- Reserved test sets were not used.")

    print("- Cost values are illustrative, not real banking costs.")

    print("- Lower estimated cost is better under the chosen assumptions.")

    print("- The dataset contains synthetic fraud labels.")

    print("- No production model was modified.")


if __name__ == "__main__":
    main()
