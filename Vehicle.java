/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

/**
 *
 * @author hp
 */
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
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
import java.net.MulticastSocket;
import java.util.Random;

public class Vehicle implements Runnable, Serializable {

    private BigInteger V_id;
    protected DatagramSocket socket = null;
    private Element Sv;
    private Pairing pairing;
    public int pub_id;

    public Vehicle(int id) {
        pub_id = id;
        Random rnd = new Random();
        V_id = new BigInteger(10, rnd);
        System.out.println("Vehicle " + pub_id + " is Running");
        System.out.println("My_id :" + V_id);
    }

    static class DataPacket implements Packet {

        String type;
        BigInteger V_id;

        public DataPacket(String type, BigInteger V_id) {
            this.type = type;
            this.V_id = V_id;
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
            return V_id;
        }

        @Override
        public String getPairingParameters() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isVa() {
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
        boolean flag = true;
        while (flag) {
            try {
                //Register in CCM by sending id.
                DataPacket dp = new DataPacket("RequestPrivateKey", V_id);
                boolean sent = false;
                do {
                    sent = send(dp, 9000);
                } while (!sent);
                System.out.println("Packet Sent ");
                //Getting Private key
                Packet resdp = receive(9001);
                System.out.println("Packet received = " + resdp.typeOfPacket());
                pairing = generatePairing(resdp.getPairingParameters());
                byte[] elementbytes = resdp.getPrivateKey();
                Sv = pairing.getG1().newElement();
                Sv.setFromBytes(elementbytes);
                System.out.println("Got my private key" + Sv);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

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

    /*
    public static void main(String args[]) throws Exception{
        Vehicle v = new Vehicle(1);
        DataPacket dp = new DataPacket("RequestPrivateKey", v.priv_id);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.writeObject(dp);
        byte[] data = outputStream.toByteArray();
        System.out.println(data.length);
    }
     */
}


/*

    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];
socket = new MulticastSocket(4446);
            InetAddress group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);
            while (true) {
                DataPacket dp=new DataPacket("RequestPrivateKey",priv_id);
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Vehicle "+pub_id+" got :"+received);
                sleep(FIVE_SECONDS);
                if ("end".equals(received)) {
                    break;
                }
            }
            socket.leaveGroup(group);
            socket.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }
 */
