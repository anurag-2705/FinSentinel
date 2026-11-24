
import pandas as pd

from pathlib import Path

from sklearn.model_selection import train_test_split
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.calibration import CalibratedClassifierCV, calibration_curve

from sklearn.metrics import (
    average_precision_score,
    roc_auc_score,
    brier_score_loss,
    log_loss
)


# ============================================================
# PATH CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "calibration_results.csv"


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

def create_model():

    return HistGradientBoostingClassifier(
        max_iter=200,
        learning_rate=0.08,
        max_leaf_nodes=15,
        l2_regularization=1.0,
        random_state=42
    )


# ============================================================
# EVALUATION
# ============================================================

def evaluate_model(name, model, X_test, y_test):

    probabilities = model.predict_proba(X_test)[:, 1]

    results = {

        "model": name,

        "pr_auc": average_precision_score(
            y_test,
            probabilities
        ),

        "roc_auc": roc_auc_score(
            y_test,
            probabilities
        ),

        "brier_score": brier_score_loss(
            y_test,
            probabilities
        ),

        "log_loss": log_loss(
            y_test,
            probabilities
        ),

        "mean_predicted_probability": probabilities.mean(),

        "actual_fraud_rate": y_test.mean(),

        "maximum_probability": probabilities.max(),

        "minimum_probability": probabilities.min()

    }

    return results, probabilities


# ============================================================
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)
    print("FINSENTINEL - PROBABILITY CALIBRATION EXPERIMENT")
    print("=" * 75)

    # --------------------------------------------------------
    # LOAD DATA
    # --------------------------------------------------------

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    print("\nDATASET INFORMATION")

    print("Total transactions:", len(dataset))
    print("Fraud transactions:", int(y.sum()))
    print("Legitimate transactions:", int((y == 0).sum()))

    # --------------------------------------------------------
    # TRAIN / TEST SPLIT
    # --------------------------------------------------------

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.30,
        random_state=42,
        stratify=y
    )

    print("\nDATA SPLIT")

    print("Training samples:", len(X_train))
    print("Testing samples:", len(X_test))

    print("Training fraud:", int(y_train.sum()))
    print("Testing fraud:", int(y_test.sum()))

    # --------------------------------------------------------
    # ORIGINAL MODEL
    # --------------------------------------------------------

    print("\nTraining original HistGradientBoosting...")

    original_model = create_model()

    original_model.fit(X_train, y_train)

    # --------------------------------------------------------
    # SIGMOID CALIBRATION
    # --------------------------------------------------------

    print("\nTraining sigmoid-calibrated model...")

    sigmoid_model = CalibratedClassifierCV(
        estimator=create_model(),
        method="sigmoid",
        cv=5
    )

    sigmoid_model.fit(X_train, y_train)

    # --------------------------------------------------------
    # ISOTONIC CALIBRATION
    # --------------------------------------------------------

    print("\nTraining isotonic-calibrated model...")

    isotonic_model = CalibratedClassifierCV(
        estimator=create_model(),
        method="isotonic",
        cv=5
    )

    isotonic_model.fit(X_train, y_train)

    # --------------------------------------------------------
    # EVALUATE ALL MODELS
    # --------------------------------------------------------

    models = {

        "Original_HistGradientBoosting": original_model,

        "Sigmoid_Calibrated": sigmoid_model,

        "Isotonic_Calibrated": isotonic_model

    }

    all_results = []

    probability_predictions = {}

    print("\n" + "=" * 75)

    print("MODEL EVALUATION ON UNTOUCHED TEST DATA")

    print("=" * 75)

    for name, model in models.items():

        results, probabilities = evaluate_model(
            name,
            model,
            X_test,
            y_test
        )

        all_results.append(results)

        probability_predictions[name] = probabilities

        print("\nMODEL:", name)

        for metric, value in results.items():

            if metric != "model":

                print(f"{metric}: {value:.6f}")

    # --------------------------------------------------------
    # CALIBRATION CURVE
    # --------------------------------------------------------

    print("\n" + "=" * 75)

    print("CALIBRATION CURVE ANALYSIS")

    print("=" * 75)

    for name, probabilities in probability_predictions.items():

        actual_fraction, predicted_fraction = calibration_curve(
            y_test,
            probabilities,
            n_bins=10,
            strategy="quantile"
        )

        print("\nMODEL:", name)

        for index, (predicted, actual) in enumerate(
            zip(predicted_fraction, actual_fraction),
            start=1
        ):

            print(
                f"Bin {index}: "
                f"Predicted={predicted:.5f}, "
                f"Actual={actual:.5f}"
            )

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
    # SUMMARY
    # --------------------------------------------------------

    print("\n" + "=" * 75)

    print("CALIBRATION COMPARISON")

    print("=" * 75)

    print(
        results_df.round(5).to_string(index=False)
    )

    print("\nResults saved to:")

    print(RESULT_PATH)

    # --------------------------------------------------------
    # IMPORTANT NOTES
    # --------------------------------------------------------

    print("\nIMPORTANT NOTES")

    print("- All models use the same training and test split.")

    print("- Calibration uses cross-validation on training data.")

    print("- Test data was not used for calibration.")

    print("- Lower Brier score is generally better.")

    print("- Lower log loss is generally better.")

    print("- Higher PR-AUC is generally better.")

    print("- Calibration does not guarantee better fraud detection.")

    print("- Dataset contains synthetic fraud labels.")

    print("- No production model was modified.")


if __name__ == "__main__":

    main()
