
import pandas as pd

from pathlib import Path

from sklearn.model_selection import train_test_split
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.inspection import permutation_importance
from sklearn.metrics import average_precision_score


# ============================================================
# PATH CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_PATH = BASE_DIR / "data" / "synthetic_transactions.csv"

RESULT_PATH = BASE_DIR / "models" / "feature_importance.csv"


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
# MAIN
# ============================================================

def main():

    print("\n" + "=" * 75)
    print("FINSENTINEL - FEATURE IMPORTANCE ANALYSIS")
    print("=" * 75)

    # --------------------------------------------------------
    # LOAD DATASET
    # --------------------------------------------------------

    if not DATA_PATH.exists():
        raise FileNotFoundError(DATA_PATH)

    dataset = pd.read_csv(DATA_PATH)

    X = dataset[FEATURES]
    y = dataset[TARGET]

    # --------------------------------------------------------
    # RECREATE SAME DATA SPLIT
    # --------------------------------------------------------

    X_train, X_temp, y_train, y_temp = train_test_split(
        X,
        y,
        test_size=0.30,
        random_state=42,
        stratify=y
    )

    X_validation, X_test, y_validation, y_test = train_test_split(
        X_temp,
        y_temp,
        test_size=0.50,
        random_state=42,
        stratify=y_temp
    )

    print("\nDATA INFORMATION")

    print("Training samples:", len(X_train))
    print("Validation samples:", len(X_validation))
    print("Test samples:", len(X_test))

    # --------------------------------------------------------
    # TRAIN HISTGRADIENTBOOSTING
    # --------------------------------------------------------

    model = HistGradientBoostingClassifier(
        max_iter=200,
        learning_rate=0.08,
        max_leaf_nodes=15,
        l2_regularization=1.0,
        random_state=42
    )

    print("\nTraining HistGradientBoosting...")

    model.fit(X_train, y_train)

    # --------------------------------------------------------
    # BASELINE VALIDATION PR-AUC
    # --------------------------------------------------------

    validation_probabilities = model.predict_proba(
        X_validation
    )[:, 1]

    baseline_pr_auc = average_precision_score(
        y_validation,
        validation_probabilities
    )

    print("\nBaseline Validation PR-AUC:",
          round(baseline_pr_auc, 5))

    # --------------------------------------------------------
    # PERMUTATION IMPORTANCE
    # --------------------------------------------------------

    print("\nCalculating permutation importance...")
    print("This may take a few minutes.")

    importance = permutation_importance(
        model,
        X_validation,
        y_validation,
        scoring="average_precision",
        n_repeats=10,
        random_state=42,
        n_jobs=-1
    )

    # --------------------------------------------------------
    # CREATE RESULTS
    # --------------------------------------------------------

    results = pd.DataFrame({

        "feature": FEATURES,

        "importance_mean": importance.importances_mean,

        "importance_std": importance.importances_std

    })

    results = results.sort_values(
        by="importance_mean",
        ascending=False
    )

    # --------------------------------------------------------
    # SAVE RESULTS
    # --------------------------------------------------------

    RESULT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    results.to_csv(
        RESULT_PATH,
        index=False
    )

    # --------------------------------------------------------
    # DISPLAY RESULTS
    # --------------------------------------------------------

    print("\n" + "=" * 75)

    print("PERMUTATION FEATURE IMPORTANCE")

    print("=" * 75)

    print(
        results.round(6).to_string(index=False)
    )

    # --------------------------------------------------------
    # INTERPRETATION
    # --------------------------------------------------------

    print("\nFEATURE INTERPRETATION")

    for _, row in results.iterrows():

        feature = row["feature"]

        score = row["importance_mean"]

        if score > 0.001:

            status = "USEFUL SIGNAL"

        elif score < -0.001:

            status = "POSSIBLE NOISE"

        else:

            status = "LOW IMPORTANCE"

        print(
            f"{feature}: {score:.6f} -> {status}"
        )

    print("\nResults saved to:")

    print(RESULT_PATH)

    print("\nIMPORTANT:")

    print(
        "Permutation importance measures predictive usefulness "
        "on this validation dataset."
    )

    print(
        "Correlated features can share or hide importance."
    )

    print(
        "Negative importance does not automatically mean "
        "a feature should be removed."
    )

    print(
        "The test set remains untouched."
    )

    print(
        "Results are based on synthetic transaction data."
    )


if __name__ == "__main__":

    main()
