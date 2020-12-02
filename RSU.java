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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
// import java.util.Date;
// import java.time.LocalTime;

/**
 *
 * @author hp
 */
public class RSU implements Runnable, Serializable {

    private BigInteger RSU_id;
    private Element Srsu;
    private Pairing pairing;
    public int port;
    private InetAddress group;
    private byte[] buf;
    BigInteger q=null;
    protected ServerSocket serverSock = null;
    Socket socket = null;

    public RSU(int port) {
        this.port = port;
        Random rnd = new Random();
        RSU_id = new BigInteger(10, rnd);
        System.out.println("RSU has started Running");
        //System.out.println("My_id :" + RSU_id);
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

        @Override
        public BigInteger getq() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    static class DataPacket4 implements SessionKeyPacket {

        String type;
        byte privateKey[];

        public DataPacket4(String type, byte[] element) {
            this.type = type;
            this.privateKey = element;
        }

        @Override
        public String typeOfPacket() {
            return type;
        }

        @Override
        public byte[] getPrivateKey() {
            return privateKey;
        }

        @Override
        public int getPort() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
    
    public BigInteger HMAC(String input) throws Exception {
        String key = "SECUREHMACKEY";
        Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        hmacSHA512.init(secretKeySpec);
        byte[] digest = hmacSHA512.doFinal(input.getBytes());
        return new BigInteger(digest);
    }

    class VehicleHandler extends Thread {

        final ObjectInputStream in;
        final ObjectOutputStream out;
        final Socket soc;
        Element KVx_RSU;  // x can be a or i

        public VehicleHandler(Socket soc, ObjectInputStream in, ObjectOutputStream out) {
            this.soc = soc;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                SessionKeyPacket dp = (SessionKeyPacket) in.readObject();
                String type = dp.typeOfPacket();
                System.out.println("RSU received packet= " + type);
                byte[] elementbytes = dp.getPrivateKey();
                Element ViPriv_Key = pairing.getG1().newElement();
                ViPriv_Key.setFromBytes(elementbytes);
                KVx_RSU = pairing.pairing(Srsu, ViPriv_Key);
                SessionKeyPacket resdp = new DataPacket4("Session Key Establishment", Srsu.toBytes());
                out.writeObject(resdp);
                sleep(500);
                out.flush();
                if (type.equals("Vi Session Key Establishment")) {
                    System.out.println("RSU established session Key with one of Vi");
                    QueryRequestPacketVi query= (QueryRequestPacketVi) in.readObject();
                    System.out.println("Query received from vi");
                    BigInteger recievedMAC = query.getMACi();
                    BigInteger TS = query.getTS();
                    BigInteger Ci = query.getCi();
                    String macinput = KVx_RSU.toBigInteger().toString() + Ci.toString() + TS.toString();
                    BigInteger MAC = HMAC(macinput);
                    if (MAC.equals(recievedMAC)) {
                        System.out.println("Packet received from Vi is Valid ");
                    } else {
                        System.out.println("Packet received from Vi is Invalid ");
                    }
                    
                } else {
                    System.out.println("RSU established session Key with Va");
                    QueryRequestPacketVa query= (QueryRequestPacketVa) in.readObject();
                    System.out.println("query received from va");
                    BigInteger recievedMAC = query.getMACa();
                    BigInteger TS = query.getTS();
                    BigInteger Ca_1 = query.getCa_1();
                    BigInteger Ca_2 = query.getCa_2();
                    List<BigInteger> primes = query.getPrimes();
                    String macinput = KVx_RSU.toBigInteger().toString() + Ca_1.toString() + Ca_2.toString()+ primes + TS.toString();
                    BigInteger MAC = HMAC(macinput);
                    if (MAC.equals(recievedMAC)) {
                        System.out.println("Packet received from Va is Valid ");
                    } else {
                        System.out.println("Packet received from Va is Invalid ");
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
            }
            //Communication is Over with Vi.
            try {
                soc.close();
                this.in.close();
                this.out.close();
            } catch (Exception e) {
                System.out.println(e);
            }
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
                q=resdp.getq();
                soc.close();
                out.close();
                in.close();
                flag = false;
                sleep(10000);//Wait untill all vehicles forms a group.
            } catch (ConnectException e) {
                //ServerNotReady
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        flag = true;
        try {
            serverSock = new ServerSocket(9004);
            serverSock.setSoTimeout(20000);
            System.out.println("RSUReady");
        } catch (Exception e) {
            System.out.println(e);
        }
        while (flag) {
            socket = null;
            try {
                sleep(100);
                socket = serverSock.accept();
                //System.out.println("A new Vehicle is connected : " + socket);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                //System.out.println("Assigning new thread for this client");
                Thread t = new VehicleHandler(socket, in, out);
                t.start();
            } catch (SocketTimeoutException e) {
                System.out.println("RSU not getting any queries.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("RSU done");
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


/*public void multicast(String multicastMessage) throws IOException {
        socket = new DatagramSocket();
        group = InetAddress.getByName("230.0.0.0");
        buf = multicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
    }*/
