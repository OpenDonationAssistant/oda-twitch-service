ALTER TABLE twitch_webhooks DROP CONSTRAINT twitch_webhooks_pkey;
ALTER TABLE twitch_webhooks ADD COLUMN id UUID PRIMARY KEY;
ALTER TABLE twitch_webhooks ADD COLUMN refresh_token_id UUID;
