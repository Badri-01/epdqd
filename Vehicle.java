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
    private boolean isVa;
    public int port; //For receiving and sending.

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

    }

    //For data transfer between vehicle to vehicle.
    static class DataPacket2 implements V2VPacket {

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

    public void multicast(V2VPacket request) throws IOException, InterruptedException {
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

    public BigInteger HMAC(BigInteger input) throws Exception {
        String key = "SECUREHMACKEY";
        Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        hmacSHA512.init(secretKeySpec);
        byte[] digest = hmacSHA512.doFinal(input.toByteArray());
        return new BigInteger(digest);
    }

    public List<BigInteger> primeNumbers(int k) {
        int i = 256;
        List<BigInteger> primelist = new ArrayList<>();
        while (true) {
            if (primelist.size() >= k) {
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
            //To handle communication of Va to each vehicle.
            class VehicleHandler extends Thread {

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
                        V2VPacket resdp = (V2VPacket) in.readObject();
                        String type = resdp.typeOfPacket();
                        System.out.println("Va received packet= " + type);
                        byte[] elementbytes = resdp.getPrivateKey();
                        Element ViPriv_Key = pairing.getG1().newElement();
                        ViPriv_Key.setFromBytes(elementbytes);
                        KVa_Vi = pairing.pairing(Sv, ViPriv_Key);
                        System.out.println("Va established.Session Key with one of Vi");
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
                        BigInteger MACai = HMAC(new BigInteger(macinput));
                        DataPacket3 dp = new DataPacket3(cipherAlphai, cipherKd, TS, MACai);
                        out.writeObject(dp);

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

            System.out.println("Vehicle " + pub_id + " is Va");
            flag = true;
            int port = 9002;
            //Broadcast Cooperation Request
            while (flag) {
                try {
                    sleep(5000);
                    V2VPacket request = new DataPacket2("Cooperation Request", Sv.toBytes(), port);
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
                System.out.println(port);
                serverSock = new ServerSocket(port);
                serverSock.setSoTimeout(10000);
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
                    break;
                } catch (Exception e) {
                    flag = false;
                    System.out.println("Exception in Va" + e);
                }
            }

            List<BigInteger> primelist = primeNumbers(threads.size());
            Random rnd = new Random();
            BigInteger Kd = new BigInteger(128, rnd);
            BigInteger Q = new BigInteger("1");
            for (int i = 0; i < primelist.size(); i++) {
                Q = Q.multiply(primelist.get(i));
            }
            for (int i = 0; i < threads.size(); i++) {
                BigInteger qi = primelist.get(i);
                BigInteger Qi = Q.divide(qi);
                BigInteger QiI = Qi.modInverse(qi);
                BigInteger Alphai = Qi.multiply(QiI);
                threads.get(i).setValues(Alphai, Kd);
            }
            System.out.println(primelist);

        } //All other vehicles acting as Vi
        else {

            Element KVa_Vi=null;
            BigInteger Alphai;
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
                V2VPacket dp = null;
                dp = (V2VPacket) is.readObject();
                port = dp.getPort();
                System.out.println("Vehicle " + pub_id + " got :" + dp.typeOfPacket());
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
                    V2VPacket dp = new DataPacket2("Accepted Cooperation Request", Sv.toBytes(), port);
                    out.writeObject(dp);
                    sleep(500);
                    QueryGroupPacket resdp = (QueryGroupPacket) in.readObject();
                    BigInteger recievedMAC=resdp.getMACai();                           
                    BigInteger TS = resdp.getTS();
                    BigInteger key = new BigInteger(KVa_Vi.toBigInteger().toString() + TS.toString());
                    BigInteger cipherAlphai =resdp.getCipherAlphai();
                    BigInteger cipherKd = resdp.getCipherKd();
                    String macinput = KVa_Vi.toBigInteger().toString() + cipherAlphai.toString() + cipherKd.toString() + TS.toString();
                    BigInteger MAC = HMAC(new BigInteger(macinput));
                    if(MAC.equals(recievedMAC)){
                        System.out.println("Received Packet is Valid ");
                    }
                    else{
                        System.out.println("Received Packet is Invalid ");
                        break;
                    }
                    Alphai = D(key, cipherAlphai);
                    Kd = D(key, cipherKd);
                    System.out.println("Alphai and Kd are received");
                    flag = false;
                } catch (ConnectException e) {
                    //Va hasn't started receiving packets.
                } catch (Exception ex) {
                    System.out.println("I am the exception " + ex);
                }
            }

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
