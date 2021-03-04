
DROP VIEW counter_group IF EXISTS;
DROP VIEW counter_sub_group IF EXISTS;

DROP TABLE counter_list IF EXISTS;
DROP TABLE quota_list IF EXISTS;
DROP TABLE subscriptions IF EXISTS;
DROP TABLE user_account IF EXISTS;
DROP TABLE user_groups IF EXISTS;

-- tables
-- Table: counter_list
CREATE TABLE counter_list (
    counter_id int NOT NULL,
    subscription_id int NOT NULL,
    user_id int NOT NULL,
    counter_value int NOT NULL,
    column_type int,
    insert_ts timestamp NOT NULL,
    CONSTRAINT counter_list_pk ASSUMEUNIQUE (counter_id)
);
PARTITION TABLE counter_list ON COLUMN user_id;

-- Table: quota_list
CREATE TABLE quota_list (
    quota_id int NOT NULL,
    subscription_id int NOT NULL,
    user_id int NOT NULL,
    quota_value DECIMAL(16,16) NOT NULL,
    quota_type int NOT NULL,
    insert_ts timestamp NOT NULL,
    CONSTRAINT quota_list_pk ASSUMEUNIQUE (quota_id)
);
PARTITION TABLE quota_list ON COLUMN user_id;

-- Table: subscriptions
CREATE TABLE subscriptions (
    subscription_id int NOT NULL,
    user_id int NOT NULL,
    start_date timestamp ,
    end_date timestamp ,
    date_subscribed timestamp,
    status varchar(32),
    insert_ts timestamp NOT NULL,
    CONSTRAINT subscriptions_pk ASSUMEUNIQUE (subscription_id)
);
PARTITION TABLE subscriptions ON COLUMN user_id;

-- Table: user_account
CREATE TABLE user_account (
    user_id int NOT NULL,
    user_name varchar(64),
    user_type int,
    user_info varchar(255),
    group_id int NOT NULL,
    insert_ts timestamp NOT NULL,
    CONSTRAINT user_account_ak_3 UNIQUE  (user_id, group_id),
    CONSTRAINT user_account_pk PRIMARY KEY (user_id)
);
PARTITION TABLE user_account ON COLUMN user_id;

-- Table: user_groups
CREATE TABLE user_groups (
    group_id int NOT NULL,
    group_name varchar(32),
    group_type varchar(32),
    members_min int,
    members_max int,
    insert_ts timestamp NOT NULL,
    CONSTRAINT insert_ts PRIMARY KEY (group_id)
);

-- Views
create view counter_group as select u.group_id, c.column_type, sum(c.counter_value) as total_counter, count(*)
  from user_account u, counter_list c
  where u.user_id = c.user_id
  group by u.group_id, c.column_type;

create view counter_sub_group as select u.group_id, c.subscription_id, c.column_type, sum(c.counter_value) as total_counter, count(*)
    from user_account u, counter_list c
    where u.user_id = c.user_id
    group by u.group_id, c.subscription_id, c.column_type;

-- Queries
select g.group_name, g.group_type, c.total_counter
   from user_groups g, counter_group c, user_account u
   where g.group_id = u.group_id and
   u.user_id = c.user_id

-- End of file.
