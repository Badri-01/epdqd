/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
// import java.util.Date;
// import java.time.LocalTime;

/**
 *
 * @author hp
 */
public class RSU implements Runnable, Serializable {

    private BigInteger RSU_id;
    protected DatagramSocket socket = null;
    private Element Srsu;
    private Pairing pairing;
    public int port;  //port1 for sending port2 for receiving
    private InetAddress group;
    private byte[] buf;

    public RSU(int port) {
        this.port = port;
        Random rnd = new Random();
        RSU_id = new BigInteger(10, rnd);
        System.out.println("RSU " + " is Running");
        System.out.println("My_id :" + RSU_id);
    }

    static class DataPacket implements Packet {

        String type;
        BigInteger RSU_id;

        public DataPacket(String type, BigInteger RSU_id) {
            this.type = type;
            this.RSU_id = RSU_id;
        }

        @Override
        public String typeOfPacket() {
            return type;
        }

        @Override
        public byte[] getPrivateKey() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public BigInteger getId() {
            return RSU_id;
        }

        @Override
        public String getPairingParameters() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isFirst() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
    
    
    public void run() {
        boolean flag = true;
        //Getting Parameters from ccm
        while (flag) {
            try {
                InetAddress ip = InetAddress.getByName("localhost");
                Socket soc = new Socket(ip, port);
                ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(soc.getInputStream());
                DataPacket dp = new DataPacket("RSURequestPrivateKey", RSU_id);
                out.writeObject(dp);
                System.out.println("RSU sent request");
                sleep(100);
                Packet resdp = (Packet) in.readObject();
                System.out.println("RSU received key");
                pairing = generatePairing(resdp.getPairingParameters());
                byte[] elementbytes = resdp.getPrivateKey();
                Srsu = pairing.getG1().newElement();
                Srsu.setFromBytes(elementbytes);
                soc.close();
                out.close();
                in.close();
                flag = false;
            }catch (ConnectException e) {
                //ServerNotReady
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        
        System.out.println("RSU done");
    }

    public void multicast(String multicastMessage) throws IOException {
        socket = new DatagramSocket();
        group = InetAddress.getByName("230.0.0.0");
        buf = multicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
    }

    public Pairing generatePairing(String params) {

        try (PrintWriter out = new PrintWriter(new File("params.properties"))) {
            //System.out.println("Parameters for the Type A curve:\n"+params.toString());
            out.println(params);
        } catch (Exception e) {
            System.out.println(e);
        }
        return PairingFactory.getPairing("params.properties");
    }

}
