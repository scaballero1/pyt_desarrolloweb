import os
from typing import Generator

from sqlmodel import Session, SQLModel, create_engine

# En Render la variable DATABASE_URL ya viene configurada.
# En tu PC cámbiala aquí o define la variable de entorno DATABASE_URL.
# Formato: postgresql://USUARIO:CLAVE@HOST:5432/NOMBRE_BD
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://postgres:12345@127.0.0.1:5432/polirestaurante",
)

# Render entrega "postgres://", pero SQLAlchemy necesita "postgresql://"
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(DATABASE_URL, echo=True, pool_pre_ping=True)


def create_db_and_tables():
    SQLModel.metadata.create_all(engine)


def get_session() -> Generator[Session, None, None]:
    with Session(engine) as session:
        yield session
