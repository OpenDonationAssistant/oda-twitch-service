create table reward (
  id VARCHAR(50) primary key,
  recipient_id UUID not null,
  refresh_token_id UUID not null,
  type varchar(20) not null
);
