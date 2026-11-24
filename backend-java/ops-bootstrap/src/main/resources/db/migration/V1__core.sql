CREATE TABLE tenant (
    id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY
);
CREATE TABLE account (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    username VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL,
    PRIMARY KEY (tenant_id, username),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);
CREATE TABLE service_asset (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(160) NOT NULL,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);
CREATE TABLE incident (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    service_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, service_id) REFERENCES service_asset(tenant_id, id)
);
CREATE TABLE diagnosis_task (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    incident_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    incident_version BIGINT NOT NULL,
    idempotency_key VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    trace_id CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL,
    run_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    attempt INT NOT NULL DEFAULT 0,
    lease_token CHAR(36),
    lease_until DATETIME(6),
    next_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deadline_at DATETIME(6) NOT NULL,
    result_json JSON,
    error_code VARCHAR(48),
    PRIMARY KEY (tenant_id, id),
    UNIQUE KEY task_idempotency (tenant_id, incident_id, idempotency_key),
    KEY task_schedule (status, next_at, lease_until),
    FOREIGN KEY (tenant_id, incident_id) REFERENCES incident(tenant_id, id)
);
CREATE TABLE audit_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    resource_type VARCHAR(16) NOT NULL,
    resource_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor VARCHAR(48) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    detail VARCHAR(250) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY audit_resource (tenant_id, resource_type, resource_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);
