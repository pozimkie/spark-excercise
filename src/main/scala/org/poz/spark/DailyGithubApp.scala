package org.poz.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import com.databricks.spark.avro._

object DailyGithubApp {

  def main(args: Array[String]) {

    val spark = SparkSession
      .builder
      .appName("DailyGithubApp")
      .getOrCreate()
    import spark.implicits._

    //TODO properties should be validated
    val input_path = spark.sparkContext.getConf.get("spark.ghevents.input")
    val output_path = spark.sparkContext.getConf.get("spark.ghevents.output")
    val partitions = spark.sparkContext.getConf.get("spark.ghevents.output.partitions")

    val df = spark.read.json(input_path + "/*.json.gz")
    val proDF = df.select(
      $"repo.id".alias("repo_id"),
      $"repo.name",$"actor.id".alias("user_id"),
      $"actor.login", $"type", $"payload.action",
      to_date($"created_at", "yyyy-MM-dd").alias("date"))

    val repositoryAggr = proDF.groupBy("repo_id", "name", "date").agg(
      count(when($"type" === "WatchEvent" and $"action" === "started", true)).alias("starred"),
      count(when($"type" === "ForkEvent", true)).alias("forks"),
      count(when($"type" === "IssuesEvent" and $"action" === "opened", true)).alias("issues"),
      count(when($"type" === "PullRequestEvent" and $"action" === "opened", true)).alias("pull_requests"))

    repositoryAggr.repartition(partitions.toInt).write.avro(output_path + "/repository")

    val userAggr = proDF.groupBy("user_id", "login", "date").agg(
      count(when($"type" === "WatchEvent" and $"action" === "started", true)).alias("starred"),
      count(when($"type" === "IssuesEvent" and $"action" === "opened", true)).alias("issues"),
      count(when($"type" === "PullRequestEvent" and $"action" === "opened", true)).alias("pull_requests"))

    userAggr.repartition(partitions.toInt).write.avro(output_path + "/user")

    spark.stop()
  }

}
