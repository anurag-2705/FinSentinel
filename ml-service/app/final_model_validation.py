
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
# PATH CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "final_model_validation.csv"


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
# MODEL DEFINITIONS
# ============================================================

def create_models():

    random_forest = RandomForestClassifier(
        n_estimators=500,
        max_depth=10,
        min_samples_split=20,
        min_samples_leaf=8,
        class_weight="balanced_subsample",
        max_features="sqrt",
        random_state=42,
        n_jobs=-1
    )

    histogram_gradient_boosting = CalibratedClassifierCV(
        estimator=HistGradientBoostingClassifier(
            max_iter=200,
            learning_rate=0.08,
            max_leaf_nodes=15,
            l2_regularization=1.0,
            random_state=42
        ),
        method="sigmoid",
        cv=5
    )

    return {
        "RandomForest_Baseline": random_forest,
        "HistGradientBoosting_Sigmoid": histogram_gradient_boosting
    }


# ============================================================
# THRESHOLD SELECTION
# ============================================================

def select_threshold(y_true, probabilities):

    thresholds = np.arange(0.01, 0.501, 0.01)

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
            best_threshold = float(threshold)

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

    results = {

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

    return results


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)

    print("FINSENTINEL - FINAL MODEL VALIDATION")

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

    # --------------------------------------------------------
    # 70% TRAINING / 30% TEMPORARY
    # --------------------------------------------------------

    X_train, X_temp, y_train, y_temp = train_test_split(
        X,
        y,
        test_size=0.30,
        random_state=42,
        stratify=y
    )

    # --------------------------------------------------------
    # 15% VALIDATION / 15% TEST
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
    # TRAIN MODELS
    # --------------------------------------------------------

    models = create_models()

    all_results = []

    for model_name, model in models.items():

        print("\n" + "=" * 75)

        print("TRAINING:", model_name)

        print("=" * 75)

        model.fit(
            X_train,
            y_train
        )

        # ----------------------------------------------------
        # VALIDATION PROBABILITIES
        # ----------------------------------------------------

        validation_probabilities = model.predict_proba(
            X_validation
        )[:, 1]

        validation_pr_auc = average_precision_score(
            y_validation,
            validation_probabilities
        )

        # ----------------------------------------------------
        # SELECT THRESHOLD ON VALIDATION ONLY
        # ----------------------------------------------------

        best_threshold, validation_f1 = select_threshold(
            y_validation,
            validation_probabilities
        )

        print("\nVALIDATION RESULTS")

        print("Validation PR-AUC:", round(validation_pr_auc, 5))

        print("Selected threshold:", round(best_threshold, 2))

        print("Validation F1:", round(validation_f1, 5))

        # ----------------------------------------------------
        # FINAL TEST EVALUATION
        # ----------------------------------------------------

        print("\nTEST RESULTS")

        result = evaluate_model(
            model_name,
            model,
            best_threshold,
            X_test,
            y_test
        )

        all_results.append(result)

        for metric, value in result.items():

            if metric != "model":

                print(f"{metric}: {value}")

    # --------------------------------------------------------
    # SAVE RESULTS
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
    # COMPARISON
    # --------------------------------------------------------

    print("\n" + "=" * 75)

    print("FINAL MODEL COMPARISON")

    print("=" * 75)

    print(
        results_df.round(5).to_string(index=False)
    )

    # --------------------------------------------------------
    # BEST MODEL BY TEST PR-AUC
    # --------------------------------------------------------

    best_model = results_df.loc[
        results_df["pr_auc"].idxmax()
    ]

    print("\nBEST TEST PR-AUC MODEL:")

    print(best_model["model"])

    print("PR-AUC:", round(best_model["pr_auc"], 5))

    print("Recall:", round(best_model["recall"], 5))

    print("Precision:", round(best_model["precision"], 5))

    print("F1:", round(best_model["f1"], 5))

    print("\nResults saved to:")

    print(RESULT_PATH)

    # --------------------------------------------------------
    # IMPORTANT NOTES
    # --------------------------------------------------------

    print("\nIMPORTANT NOTES")

    print("- Thresholds were selected using validation data only.")

    print("- Test data was not used for threshold selection.")

    print("- Both models use identical data splits.")

    print("- Calibration was fitted using training data only.")

    print("- Test results are preliminary and based on one split.")

    print("- Dataset contains synthetic fraud labels.")

    print("- No production model was modified.")


if __name__ == "__main__":

    main()
