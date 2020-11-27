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
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.Random;

public class Vehicle implements Runnable, Serializable {

    private BigInteger V_id;
    private Element Sv;
    private Pairing pairing;
    public int pub_id;
    private boolean isVa;
    public int port; //port1 for sending port2 for receiving

    public Vehicle(int id, int port) {
        pub_id = id;
        Random rnd = new Random();
        V_id = new BigInteger(10, rnd);
        this.port = port;
        System.out.println("Vehicle " + pub_id + " is Running");
        System.out.println("Vehicle " + pub_id + " id :" + V_id);
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
                DataPacket dp = new DataPacket("RequestPrivateKey" + pub_id, V_id);
                out.writeObject(dp);
                System.out.println("Vehicle " + pub_id + " sent request");
                sleep(100);
                Packet resdp = (Packet) in.readObject();
                System.out.println("Vehicle " + pub_id + " received key");
                pairing = generatePairing(resdp.getPairingParameters());
                byte[] elementbytes = resdp.getPrivateKey();
                Sv = pairing.getG1().newElement();
                Sv.setFromBytes(elementbytes);
                //System.out.println("Vehicle " + pub_id + " private key" + Sv);
                isVa = resdp.isFirst();
                //System.out.println("isVa"+isVa);
                soc.close();
                out.close();
                in.close();
                flag = false;
            } catch (ConnectException e) {
                //ServerNotReady
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        //for Va
        if (isVa) {
            System.out.println("Vehicle " + pub_id + " is Va");
        } else {
            System.out.println("Vehicle " + pub_id + " is one of Vi");
        }
        System.out.println("Vehicle " + pub_id + " done");
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
