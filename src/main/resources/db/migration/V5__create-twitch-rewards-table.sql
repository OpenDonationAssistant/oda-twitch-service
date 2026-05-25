create table twitch_rewards (
  id UUID primary key,
  recipient_id varchar(255) not null,
  refresh_token_id UUID not null,
  type varchar(255) not null,
  cost integer not null
);
