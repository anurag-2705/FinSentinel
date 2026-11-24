from pathlib import Path

import joblib
import pandas as pd

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_PATH = BASE_DIR / "models" / "fraud_model.joblib"

app = FastAPI(
    title="FinSentinel ML Fraud Detection API",
    description="Machine learning service for transaction risk assessment.",
    version="1.0.0"
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
        "model_loaded": model is not None
    }


@app.post("/predict")
def predict_fraud(transaction: TransactionRequest):

    try:
        transaction_data = transaction.model_dump()

        input_data = pd.DataFrame(
            [transaction_data],
            columns=FEATURES
        )

        fraud_probability = float(
            model.predict_proba(input_data)[0][1]
        )

        is_fraud = fraud_probability >= THRESHOLD

        if fraud_probability >= 0.70:
            risk_level = "HIGH"
        elif fraud_probability >= 0.30:
            risk_level = "MEDIUM"
        else:
            risk_level = "LOW"

        return {
            "success": True,
            "model": "RandomForestClassifier",
            "fraud_probability": round(
                fraud_probability, 4
            ),
            "fraud_percentage": round(
                fraud_probability * 100, 2
            ),
            "threshold": THRESHOLD,
            "is_fraud": is_fraud,
            "risk_level": risk_level,
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