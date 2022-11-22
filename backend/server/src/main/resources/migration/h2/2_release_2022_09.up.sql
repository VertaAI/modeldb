
-- H2 2.1.214;
SET DB_CLOSE_DELAY -1;
CREATE MEMORY TABLE "TEST_2"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "CLIENT_KEY" CHARACTER LARGE OBJECT,
    "CLOUD_STORAGE_FILE_PATH" CHARACTER LARGE OBJECT,
    "CLOUD_STORAGE_KEY" CHARACTER LARGE OBJECT,
    "ENTITY_ID" CHARACTER VARYING(50),
    "ENTITY_NAME" CHARACTER VARYING(50)
);