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
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.io.IOException;
import java.net.ConnectException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.crypto.Cipher;

/**
 *
 * @author hp
 */
public class RSU implements Runnable, Serializable {

    private BigInteger RSU_id;
    private Element Srsu;
    private Pairing pairing;
    private int port;
    private BigInteger q = null;
    protected ServerSocket serverSock = null;
    protected Socket socket = null;
    private BigInteger sigmaCi = new BigInteger("0");
    private BigInteger Ca1 = null;
    private BigInteger part3 = new BigInteger("0");
    private BigInteger Q;
    private BigInteger Kd = null;
    private List<BigInteger> primes;
    private int queryDone = 0;

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

    static class DataPacket7 implements ContentPacket {

        BigInteger TS, MACm;
        byte[] Cm;

        public DataPacket7(byte[] Cm, BigInteger TS, BigInteger MACm) {
            this.Cm = Cm;
            this.TS = TS;
            this.MACm = MACm;
        }

        @Override
        public byte[] getCm() {
            return Cm;
        }

        @Override
        public BigInteger getTS() {
            return TS;
        }

        @Override
        public BigInteger getMACm() {
            return MACm;
        }
    }

    public void multicast(ContentPacket request) throws IOException, InterruptedException {
        DatagramSocket socket = null;
        InetAddress group;
        socket = new DatagramSocket();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.flush();
        os.writeObject(request);
        sleep(500);
        os.flush();
        byte[] buf = outputStream.toByteArray();
        group = InetAddress.getByName("230.0.0.0");
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        os.close();
        socket.close();
    }

    public Element H(BigInteger plainText) {
        Element h = pairing.getG1().newElement();
        try {
            MessageDigest mdSha1 = MessageDigest.getInstance("SHA3-256");
            byte[] pSha = mdSha1.digest(plainText.toByteArray());
            BigInteger no = new BigInteger(1, pSha);     //1 indicates positive number.
            byte[] ba = no.toByteArray();
            h = pairing.getG1().newElement().setFromHash(ba, 0, ba.length);
        } catch (Exception e) {
            System.out.println(e);
        }
        return h;
    }

    public static byte[] E(BigInteger key, String clearText) throws Exception {

        Cipher rc4 = Cipher.getInstance("RC4");
        SecretKeySpec rc4Key = new SecretKeySpec(key.toByteArray(), "RC4");
        rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
        byte[] cipherText = rc4.update(clearText.getBytes());
        return cipherText;
    }
    

    public BigInteger H2(BigInteger plainText) throws Exception {
        MessageDigest mdSha1 = MessageDigest.getInstance("MD5");
        byte[] pSha = mdSha1.digest(plainText.toByteArray());
        BigInteger no = new BigInteger(1, pSha);     //1 indicates positive number.
        byte[] ba = no.toByteArray();
        return new BigInteger(ba);
    }

    public BigInteger HMAC(String input) throws Exception {
        String key = "SECUREHMACKEY";
        Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        hmacSHA512.init(secretKeySpec);
        byte[] digest = hmacSHA512.doFinal(input.getBytes());
        return new BigInteger(digest);
    }

    public String getContent(List<BigInteger> dataIdentifiers) {
        String c = "";
        for (int i = 0; i < dataIdentifiers.size(); i++) {
            BigInteger d = dataIdentifiers.get(i);
            String s = d.toString();
            c = c + s;
            String ans = " : answer to " + s + "\n";
            c = c + ans;
        }
        return c;
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
                Element VxPriv_Key = pairing.getG1().newElement();
                VxPriv_Key.setFromBytes(elementbytes);
                KVx_RSU = pairing.pairing(H(RSU_id), VxPriv_Key);
                SessionKeyPacket resdp = new DataPacket4("Session Key Establishment", Srsu.toBytes());
                out.writeObject(resdp);
                sleep(500);
                out.flush();
                if (type.equals("Vi Session Key Establishment")) {
                    System.out.println("RSU established session Key with one of Vi");
                    QueryRequestPacketVi query = (QueryRequestPacketVi) in.readObject();
                    System.out.println("Query received from vi");
                    BigInteger recievedMAC = query.getMACi();
                    BigInteger TS = query.getTS();
                    BigInteger Ci = query.getCi();
                    String macinput = KVx_RSU.toBigInteger().toString() + Ci.toString() + TS.toString();
                    BigInteger MAC = HMAC(macinput);

                    //Query aggregation and reading
                    if (MAC.equals(recievedMAC)) {
                        System.out.println("Packet received from Vi is Valid ");
                        sigmaCi = sigmaCi.add(Ci);
                        String forpart3 = KVx_RSU.toBigInteger().toString() + "1" + TS.toString();
                        part3 = part3.add(H2(new BigInteger(forpart3)));

                    } else {
                        System.out.println("Packet received from Vi is Invalid ");
                    }

                } else {
                    System.out.println("RSU established session Key with Va");
                    QueryRequestPacketVa query = (QueryRequestPacketVa) in.readObject();
                    System.out.println("query received from va");
                    BigInteger recievedMAC = query.getMACa();
                    BigInteger TS = query.getTS();
                    BigInteger Ca_1 = query.getCa_1();
                    BigInteger Ca_2 = query.getCa_2();
                    List<BigInteger> primeslist = query.getPrimes();
                    String macinput = KVx_RSU.toBigInteger().toString() + Ca_1.toString() + Ca_2.toString() + primeslist + TS.toString();

                    BigInteger MAC = HMAC(macinput);
                    if (MAC.equals(recievedMAC)) {
                        System.out.println("Packet received from Va is Valid ");
                        primes = primeslist;
                        Ca1 = Ca_1;
                        String forpart3 = KVx_RSU.toBigInteger().toString() + "1" + TS.toString();
                        part3 = part3.add(H2(new BigInteger(forpart3)));
                        String forpartKd_2 = KVx_RSU.toBigInteger().toString() + "3" + TS.toString();
                        BigInteger partKd_2 = H2(new BigInteger(forpartKd_2));
                        Kd = Ca_2.subtract(partKd_2);
                        Kd = Kd.mod(q);
                    } else {
                        System.out.println("Packet received from Va is Invalid ");
                        System.out.println(macinput);
                    }
                }
                queryDone++;

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
                q = resdp.getq();
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
            serverSock.setSoTimeout(5000);
            System.out.println("RSUReady");
        } catch (Exception e) {
            System.out.println(e);
        }
        int count = 0;
        while (flag) {
            socket = null;
            try {
                sleep(100);
                socket = serverSock.accept();
                count++;
                //System.out.println("A new Vehicle is connected : " + socket);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                //System.out.println("Assigning new thread for this client");
                Thread t = new VehicleHandler(socket, in, out);
                t.start();
            } catch (SocketTimeoutException e) {
                //System.out.println("RSU not getting any queries");//\ncount="+count+"queryDone="+queryDone+"Kd = "+Kd);
                if (Kd != null && count == queryDone) {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Response to the queries.

        BigInteger Q = primes.get(0);
        for (int i = 1; i < primes.size(); i++) {
            Q = Q.multiply(primes.get(i));
        }
        //System.out.println("sigmaCi="+sigmaCi+"\nCa1="+Ca1+"\npart3="+part3);
        BigInteger M = new BigInteger("0");
        M = M.add(sigmaCi);
        M = M.add(Ca1);
        M = M.subtract(part3.mod(q));
        //M = M.mod(q);
        //System.out.println(primes);
        List<BigInteger> dataIdentifiers = new ArrayList<>();
        for (int i = 0; i < primes.size(); i++) {
            BigInteger temp = M.mod(Q);
            dataIdentifiers.add(temp.mod(primes.get(i)));
        }
        //System.out.println("Q=" + Q + "\nM=" + M);
        System.out.println("Data Identifiers Obtained :");
        for (int i = 0; i < dataIdentifiers.size(); i++) {
            System.out.println("m" + (i + 1) + " =" + dataIdentifiers.get(i));
        }
        String content = getContent(dataIdentifiers);
        System.out.println("content :\n" + content );

        //multicast content...
        flag = true;
        int port = 9008;
        //Broadcast Cooperation Request
        while (flag) {
            try {
                sleep(5000);
                LocalDateTime datetime = LocalDateTime.now();
                DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("ddMMyyyyHHmmssnn");
                String formattedDate = datetime.format(myFormatObj);
                BigInteger TS = new BigInteger(formattedDate);
                String Key = Kd.toString() + TS.toString();
                byte[] Cm = E(new BigInteger(Key), content);
                String macinput = Kd.toString() + new String(Cm) + TS.toString();
                BigInteger MACm = HMAC(macinput);
                //System.out.println("mac : " + MACm);
                ContentPacket con = new DataPacket7(Cm, TS, MACm);
                multicast(con);
                flag = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("RSU sent response to all queries.");
        //System.out.println("sigmaCi in v:"+Vehicle.sigmaCi+"\nCa1 in v:"+Vehicle.Ca1+"\nMpart3 in v:"+Vehicle.Mpart3);
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
