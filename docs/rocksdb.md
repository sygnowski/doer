### Working with RocksDB

Example of Key-Value operations:
```bash
# put to a default column family
doer rocks -db ./my-db -a put -k KeyName -v SomeValue

# get key value
doer rocks -db ./my-db -a get -k KeyName

# add new a column family and put key-value pair into column 
doer rocks -db ./my-db -a put -cf column-name -k KeyName -v SomeValue

# listing column family key-value pairs
doer rocks -db ./my-db -cf column-name -a get
```