import numpy as np
import pandas as pd
from pathlib import Path


def generate_dataset(rows=20000, seed=42):
    rng = np.random.default_rng(seed)

    amount = np.round(
        rng.lognormal(mean=7.5, sigma=1.3, size=rows), 2
    )

    transaction_hour = rng.integers(0, 24, rows)
    is_weekend = rng.binomial(1, 0.28, rows)

    transactions_last_1h = rng.poisson(1.5, rows)
    failed_txns_24h = rng.poisson(0.25, rows)

    new_device = rng.binomial(1, 0.12, rows)
    new_payee = rng.binomial(1, 0.15, rows)

    geo_distance_km = np.round(
        rng.exponential(scale=80, size=rows), 2
    )

    account_age_days = rng.integers(1, 3000, rows)
    is_international = rng.binomial(1, 0.06, rows)

    average_amount_30d = np.round(
        rng.lognormal(mean=7.0, sigma=1.0, size=rows), 2
    )

    amount_to_average_ratio = np.round(
        amount / np.maximum(average_amount_30d, 1), 3
    )

    unusual_hour = (
        (transaction_hour >= 0) & (transaction_hour <= 5)
    ).astype(int)

    logit = (
        -5.2
        + 0.000025 * amount
        + 0.55 * np.maximum(transactions_last_1h - 3, 0)
        + 0.9 * failed_txns_24h
        + 1.0 * new_device
        + 0.9 * new_payee
        + 0.002 * geo_distance_km
        + 1.1 * is_international
        + 0.5 * (amount_to_average_ratio > 3)
        + 0.5 * unusual_hour
        + 0.4 * (account_age_days < 30)
    )

    fraud_probability = 1 / (1 + np.exp(-logit))

    is_fraud = rng.binomial(1, fraud_probability)

    dataset = pd.DataFrame({
        "amount": amount,
        "transaction_hour": transaction_hour,
        "is_weekend": is_weekend,
        "transactions_last_1h": transactions_last_1h,
        "failed_txns_24h": failed_txns_24h,
        "new_device": new_device,
        "new_payee": new_payee,
        "geo_distance_km": geo_distance_km,
        "account_age_days": account_age_days,
        "is_international": is_international,
        "average_amount_30d": average_amount_30d,
        "amount_to_average_ratio": amount_to_average_ratio,
        "is_fraud": is_fraud
    })

    return dataset


if __name__ == "__main__":
    dataset = generate_dataset()

    output_path = (
        Path(__file__).resolve().parents[1]
        / "data"
        / "synthetic_transactions.csv"
    )

    output_path.parent.mkdir(parents=True, exist_ok=True)
    dataset.to_csv(output_path, index=False)

    print("Synthetic dataset generated successfully.")
    print(f"Total transactions: {len(dataset)}")
    print(f"Fraudulent transactions: {dataset['is_fraud'].sum()}")
    print(
        f"Fraud percentage: "
        f"{dataset['is_fraud'].mean() * 100:.2f}%"
    )
    print(f"Saved to: {output_path}")
