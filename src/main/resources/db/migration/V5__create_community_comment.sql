CREATE TABLE community_comment (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    author_nickname VARCHAR(30),
    author_profile_image_url TEXT,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_community_comment_post FOREIGN KEY (post_id)
        REFERENCES community_post (id) ON DELETE CASCADE,
    CONSTRAINT ck_community_comment_content CHECK (btrim(content) <> '')
);

CREATE INDEX idx_community_comment_post_active
    ON community_comment (post_id, created_at)
    WHERE deleted_at IS NULL;
