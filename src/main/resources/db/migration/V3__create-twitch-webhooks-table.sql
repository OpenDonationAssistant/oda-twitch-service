create table twitch_webhooks (
  recipient_id varchar(255) primary key,
  twitch_id varchar(255) not null,
  subscription_ids text[] default '{}'
);
