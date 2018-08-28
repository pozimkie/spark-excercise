# Daily Github

Allegro Big Data recruitment task.

This solution written in Scala is using Spark 2.2 for data processing with com.databricks:spark-avro library for providing output files
 in AVRO format. Horizontal scalability is provided by Spark capabilities to compute on multinode cluster. 


## Building solution
```bash
mvn clean package
```
Tested on Apache Maven 3.3.9

## Downloading github data

Below script will download gzipped github events for whole specified month of a year.

```bash
cd src/main/resources
./get-events.sh <YYYY> <MM> /path/to/dest/dir
```
Example: ``./get-events.sh 2016 03 /tmp/ghdata``

Optionally downloaded data can be copied to hdfs storage: 
```bash
hadoop fs -copyFromLocal /path/to/dest/dir /dir/on/hdfs
```
## Submitting spark job

```bash
./bin/spark-submit --class org.poz.spark.DailyGithubApp --master [yarn|sparkURI]  --properties-file /tmp/dailygithub.properties --packages com.databricks:spark-avro_2.11:4.0.0 /path/to/daily-github-app-1.0-SNAPSHOT.jar
```

### Configuration properties
```properties
spark.ghevents.input=file:///tmp/ghdata
```
URI pointing to directory containing github monthly archive (json.gz files). Can be either located on local file system 
(file://) or hadoop file system (hdfs://)   
```properties
spark.ghevents.output=hdfs:///tmp/ghreport
```
URI pointing to directory where avro aggregates should be stored after processing. Can be either located on local file system 
(file://) or hadoop file system (hdfs://)
```properties
spark.ghevents.output.partitions=4
```   
Number of partitions for output DataFrame (actually also number of partial avro files)


Properties can be also submitted as arguments of spark-submit using --conf k=v parameter
    
## Output

Spark job creates two aggregrates:
``{spark.ghevents.output}/repository`` and ``{spark.ghevents.output}/user``
Both will containing relevant data with following schema (avro files contain JSON schema metadata):
```
repository
 |-- repo_id: long (nullable = true)
 |-- name: string (nullable = true)
 |-- date: date (nullable = true)
 |-- starred: long (nullable = false)
 |-- forks: long (nullable = false)
 |-- issues: long (nullable = false)
 |-- pull_requests: long (nullable = false)
```

```
user
 |-- user_id: long (nullable = true)
 |-- login: string (nullable = true)
 |-- date: date (nullable = true)
 |-- starred: long (nullable = false)
 |-- issues: long (nullable = false)
 |-- pull_requests: long (nullable = false)
```

## Production deployment
In production environment it is recommended to use multinode spark computing cluster. Command ``spark-submit`` should include then 
parameters for resource allocation like ```--driver-memory –num-executors  --executor-memory  --executor-cores``` tuned specifically to cluster capabilities and job priority.

Job can be also scheduled in Oozie for running in workflows. Instructions for spark job configuration can be found here: 
https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.1/bk_spark-component-guide/content/ch_oozie-spark-action.html#spark-config-oozie-spark2

It would be the best to provide daily-github-app jar to submit-spark commmand directory from central artifactory.

## End-to-end test

E2E tests invoked by build tool (maven in my case) are not provided. I haven't managed to found (and evaluate) plugins/tools 
that could easily provide running spark-job and comparsion of processing results against expected
values. That is probably possible by running local spark using docker-maven-plugin or exec-maven-plugin and combining with some shell tools. Nevertheless orchestrating 
the tools and evaluating processing results seems to be significant effort for me and is beyond the time I can spent on this recruitment task.

In my opinion better place for E2E test would be CI environment managed by Jenkins with handy plugins for docker and BDD testing frameworks.

## Other TODOs and known limitations

It would be good to include maven-release-plugin for releasing the snapshots to artifactory

Current application logic is quite simple so I decided to do not create dedicated log4j logger.

It can be worth to evaluate if (and how) partitioning factor affects performance of computation done on DataFrame created by ``cal df = spark.read.json(input_path + "/*.json.gz")``. Answering what would be the best number of partitions for given spark cluster and its resources. 