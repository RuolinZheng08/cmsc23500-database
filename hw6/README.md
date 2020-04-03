# DB DDL, Indexes, and Bulk Loading

In this homework you are creating a database schema, bulk loading data, and creating indexes for the data.
The data for this HW is pothole data from the City of Chicago (note this is a trimmed version of the dataset).

You will be reading and writing from a private PostgreSQL database hosted on a server **only accessible from a campus network**.
Please note that you will need to use your CNET ID and a password emailed to you to access the DB.

To test your database you can use psql, a client application to connect to a PostgreSQL server. 
Psql is installed on csil machines, which you can connect to via `ssh linux.cs.uchicago.edu`.
See earlier SQL homework instructions on how to use psql.
You should simply be able to
run `psql` and you will be connected to a database that has the same name as your ucnetid.
You can run sql in psql by creating a file (eg myquery.sql) with the SQL statement and then
passing -f to psql (`psql -f myquery.sql`).

# DB DDL, Queries, Index, and JDBC

In this homework you are completing the DatabaseWrapper class to complete a series of functions related to writing and reading
data about potholes from the City of Chicago (note this is a trimmed version of the dataset).



You will be reading and writing from a private database. 
You will need to add your (random) password to the DatabaseConstants.java file. Please note that normally you should never commit
a password and add to a git repository. Since this is a random password, gitlab is private, and the machines are only accessible on 
campus, we view this as OK.


You will be using JDBC to connect and interact with the database.
See
 - http://www.postgresqltutorial.com/postgresql-jdbc/
 - https://jdbc.postgresql.org/documentation/head/connect.html
 - https://www.tutorialspoint.com/jdbc/jdbc-create-tables.htm

You will need to accomplish a few things to get 100% on this homework. Note that for this homework 
you will need to modify DatabaseWrapper.java and possibly DatabaseConstants.java.  You should not modify Driver or Predicate.
For you code to connect to the database you will need to be runinng the code directly on the VM we provided you. 
*Note that the VM has your CSIL directory mounted, so you can make changes on the remote file system directly*

### Dataset
Get the dataset from
```
http://people.cs.uchicago.edu/~aelmore/class/311_Service_Requests_-_Pot_Holes_Reported.tsv
```
You can easily download this on the vm using wget:
```
wget http://people.cs.uchicago.edu/~aelmore/class/311_Service_Requests_-_Pot_Holes_Reported.tsv
```
## Important note on Java
This HW assumes you are using Java 8+. If you are using an older version of Java you will need to change your psql jdbc jar. Download the right jar at:
https://jdbc.postgresql.org/download.html and update your ant and eclipse files to use the new jar (or cheat and keep the same name of lib/postgresql-42.2.1.jar)

### Your Database
You should have a database on server that is emailed to you. that has the same name as your cnet ID. You should be able to connect to the database using your
cnet ID and password. For example, if *your* database host is cs235-6, you can test via psql -h cs235-6.cs.uchicago.edu -U <yourCNETID>  <yourCNETID>

## CREATE and DROP a Table (5%)

You will need to invoke the SQL commands for createPotholeTable and dropPotholeTable functions. You can
read the file or copy the SQL commands into your code.

## Open and close connection to database (5%)

Implement the functions openConnection and closeConnection.  Neither function should throw an exception.
The openConnection will return false if a connection cannot be opened.
Use the conn object to store the open connection.

## Insert records into the database (30%)
Implement loadPotholeRecord that takes a line from the TSV file as a string. If a record cannot be inserted, return false. You might need to try to optimize the loading some -- think about your use of transactions and batching. For full credit you will need to support batching of records via 
transactions and use prepared statements (see query database). For partial credit, only support a batch size of 1 via autocommit as true. 
Please read [the Java Docs on JDBC transactions](https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html)

Note there is a finalizeLoading function in the DatabaseWrapper that the driver will call after
all the load records 

## Add Indexes (10%)
Create at least two indexes that you expect should benefit some of the queries outlined below. You may want to experiment and validate that your index selection is good for the given queries.

## Query database (30%)
Implement getServiceRequestNumbers to query the database based on two predicates.
For full credit you need to use PreparedStatements for the queries.
Additionally, you should create the prepared statements once and reuse them.
A preperared statement is parameterized query.
[Read on Prepared Statements](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html)
[More on Prepeared Statements and Reuse](http://tutorials.jenkov.com/jdbc/preparedstatement.html)

The universe of possible predicates is bound to:
 - p1 ZIP =
 - p1 LAT > , p2 LAT <
 - p1 LONG >, p2 LONG <
 - p1 ZIP =, p2 NUM POTHOLES >
 - p1 MOST RECENT ACTION =  

 Note the second predicate may be null.

## Experiment and Analyze (20%)
For full credit here test your code with different batch sizes to measure the impact of transactions on data loading, and test the impact of your indexes.  You do not need a fine grained test (eg only test orders of magnitude on batchsize) and you do not need an exhaustive index search.  

## Running the code
You can either run the Driver program through Eclipse (or your favorite IDE), or use ant.
To build: `ant build`
To run: `ant Driver`
The task ant Driver takes three optional parameters id, batch, and limit.  Batch is passed along to the 
constructor of DatabaseWrapper to indicate how many inserts should be grouped together for a batch. 
Limit is for testing that tells the Driver how many records we will load. Id is for setting your cnetID without having to modify the DatatbaseConstants file.  Your final submission
should run with no limit set.  

For example, to run the hw with a batch size of 10 and to limit to the first 100 records we would run:
```
ant Driver -Dbatch=10 -Dlimit=100
```


### Eclipse
Project files for eclipse already exist. You can run Import - Existing Project - Navigate to the hw directory and hit ok.

## To submit
Please add a very short write up in mywriteup.txt that includes what indexes you added and the output from your final solution (just what the code asks you to add/copy). You should provide a very short analysis or 
description of batchsizing and index selection. 

### Exceptions
 You can modify the DBWrapper to throw an exception if you need (outside of closeConnection). The Driver has a generic exception handler. Note that you should properly handle any open transactions before an exception is thrown.


### Asking Questions
*Many* problems for this homework will be solved with a quick Google/stack overflow /stack exchange search. If the problem is not postgresql related, when you post a question please list at least one search term that you looked for the answer using.
