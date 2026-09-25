from fastapi import FastAPI
from .routers import match

app = FastAPI(title="Football Analytics API", version="0.1.0")

# Register routers
app.include_router(match.router, prefix="/matches", tags=["matches"])

# Health check
@app.get("/health", tags=["system"])
def health_check():
    return {"status": "ok"}
