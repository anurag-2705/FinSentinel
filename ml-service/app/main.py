from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import shap

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_PATH = BASE_DIR / "models" / "fraud_model.joblib"

app = FastAPI(
    title="FinSentinel ML Fraud Detection API",
    description="Machine learning and explainable AI for transaction risk assessment.",
    version="2.0.0"
)


# Load trained model
if not MODEL_PATH.exists():
    raise RuntimeError(
        f"Trained model not found: {MODEL_PATH}. "
        "Run train_model.py first."
    )

bundle = joblib.load(MODEL_PATH)

model = bundle["model"]
FEATURES = bundle["features"]
THRESHOLD = bundle.get("threshold", 0.5)

# Initialize SHAP TreeExplainer
explainer = shap.TreeExplainer(model)


class TransactionRequest(BaseModel):
    amount: float = Field(gt=0)
    transaction_hour: int = Field(ge=0, le=23)
    is_weekend: int = Field(ge=0, le=1)
    transactions_last_1h: int = Field(ge=0)
    failed_txns_24h: int = Field(ge=0)
    new_device: int = Field(ge=0, le=1)
    new_payee: int = Field(ge=0, le=1)
    geo_distance_km: float = Field(ge=0)
    account_age_days: int = Field(ge=0)
    is_international: int = Field(ge=0, le=1)
    average_amount_30d: float = Field(ge=0)
    amount_to_average_ratio: float = Field(ge=0)


FEATURE_EXPLANATIONS = {
    "amount": "Transaction amount",
    "transaction_hour": "Transaction time",
    "is_weekend": "Weekend transaction",
    "transactions_last_1h": "Transaction frequency",
    "failed_txns_24h": "Failed transactions",
    "new_device": "New device detected",
    "new_payee": "New beneficiary",
    "geo_distance_km": "Geographical distance",
    "account_age_days": "Account age",
    "is_international": "International transaction",
    "average_amount_30d": "Average transaction amount",
    "amount_to_average_ratio": "Amount compared with account average"
}


def get_risk_level(probability):

    if probability >= 0.70:
        return "HIGH"
    elif probability >= 0.30:
        return "MEDIUM"

    return "LOW"


def build_input(transaction):

    transaction_data = transaction.model_dump()

    return pd.DataFrame(
        [transaction_data],
        columns=FEATURES
    )


@app.get("/")
def root():

    return {
        "service": "FinSentinel ML Fraud Detection",
        "status": "running",
        "model": "RandomForestClassifier"
    }


@app.get("/health")
def health():

    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "shap_loaded": explainer is not None
    }


@app.post("/predict")
def predict_fraud(transaction: TransactionRequest):

    try:

        input_data = build_input(transaction)

        fraud_probability = float(
            model.predict_proba(input_data)[0][1]
        )

        is_fraud = fraud_probability >= THRESHOLD

        return {
            "success": True,
            "model": "RandomForestClassifier",
            "fraud_probability": round(fraud_probability, 4),
            "fraud_percentage": round(fraud_probability * 100, 2),
            "threshold": THRESHOLD,
            "is_fraud": is_fraud,
            "risk_level": get_risk_level(fraud_probability),
            "message": (
                "Transaction flagged for review"
                if is_fraud
                else "Transaction not flagged by ML model"
            ),
            "disclaimer": (
                "Experimental model trained on synthetic data. "
                "Not validated for real banking decisions."
            )
        }

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=f"Prediction failed: {str(error)}"
        )


@app.post("/explain")
def explain_fraud(transaction: TransactionRequest):

    try:

        input_data = build_input(transaction)

        # Get model prediction
        fraud_probability = float(
            model.predict_proba(input_data)[0][1]
        )

        # Calculate SHAP values
        shap_values = explainer.shap_values(input_data)

        # SHAP 0.54 returns:
        # samples x features x classes
        shap_array = np.asarray(shap_values)

        if shap_array.ndim == 3:
            contributions = shap_array[0, :, 1]

        elif shap_array.ndim == 2:
            contributions = shap_array[0]

        else:
            raise ValueError(
                f"Unexpected SHAP output shape: {shap_array.shape}"
            )

        # Build feature explanations
        feature_results = []

        for feature, value in zip(FEATURES, contributions):

            contribution = float(value)

            feature_results.append({
                "feature": feature,
                "label": FEATURE_EXPLANATIONS.get(
                    feature, feature
                ),
                "value": float(input_data.iloc[0][feature]),
                "shap_value": round(contribution, 6),
                "impact": (
                    "INCREASES_RISK"
                    if contribution > 0
                    else "DECREASES_RISK"
                    if contribution < 0
                    else "NEUTRAL"
                )
            })

        # Sort by absolute SHAP contribution
        feature_results.sort(
            key=lambda item: abs(item["shap_value"]),
            reverse=True
        )

        top_factors = feature_results[:5]

        increasing_factors = [
            item for item in feature_results
            if item["shap_value"] > 0
        ]

        decreasing_factors = [
            item for item in feature_results
            if item["shap_value"] < 0
        ]

        return {
            "success": True,
            "model": "RandomForestClassifier",
            "fraud_probability": round(fraud_probability, 4),
            "fraud_percentage": round(
                fraud_probability * 100, 2
            ),
            "risk_level": get_risk_level(fraud_probability),
            "threshold": THRESHOLD,
            "top_factors": top_factors,
            "risk_increasing_factors": increasing_factors[:5],
            "risk_decreasing_factors": decreasing_factors[:5],
            "disclaimer": (
                "SHAP explanations describe this experimental model's "
                "behavior. The model uses synthetic training data and "
                "is not validated for real banking decisions."
            )
        }

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=f"SHAP explanation failed: {str(error)}"
        )