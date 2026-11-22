-- Apply once to the PostgreSQL database before enabling review likes.
-- One customer can like each review once.
CREATE TABLE IF NOT EXISTS productreviewlikes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_productreviewlikes_review
        FOREIGN KEY (review_id) REFERENCES productreviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_productreviewlikes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_productreviewlikes_review_user
        UNIQUE (review_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_productreviewlikes_review
    ON productreviewlikes(review_id);
