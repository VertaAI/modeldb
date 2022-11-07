BEGIN /*!90000 PESSIMISTIC */;

create table if not exists `test_table`(
    `i` int not null primary key
);

insert into `test_table`(`i`) values (999);

COMMIT;