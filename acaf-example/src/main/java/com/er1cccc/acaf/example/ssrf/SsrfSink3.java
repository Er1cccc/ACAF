package com.er1cccc.acaf.example.ssrf;

import com.er1cccc.acaf.config.ControllableParam;
import com.er1cccc.acaf.config.PassthroughRegistry;
import com.er1cccc.acaf.config.Sink;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class SsrfSink3 implements Sink {
    private ControllableParam params = new ControllableParam();

    public SsrfSink3(){
        params.put("url","http://localhost");
    }

    @Override
    public Object sinkMethod() throws Exception {
        Socket socket = new Socket((String) params.getParameter("host"), (Integer) params.getParameter("port"));
        InputStream in = socket.getInputStream();
        return null;
    }

    @Override
    public void addPassthrough(PassthroughRegistry passthroughRegistry) {
        try{
            Class<Socket> socketClass = Socket.class;
            Constructor<Socket> constructor = socketClass.getConstructor(String.class, int.class);
            passthroughRegistry.addPassthrough(constructor,1,2);
            Method getInputStream = socketClass.getMethod("getInputStream");
            passthroughRegistry.addPassthrough(getInputStream,0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        Class<Socket> socketClass = Socket.class;
        Constructor<Socket> constructor = socketClass.getConstructor(String.class, int.class);
        Method getInputStream = socketClass.getMethod("getInputStream");
    }
}
