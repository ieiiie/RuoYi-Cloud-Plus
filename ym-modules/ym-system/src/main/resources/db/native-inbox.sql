-- Execute in the current ym-system business database. Existing messages are untouched.
CREATE TABLE IF NOT EXISTS sys_inbox_delivery (
 delivery_id varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 tenant_id varchar(20) NOT NULL,
 payload_hash varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 message_id bigint NOT NULL,
 received_at datetime(3) NOT NULL,
 PRIMARY KEY (delivery_id),
 UNIQUE KEY uk_inbox_message_id (message_id)
) ENGINE=InnoDB COMMENT='Native notification receipt and inbox deduplication';
