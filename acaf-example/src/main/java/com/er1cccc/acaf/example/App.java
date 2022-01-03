package com.er1cccc.acaf.example;


import com.er1cccc.acaf.Launcher;
import com.er1cccc.acaf.example.sql.SqlConfigurer;
import com.er1cccc.acaf.example.ssrf.SSRFConfigurer;

public class App {
    public static void main( String[] args ) throws Exception{
        Launcher.launch(new SSRFConfigurer(),args);
    }
}
