package org.BernardX.spark.sql.perf.tpcds

import org.apache.spark.sql.SparkSession

object GenerateTpcdsData {
  def main(args: Array[String]): Unit = {
    if (args.length != 4) {
      System.err.println(
        s"Usage: $RunTpcds <DATA_SCALE> <SKIP_DATAGENERATION> <DSAGEN_DIR> <HADOOP_HOST>"
      )
      System.exit(1)
    }
    try {
      val scaleFactor: String = args(0)
      val skipDataGenerate: String = args(1)
      val dsdgenDir = args(2)
      val hadoopHost = args(3)
      val rootDir = s"hdfs://$hadoopHost:9000/BenchmarkData/Tpcds/tpcds_$scaleFactor"
      val format = "parquet"
      val databaseName = "tpcds_" + scaleFactor +"_parquet"

      val sparkSession = SparkSession
        .builder()
        .enableHiveSupport()
        .getOrCreate()

      val sqlContext = sparkSession.sqlContext
      val tables = new TPCDSTables(sparkSession.sqlContext,
        dsdgenDir = dsdgenDir,
        scaleFactor = scaleFactor,
        useDoubleForDecimal = true,
        useStringForDate = true)

      if (!skipDataGenerate.toBoolean) {
        tables.genData(
          location = rootDir,
          format = format,
          overwrite = true,
          partitionTables = false,
          clusterByPartitionColumns = false,
          filterOutNullPartitionValues = false,
          numPartitions = 120)
      }

      //创建临时表
      tables.createTemporaryTables(rootDir, format)
      //将表信息注册到 hive metastore
      sqlContext.sql(s"create database $databaseName")
      tables.createExternalTables(rootDir, format, databaseName, overwrite = true, discoverPartitions = false)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}
