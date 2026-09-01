CREATE TABLE recommendations (
    recommendation_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    suggested_quantity INTEGER NOT NULL,
    order_deadline_date DATE NOT NULL,
    justification TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    generation_date DATE NOT NULL,
    feedback_comment TEXT,
    feedback_date DATE
);

CREATE INDEX idx_recommendations_store_status ON recommendations (store_id, status);
