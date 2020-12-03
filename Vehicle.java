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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.math.BigInteger;
import java.net.BindException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Vehicle implements Runnable, Serializable {

    private BigInteger V_id;
    private Element Sv;
    private Pairing pairing;
    public int pub_id;
    BigInteger q = null;
    private boolean isVa;
    public int port; //For receiving and sending.
    BigInteger miaisum=new BigInteger("0");
    static BigInteger sigmaCi=new BigInteger("0");
    static BigInteger Ca1=null;
    static BigInteger Mpart3=new BigInteger("0");

    public Vehicle(int id, int port) {
        pub_id = id;
        Random rnd = new Random();
        V_id = new BigInteger(10, rnd);
        this.port = port;
        System.out.println("Vehicle " + pub_id + " is Running");
        System.out.println("Vehicle " + pub_id + " id :" + V_id);
    }

    //For communication of Vehicle to CCM
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

        @Override
        public BigInteger getq() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    //For data transfer between vehicle to vehicle.
    static class DataPacket2 implements SessionKeyPacket {

        String type;
        byte privateKey[];
        int port;

        public DataPacket2(String type, byte[] element, int port) {
            this.type = type;
            this.privateKey = element;
            this.port = port;
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
            return port;
        }
    }

    static class DataPacket3 implements QueryGroupPacket {

        BigInteger cipherAlphai, cipherKd, TS, MACai;

        public DataPacket3(BigInteger cipherAlphai, BigInteger cipherKd, BigInteger TS, BigInteger MACai) {
            this.cipherAlphai = cipherAlphai;
            this.cipherKd = cipherKd;
            this.TS = TS;
            this.MACai = MACai;
        }

        @Override
        public BigInteger getCipherAlphai() {
            return cipherAlphai;
        }

        @Override
        public BigInteger getCipherKd() {
            return cipherKd;
        }

        @Override
        public BigInteger getTS() {
            return TS;
        }

        @Override
        public BigInteger getMACai() {
            return MACai;
        }

    }

    static class DataPacket5 implements QueryRequestPacketVi {

        BigInteger Vi, Ci, TS, MACi;

        public DataPacket5(BigInteger Vi, BigInteger Ci, BigInteger TS, BigInteger MACi) {
            this.Vi = Vi;
            this.Ci = Ci;
            this.TS = TS;
            this.MACi = MACi;
        }

        @Override
        public BigInteger getTS() {
            return TS;
        }

        @Override
        public BigInteger getVi() {
            return Vi;
        }

        @Override
        public BigInteger getCi() {
            return Ci;
        }

        @Override
        public BigInteger getMACi() {
            return MACi;
        }

    }

    static class DataPacket6 implements QueryRequestPacketVa {

        BigInteger Va, Ca_1, Ca_2, TS, MACa;
        List<BigInteger> primes;

        public DataPacket6(BigInteger Va, BigInteger Ca_1, BigInteger Ca_2, List<BigInteger> primes, BigInteger TS, BigInteger MACa) {
            this.Va = Va;
            this.Ca_1 = Ca_1;
            this.Ca_2 = Ca_2;
            this.primes = primes;
            this.TS = TS;
            this.MACa = MACa;
        }

        @Override
        public BigInteger getTS() {
            return TS;
        }

        @Override
        public BigInteger getVa() {
            return Va;
        }

        @Override
        public BigInteger getCa_1() {
            return Ca_1;
        }

        @Override
        public BigInteger getCa_2() {
            return Ca_2;
        }

        @Override
        public BigInteger getMACa() {
            return MACa;
        }

        @Override
        public List<BigInteger> getPrimes() {
            return primes;
        }

    }

    public void multicast(SessionKeyPacket request) throws IOException, InterruptedException {
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

    public static BigInteger E(BigInteger key, BigInteger clearText) throws Exception {

        Cipher rc4 = Cipher.getInstance("RC4");
        SecretKeySpec rc4Key = new SecretKeySpec(key.toByteArray(), "RC4");
        rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
        byte[] cipherText = rc4.update(clearText.toByteArray());
        return new BigInteger(cipherText);

    }

    public static BigInteger D(BigInteger key, BigInteger cipherText) throws Exception {

        SecretKeySpec rc4Key = new SecretKeySpec(key.toByteArray(), "RC4");
        Cipher rc4Decrypt = Cipher.getInstance("RC4");
        rc4Decrypt.init(Cipher.DECRYPT_MODE, rc4Key);
        byte[] clearText = rc4Decrypt.update(cipherText.toByteArray());
        return new BigInteger(clearText);

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

    public List<BigInteger> primeNumbers(int k) {
        int i = 256;
        List<BigInteger> primelist = new ArrayList<>();
        while (true) {
            if (primelist.size() > k) {
                break;
            }
            if (BigInteger.valueOf(i).isProbablePrime(i / 2)) {
                primelist.add(BigInteger.valueOf(i));
            }
            i++;
        }
        return primelist;
    }

    @Override
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
                q = resdp.getq();
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

            ArrayList<Element> SessionKeys = new ArrayList<>(); //Used in step 3. Ca,1

            class VehicleHandler extends Thread { // To handle communication of Va to each vehicle.

                final ObjectInputStream in;
                final ObjectOutputStream out;
                final Socket soc;
                Element KVa_Vi;
                BigInteger Alphai;
                BigInteger Kd;

                public VehicleHandler(Socket soc, ObjectInputStream in, ObjectOutputStream out) {
                    this.soc = soc;
                    this.in = in;
                    this.out = out;
                    Alphai = null;
                    Kd = null;
                }

                public void setValues(BigInteger Alphai, BigInteger Kd) {
                    this.Alphai = Alphai;
                    this.Kd = Kd;
                }

                @Override
                public void run() {
                    try {
                        SessionKeyPacket resdp = (SessionKeyPacket) in.readObject();
                        String type = resdp.typeOfPacket();
                        System.out.println("Va received packet= " + type);
                        byte[] elementbytes = resdp.getPrivateKey();
                        Element ViPriv_Key = pairing.getG1().newElement();
                        ViPriv_Key.setFromBytes(elementbytes);
                        KVa_Vi = pairing.pairing(Sv, ViPriv_Key);
                        System.out.println("Va established.Session Key with one of Vi");
                        SessionKeys.add(KVa_Vi);
                        while (Alphai == null || Kd == null) {
                            // do nothing
                            sleep(100);
                        }
                        LocalDateTime datetime = LocalDateTime.now();
                        System.out.println("TimeStamp:" + datetime);
                        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("ddMMyyyyHHmmssnn");
                        String formattedDate = datetime.format(myFormatObj);
                        System.out.println("After formatting: " + formattedDate);
                        BigInteger TS = new BigInteger(formattedDate);
                        BigInteger key = new BigInteger(KVa_Vi.toBigInteger().toString() + TS.toString());
                        BigInteger cipherAlphai = E(key, Alphai);
                        BigInteger cipherKd = E(key, Kd);
                        String macinput = KVa_Vi.toBigInteger().toString() + cipherAlphai.toString() + cipherKd.toString() + TS.toString();
                        BigInteger MACai = HMAC(macinput);
                        DataPacket3 dp = new DataPacket3(cipherAlphai, cipherKd, TS, MACai);
                        System.out.println("Alphai sent: " + Alphai + "\nKd sent: " + Kd);
                        out.writeObject(dp);
                        soc.close();
                        this.in.close();
                        this.out.close();

                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    //Communication is Over with Vi.
                }

            }

            System.out.println("Vehicle " + pub_id + " is Va");
            flag = true;
            int port = 9002;
            //Broadcast Cooperation Request
            while (flag) {
                try {
                    sleep(5000);
                    SessionKeyPacket request = new DataPacket2("Cooperation Request", Sv.toBytes(), port);
                    multicast(request);
                    flag = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Accept requests from each vehicle
            ServerSocket serverSock = null;
            Socket socket = null;

            try {
                sleep(1000);
                //System.out.println(port);
                serverSock = new ServerSocket(port);
                serverSock.setSoTimeout(5000);
                //System.out.println("Va Started Accepting requests.");
            } catch (BindException e) {
                System.out.println("Bind exception here " + e);
            } catch (Exception e) {
                System.out.println(e);
            }
            //Make a thread for each communication with Vehicles.
            flag = true;
            ArrayList<VehicleHandler> threads = new ArrayList<>();
            while (flag) {
                socket = null;
                try {
                    sleep(100);
                    socket = serverSock.accept();
                    //System.out.println("A new Vehicle joined query group : " + socket);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    //System.out.println("Assigning new thread for this client");
                    VehicleHandler v = new VehicleHandler(socket, in, out);
                    Thread t = v;
                    threads.add(v);
                    t.start();
                } catch (SocketTimeoutException e) {
                    //System.out.println("Not getting any requests");
                    flag = false;
                    try {
                        serverSock.close();
                    } catch (IOException ex) {
                    }
                    break;
                } catch (Exception e) {
                    flag = false;
                    System.out.println("Exception in Va" + e);
                }
            }

            List<BigInteger> primelist = primeNumbers(threads.size()); //returns k no of prime numbers k=(threads.size())+1;
            //First k-1 prime numbers are for Vi's. And last prime is for Va.
            Random rnd = new Random();
            BigInteger Kd = new BigInteger(128, rnd);
            BigInteger Q = primelist.get(0);
            System.out.println(primelist);
            for (int i = 1; i < primelist.size(); i++) {
                Q=Q.multiply(primelist.get(i));
            }
            System.out.println("Q=" + Q);
            for (int i = 0; i < threads.size(); i++) {
                BigInteger qi = primelist.get(i);
                BigInteger Qi = Q.divide(qi);
                BigInteger QiI = Qi.modInverse(qi);
                BigInteger Alphai = Qi.multiply(QiI);
                threads.get(i).setValues(Alphai, Kd);
            }
            int a = threads.size();
            BigInteger qa = primelist.get(a);
            BigInteger Qa = Q.divide(qa);
            BigInteger QaI = Qa.modInverse(qa);
            BigInteger Alpha_a = Qa.multiply(QaI);

            // System.out.println(primelist);
            // Query Group Formulation step completed.
            // Query Request Generation step started.
            Element KVa_RSU = null;
            flag = true;
            port = 9004;
            while (flag) {
                try {
                    sleep(5000);
                    // Session Key Establishment.
                    InetAddress ip = InetAddress.getByName("localhost");
                    socket = new Socket(ip, port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    SessionKeyPacket dp = new DataPacket2("Va Session Key Establishment", Sv.toBytes(), 9004);
                    out.writeObject(dp);
                    out.flush();
                    sleep(500);
                    SessionKeyPacket resdp = (SessionKeyPacket) in.readObject();
                    String type = resdp.typeOfPacket();
                    System.out.println("Vi received packet= " + type);
                    byte[] elementbytes = resdp.getPrivateKey();
                    Element RSUPriv_Key = pairing.getG1().newElement();
                    RSUPriv_Key.setFromBytes(elementbytes);
                    KVa_RSU = pairing.pairing(Sv, RSUPriv_Key);
                    System.out.println("Va established session Key with RSU");

                    //Sending Query request
                    BigInteger ma = new BigInteger(8, rnd);
                    BigInteger part1_a1 = ma.multiply(Alpha_a);
                    miaisum=miaisum.add(part1_a1);
                    LocalDateTime datetime = LocalDateTime.now();
                    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("ddMMyyyyHHmmssnn");
                    String formattedDate = datetime.format(myFormatObj);
                    //System.out.println("After formatting: " + formattedDate);
                    BigInteger TS = new BigInteger(formattedDate);
                    String forPart2_a1 = KVa_RSU.toBigInteger().toString() + "1" + TS.toString();
                    BigInteger part2_a1 = H2(new BigInteger(forPart2_a1));
                    BigInteger part3_a1 = new BigInteger("0");
                    System.out.println("NO of Session keys"+SessionKeys.size());
                    for (int i = 0; i < SessionKeys.size(); i++) {
                        String forPart3_a1 = SessionKeys.get(i).toBigInteger().toString() + "2" + TS.toString();
                        part3_a1 = part3_a1.add(H2(new BigInteger(forPart3_a1))).mod(q);
                    }
                    BigInteger Ca_1 = part1_a1;
                    Ca_1 = Ca_1.add(part2_a1);
                    Ca_1 = Ca_1.subtract(part3_a1);
                    //Ca_1 = Ca_1;
                    
                    Ca1=Ca_1;
                    // part1_a2 is Kd 
                    String forPart2_a2 = KVa_RSU.toBigInteger().toString() + "3" + TS.toString();
                    BigInteger part2_a2 = H2(new BigInteger(forPart2_a2)).mod(q);
                    BigInteger Ca_2 = Kd;
                    Ca_2 = Ca_2.add(part2_a2);
                    //Ca_2 = Ca_2.mod(q);

                    String macinput = KVa_RSU.toBigInteger().toString() + Ca_1.toString() + Ca_2.toString() + primelist + TS.toString();
                    //System.out.println("HMAC from Va"+macinput);
                    BigInteger MACi = HMAC(macinput);
                    DataPacket6 query = new DataPacket6(BigInteger.valueOf(pub_id), Ca_1, Ca_2, primelist, TS, MACi);
                    
                    String forpart3 = KVa_RSU.toBigInteger().toString() + "1" + TS.toString();
                    Mpart3 = Mpart3.add(H2(new BigInteger(forpart3)));
                    out.writeObject(query);
                    sleep(500);
                    System.out.println("Query Request Sent from Vi");
                    System.out.println("Data Identifier Va Sent: " + ma);
                    //Query Request Sent

                    flag = false;
                    socket.close();
                    out.close();
                    in.close();
                } catch (ConnectException e) {
                    //Va hasn't started receiving packets.
                } catch (Exception ex) {
                    System.out.println("I am the exception Va " + ex);
                }
            }

        } //All other vehicles acting as Vi
        else {

            Element KVa_Vi = null;
            BigInteger Alphai = null;
            BigInteger Kd;

            System.out.println("Vehicle " + pub_id + " is one of Vi");
            MulticastSocket socket = null;
            int port = 9002;
            byte[] buf = new byte[1024];
            try {
                socket = new MulticastSocket(4446);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                socket.joinGroup(group);
                //
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                byte[] data = packet.getData();
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                ObjectInputStream is = new ObjectInputStream(in);
                SessionKeyPacket dp = null;
                dp = (SessionKeyPacket) is.readObject();
                port = dp.getPort();
                System.out.println("Vehicle " + pub_id + " received= " + dp.typeOfPacket());
                byte[] elementbytes = dp.getPrivateKey();
                Element VaPriv_Key = pairing.getG1().newElement();
                VaPriv_Key.setFromBytes(elementbytes);
                KVa_Vi = pairing.pairing(Sv, VaPriv_Key);
                System.out.println("Session Key Established.");
                //System.out.println("Class Name : "+KVa_Vi.getClass().getName()+"BigInteger Value: "+KVa_Vi.toBigInteger()+"\n Bit length: "+KVa_Vi.toBigInteger().bitLength());
                sleep(5000);
                //
                socket.leaveGroup(group);
                socket.close();
            } catch (Exception ex) {
                System.out.println(ex);
            }
            flag = true;
            //Accepted cooperation request and sending response

            while (flag) {
                try {
                    InetAddress ip = InetAddress.getByName("localhost");
                    //System.out.println(port);
                    Socket soc = new Socket(ip, port);
                    ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(soc.getInputStream());
                    SessionKeyPacket dp = new DataPacket2("Accepted Cooperation Request", Sv.toBytes(), port);
                    out.writeObject(dp);
                    sleep(500);
                    QueryGroupPacket resdp = (QueryGroupPacket) in.readObject();
                    BigInteger recievedMAC = resdp.getMACai();
                    BigInteger TS = resdp.getTS();
                    BigInteger key = new BigInteger(KVa_Vi.toBigInteger().toString() + TS.toString());
                    BigInteger cipherAlphai = resdp.getCipherAlphai();
                    BigInteger cipherKd = resdp.getCipherKd();
                    String macinput = KVa_Vi.toBigInteger().toString() + cipherAlphai.toString() + cipherKd.toString() + TS.toString();
                    BigInteger MAC = HMAC(macinput);
                    if (MAC.equals(recievedMAC)) {
                        System.out.println("Received Packet is Valid ");
                    } else {
                        System.out.println("Received Packet is Invalid ");
                        break;
                    }
                    Alphai = D(key, cipherAlphai);
                    Kd = D(key, cipherKd);
                    System.out.println("Alphai and Kd are received");
                    System.out.println("Alphai received: " + Alphai + "\nKd received: " + Kd);
                    flag = false;
                    soc.close();
                    out.close();
                    in.close();
                } catch (ConnectException e) {
                    //Va hasn't started receiving packets.
                } catch (Exception ex) {
                    System.out.println("I am the exception " + ex);
                }
            }
            // Query Group Formulation step completed.
            // Query Request Generation..

            Element KVi_RSU = null;
            flag = true;
            port = 9004;
            while (flag) {
                try {
                    // Session Key Establishment.
                    InetAddress ip = InetAddress.getByName("localhost");
                    Socket soc = new Socket(ip, port);
                    ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(soc.getInputStream());
                    SessionKeyPacket dp = new DataPacket2("Vi Session Key Establishment", Sv.toBytes(), 9004);
                    out.writeObject(dp);
                    sleep(500);
                    SessionKeyPacket resdp = (SessionKeyPacket) in.readObject();
                    String type = resdp.typeOfPacket();
                    System.out.println("Vi received packet= " + type);
                    byte[] elementbytes = resdp.getPrivateKey();
                    Element RSUPriv_Key = pairing.getG1().newElement();
                    RSUPriv_Key.setFromBytes(elementbytes);
                    KVi_RSU = pairing.pairing(Sv, RSUPriv_Key);
                    System.out.println("Vi established session Key with RSU");

                    //Sending Query request
                    Random rnd = new Random();
                    BigInteger mi = new BigInteger(8, rnd);
                    BigInteger part1 = mi.multiply(Alphai);
                    LocalDateTime datetime = LocalDateTime.now();
                    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("ddMMyyyyHHmmssnn");
                    String formattedDate = datetime.format(myFormatObj);
                    //System.out.println("After formatting: " + formattedDate);
                    BigInteger TS = new BigInteger(formattedDate);
                    String forPart2 = KVi_RSU.toBigInteger().toString() + "1" + TS.toString();
                    BigInteger part2 = H2(new BigInteger(forPart2));
                    String forPart3 = KVa_Vi.toBigInteger().toString() + "2" + TS.toString();
                    BigInteger part3 = H2(new BigInteger(forPart3)).mod(q);

                    BigInteger Ci = part1;
                    miaisum=miaisum.add(part1);
                    Ci = Ci.add(part2);
                    Ci = Ci.add(part3);
                    //Ci = Ci.mod(q);
                    sigmaCi = sigmaCi.add(Ci);
                    String macinput = KVi_RSU.toBigInteger().toString() + Ci.toString() + TS.toString();
                    BigInteger MACi = HMAC(macinput);
                    DataPacket5 query = new DataPacket5(BigInteger.valueOf(pub_id), Ci, TS, MACi);
                    String forpart3 = KVi_RSU.toBigInteger().toString() + "1" + TS.toString();
                    Mpart3 = Mpart3.add(H2(new BigInteger(forpart3)));
                    out.writeObject(query);
                    sleep(500);
                    System.out.println("Query Request Sent from Vi");
                    System.out.println("Data Identifier Vi Sent: " + mi);
                    //Query Request Sent
                    

                    flag = false;
                    soc.close();
                    out.close();
                    in.close();
                } catch (ConnectException e) {
                    //Va hasn't started receiving packets.
                } catch (Exception ex) {
                    System.out.println("I am the exception Vi" + ex);
                    //flag=false;
                }
            }
        }
        System.out.println("M calculated in V:"+miaisum);
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
