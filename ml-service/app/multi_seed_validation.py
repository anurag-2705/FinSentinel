
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
# PATH CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "multi_seed_results.csv"

SUMMARY_PATH = BASE_DIR / "models" / "multi_seed_summary.csv"


# ============================================================
# CONFIGURATION
# ============================================================

RANDOM_SEEDS = [21, 42, 100, 123, 2026]

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

def select_threshold(y_true, probabilities):

    thresholds = [
        round(value / 100, 2)
        for value in range(1, 51)
    ]

    best_threshold = 0.50
    best_f1 = -1

    for threshold in thresholds:

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
            best_threshold = threshold

    return best_threshold, best_f1


# ============================================================
# MODEL EVALUATION
# ============================================================

def evaluate_model(
    model_name,
    model,
    threshold,
    X_test,
    y_test
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

    return {

        "model": model_name,

        "threshold": threshold,

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

        "false_negatives": int(fn)

    }


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)

    print("FINSENTINEL - MULTI-SEED MODEL VALIDATION")

    print("=" * 75)

    # --------------------------------------------------------
    # LOAD DATA
    # --------------------------------------------------------

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET INFORMATION")

    print("Total samples:", len(dataset))
    print("Total fraud:", int(y.sum()))
    print("Fraud percentage:", round(y.mean() * 100, 2), "%")

    print("\nRANDOM SEEDS:", RANDOM_SEEDS)

    all_results = []

    # --------------------------------------------------------
    # REPEAT EXPERIMENT FOR EACH SEED
    # --------------------------------------------------------

    for seed in RANDOM_SEEDS:

        print("\n" + "=" * 75)

        print("RANDOM SEED:", seed)

        print("=" * 75)

        # ----------------------------------------------------
        # CREATE DATA SPLITS
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

        print("Training fraud:", int(y_train.sum()))
        print("Validation fraud:", int(y_validation.sum()))
        print("Testing fraud:", int(y_test.sum()))

        models = create_models(seed)

        # ----------------------------------------------------
        # TRAIN EACH MODEL
        # ----------------------------------------------------

        for model_name, model in models.items():

            print("\nTraining:", model_name)

            model.fit(
                X_train,
                y_train
            )

            # -----------------------------------------------
            # VALIDATION
            # -----------------------------------------------

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

            print("\nValidation PR-AUC:",
                  round(validation_pr_auc, 5))

            print("Selected threshold:", threshold)

            print("Validation F1:", round(validation_f1, 5))

            # -----------------------------------------------
            # TEST
            # -----------------------------------------------

            result = evaluate_model(
                model_name,
                model,
                threshold,
                X_test,
                y_test
            )

            result["seed"] = seed

            result["validation_pr_auc"] = validation_pr_auc

            result["validation_f1"] = validation_f1

            all_results.append(result)

            print("\nTest Results")

            for metric, value in result.items():

                if metric not in [
                    "model",
                    "seed",
                    "validation_pr_auc",
                    "validation_f1"
                ]:

                    print(f"{metric}: {value}")

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
    # SUMMARY STATISTICS
    # --------------------------------------------------------

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

    print("\n" + "=" * 75)

    print("MULTI-SEED VALIDATION SUMMARY")

    print("=" * 75)

    print(summary.round(4).to_string())

    # --------------------------------------------------------
    # SAVE SUMMARY
    # --------------------------------------------------------

    summary.to_csv(SUMMARY_PATH)

    # --------------------------------------------------------
    # MODEL COMPARISON
    # --------------------------------------------------------

    mean_pr_auc = results_df.groupby("model")[
        "pr_auc"
    ].mean()

    mean_f1 = results_df.groupby("model")[
        "f1"
    ].mean()

    print("\n" + "=" * 75)

    print("MODEL COMPARISON")

    print("=" * 75)

    for model_name in mean_pr_auc.index:

        print("\nModel:", model_name)

        print(
            "Mean PR-AUC:",
            round(mean_pr_auc[model_name], 5)
        )

        print(
            "Mean F1:",
            round(mean_f1[model_name], 5)
        )

    best_model = mean_pr_auc.idxmax()

    print("\nBEST MODEL BY MEAN TEST PR-AUC:")

    print(best_model)

    # --------------------------------------------------------
    # RESULTS LOCATIONS
    # --------------------------------------------------------

    print("\nINDIVIDUAL RESULTS:")

    print(RESULT_PATH)

    print("\nSUMMARY RESULTS:")

    print(SUMMARY_PATH)

    # --------------------------------------------------------
    # IMPORTANT NOTES
    # --------------------------------------------------------

    print("\nIMPORTANT NOTES")

    print("- Five different random seeds were evaluated.")

    print("- Thresholds were selected using validation data.")

    print("- Each test set remained untouched during its run.")

    print("- Models use identical splits within each seed.")

    print("- Results across seeds are not independent because")
    print("  the underlying dataset is reused.")

    print("- This is robustness analysis, not external validation.")

    print("- Dataset contains synthetic fraud labels.")

    print("- No production model was modified.")


if __name__ == "__main__":

    main()
