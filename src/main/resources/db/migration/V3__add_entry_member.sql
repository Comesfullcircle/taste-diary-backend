-- 작성자를 추정할 수 없는 기존 기록이 있으면 실패한다.
-- 그런 환경에서는 소유자 매핑을 결정한 후 마이그레이션해야 한다.
ALTER TABLE culture_entries
    ADD COLUMN member_id BIGINT NOT NULL REFERENCES members(id);

CREATE INDEX idx_culture_entries_member_date_id
    ON culture_entries (member_id, experienced_on DESC, id DESC);
