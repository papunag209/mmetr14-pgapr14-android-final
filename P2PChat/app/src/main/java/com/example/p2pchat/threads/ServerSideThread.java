package com.example.p2pchat.threads;

import android.os.Handler;
import android.util.Log;

import com.example.p2pchat.MainActivity;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSideThread extends Thread {
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public Socket getSocket() {
        return socket;
    }

    ServerSocket serverSocket;
    Socket socket;
    SendAndReceive sendAndReceive;
    Handler handler;

    private static final String TAG = "ServerSideThread";

    public SendAndReceive getSendAndReceive() {
        return sendAndReceive;
    }

    public ServerSideThread(Handler handler){
        this.handler = handler;
    }

    @Override
    public void run() {
        try {

            serverSocket = new ServerSocket(8080);
            Log.d(TAG, "run: SOCKET IS: " + socket);

            socket = serverSocket.accept();
            Log.d(TAG, "run: SOCKET IS AFTER ACCEPT: " + socket);
            this.sendAndReceive = new SendAndReceive(socket,handler);
            Log.d(TAG, "run: INITIALIZED SEND AND RECEIVE" + sendAndReceive);
            sendAndReceive.start();
            Log.d(TAG, "run: sending msg from server");
            sendAndReceive.write("gamadjobaa".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
