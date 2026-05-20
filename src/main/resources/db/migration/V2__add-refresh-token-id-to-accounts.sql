alter table accounts
  add column if not exists refresh_token_id varchar(255);

