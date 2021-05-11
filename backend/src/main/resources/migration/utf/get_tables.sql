select col.table_schema as database_name,
       col.table_name,
       col.column_name,
       col.data_type,
       col.character_maximum_length as maximum_length
from information_schema.columns col
join information_schema.tables tab on tab.table_schema = col.table_schema
                                   and tab.table_name = col.table_name
                                   and tab.table_type = 'BASE TABLE'
where col.data_type in ('char', 'varchar',
                        'text', 'longtext')
      and col.table_schema not in ('information_schema', 'sys',
                                   'performance_schema', 'mysql')
      and col.table_name not in ('DATABASECHANGELOG', 'DATABASECHANGELOGLOCK', 'database_change_log', 'database_change_log_lock')
    and col.column_name not in ('key')
    and col.column_name not like '%id'
    and col.column_name not like '%hash'
     -- and col.table_schema = 'database_name' -- put your database name here
order by col.table_schema,
         col.table_name,
         col.ordinal_position;
