/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import static java.lang.Thread.sleep;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.time.LocalTime;

/**
 *
 * @author hp
 */
public class RSU implements Runnable {

    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    private InetAddress group;
    private long FIVE_SECONDS = 5000;
    private byte[] buf;
    
    public void run() {
        boolean flag=true;
        System.out.println("Hey There I am Rsu and I am running;");
        while (flag) {
            try {
                String dString;
                if (in == null) {
                    dString = new Date().toString();
                } else {
                    dString = LocalTime.now().toString();
                }
                multicast(dString);
                sleep(FIVE_SECONDS);
                System.out.println("RSU is still running;");
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("I'm Done...");
    }


    public void multicast(String multicastMessage) throws IOException {
        socket = new DatagramSocket();
        group = InetAddress.getByName("230.0.0.0");
        buf = multicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
    }

}
