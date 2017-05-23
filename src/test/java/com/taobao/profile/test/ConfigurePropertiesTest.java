package com.taobao.profile.test;

import com.mysql.jdbc.ConnectionImpl;
import com.mysql.jdbc.Field;
import com.mysql.jdbc.StatementImpl;
import com.taobao.profile.Profiler;
import com.taobao.profile.config.ConfigureProperties;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurePropertiesTest {

  private ConnectionImpl connection;

  private Profiler profiler;

    public DataSourceTransactionManager dataSourceTransactionManager;
    ConnectionImpl getConnection;
    public static String db;
    public static String host;
    public static int port;
  @Test
  public void testConfigureProperties(){
    Properties prop = new Properties();
    prop.put("file.name", "tprofiler.log");
    prop.put("log.file.path", "${user.home}/${file.name}");
    Properties context = System.getProperties();
    context.putAll(prop);
    
    Properties properties  = new ConfigureProperties(prop, context);
    Assert.assertEquals(properties.getProperty("log.file.path"), System.getProperty("user.home") + "/tprofiler.log" );
  }

  @Test
  public void testConfigure() throws IOException{
    Properties properties = new Properties();
    InputStream in = getClass().getClassLoader().getResourceAsStream("profile.properties");
    properties.load(in);

    Properties context = new Properties(System.getProperties());
    context.putAll(System.getProperties());
    context.putAll(properties);
    try{
      ConfigureProperties configureProperties = new ConfigureProperties(properties, context);
      String logFilePath = configureProperties.getProperty("logFilePath");
      Assert.assertEquals(logFilePath, System.getProperty("user.home") + "/logs/tprofiler.log");
    }finally{
      in.close();
    }
  }


  public void testEhance(StatementImpl callingStatement, String sql, int maxRows, Buffer packet, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Field[] cachedMetadata) {


  //  TransactionStatus transaction = dataSourceTransactionManager.getTransaction();

    if (sql == null) {
          sql = new String();
      } else {
          sql = new Buffer().getByteBuffer().toString();

      }

      String host = connection.getHost();
      int port = this.port;

      String db = this.db;



      Profiler.start4Mysql(host,port,db,sql);

  }

    static class Buffer {

      private byte[] byteBuffer;


        public byte[] getByteBuffer() {
            return this.byteBuffer;
        }

    }

}
