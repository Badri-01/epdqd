/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.net.BindException;
import java.net.SocketException;
import java.net.MulticastSocket;
import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
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
    public int pub_id;
    public int port1, port2;  //port1 for sending port2 for receiving
    private InetAddress group;
    private byte[] buf;

    public RSU(int port1, int port2) {
        this.port1 = port1;
        this.port2 = port2;
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

    public Packet receive(int port) throws Exception {
        socket = new DatagramSocket(port);
        byte[] incomingData = new byte[1024];
        DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
        socket.setSoTimeout(15000);
        socket.receive(incomingPacket);
        byte[] data = incomingPacket.getData();
        socket.close();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        Packet dp = (Packet) is.readObject();
        return dp;
    }

    boolean isPortOccupied(int port) {
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket(port);
            sock.close();
            return false;
        } catch (BindException ignored) {
            return true;
        } catch (SocketException ex) {
            System.out.println(ex);
            return true;
        }
    }

    public boolean send(Packet packet, int port) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(outputStream);
            os.flush();
            sleep(2000);
            os.writeObject(packet);
            os.flush();
            byte[] data = outputStream.toByteArray();
            InetAddress IPAddress = InetAddress.getByName("localhost");
            socket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
            socket.send(sendPacket);
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    public void run() {
        try {
            //register in ccm by sending id..
            DataPacket dp = new DataPacket("RSURequestPrivateKey", RSU_id);
            while (isPortOccupied(9000)) {
            }
            send(dp, 9000);
            System.out.println("Sent packet(rsu)");
            //getting the private key..
            Packet resdp = receive(9001);
            System.out.println("Packet received(rsu) = " + resdp.typeOfPacket());
            pairing = generatePairing(resdp.getPairingParameters());
            byte[] elementbytes = resdp.getPrivateKey();
            System.out.println("receiver side(rsu): " + elementbytes);
            Srsu = pairing.getG1().newElement();
            Srsu.setFromBytes(elementbytes);
            System.out.println("Got my private key(rsu)" + Srsu);

        } catch (Exception e) {
            System.out.println("exception in rsu..: " + e);
        }
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
