package com.er1cccc.acaf.example.sql;

import com.er1cccc.acaf.config.ControllableParam;
import com.er1cccc.acaf.config.Sink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SqlSink implements Sink {
    private ControllableParam params = new ControllableParam();

    public SqlSink(){
        params.put("name","zhangsan");
    }
    private final JdbcTemplate jdbcTemplate=new JdbcTemplate();


    @Override
    public Object sinkMethod() throws Exception {
//        jdbcTemplate.query("select * from t_user where name=\"" + (String)params.getParameter("name") + "\"", (RowMapper<Object>) null);
        jdbcTemplate.query((String)params.getParameter("name"), (RowMapper<Object>) null);
        return null;
    }

    public static void main(String[] args) throws Exception{
        new SqlSink().sinkMethod();
    }
}
