CREATE TABLE alert_source (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE alert_group (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    service_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    watermark_ms BIGINT NOT NULL DEFAULT 0,
    info_count BIGINT NOT NULL DEFAULT 0,
    warning_count BIGINT NOT NULL DEFAULT 0,
    critical_count BIGINT NOT NULL DEFAULT 0,
    suppressed_count BIGINT NOT NULL DEFAULT 0,
    next_candidate_ms BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, service_id) REFERENCES service_asset(tenant_id, id)
);

CREATE TABLE alert_receipt (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    format VARCHAR(16) NOT NULL,
    raw_payload MEDIUMBLOB NOT NULL,
    received_ms BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, source_id) REFERENCES alert_source(tenant_id, id)
);

CREATE TABLE alert_event (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_key VARCHAR(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    group_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_ms BIGINT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    counted BOOLEAN NOT NULL,
    late BOOLEAN NOT NULL,
    PRIMARY KEY (tenant_id, id),
    UNIQUE KEY alert_deduplication (tenant_id, source_id, event_key),
    KEY window_eviction (tenant_id, group_id, counted, occurred_ms, severity),
    FOREIGN KEY (tenant_id, source_id) REFERENCES alert_source(tenant_id, id),
    FOREIGN KEY (tenant_id, group_id) REFERENCES alert_group(tenant_id, id)
);

CREATE TABLE alert_receipt_event (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    receipt_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ordinal INT NOT NULL,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    duplicate_event BOOLEAN NOT NULL,
    PRIMARY KEY (tenant_id, receipt_id, ordinal),
    FOREIGN KEY (tenant_id, receipt_id) REFERENCES alert_receipt(tenant_id, id),
    FOREIGN KEY (tenant_id, event_id) REFERENCES alert_event(tenant_id, id)
);

CREATE TABLE incident_candidate (
    tenant_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    group_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(24) NOT NULL,
    watermark_ms BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, id),
    KEY candidate_group (tenant_id, group_id, watermark_ms),
    FOREIGN KEY (tenant_id, group_id) REFERENCES alert_group(tenant_id, id),
    FOREIGN KEY (tenant_id, event_id) REFERENCES alert_event(tenant_id, id)
);
