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
import it.unisa.dia.gas.jpbc.PairingParametersGenerator;              // For generation of parameters to Bilinear Pairing.
import it.unisa.dia.gas.jpbc.PairingParameters;                      //Class that holds the parameters.
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;    //Eliptic Curve(y^2 = x^3 + x) Generator For Bilinear pairing
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;         // To get instance of Pairing;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Element;

import java.io.File;
import java.io.PrintWriter;//For file writing;
import java.math.BigInteger;
import java.util.Random;
import java.security.MessageDigest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CCM implements Runnable, Serializable {

    PairingParameters params;
    Pairing pairing;
    Field Zp, G, GT;              //Groups
    BigInteger p;               //prime number
    Element P;                 //Generator of Group G
    Element s;                //CCM master Key
    Element Ppub;            //CCM public key
    BigInteger q;           //Large Prime Set by CCM
    protected DatagramSocket socket = null;
    private InetAddress group;
    private byte[] buf;

    static class DataPacket implements Packet {

        String type;
        byte element[];
        String pairParams;
        BigInteger V_id;

        public DataPacket(String type, BigInteger V_id, byte[] element, String pairParams) {
            this.type = type;
            this.V_id = V_id;
            this.element = element;
            this.pairParams = pairParams;
        }

        @Override
        public String typeOfPacket() {
            return type;
        }

        @Override
        public BigInteger getId() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getPairingParameters() {
            return pairParams;
        }

        @Override
        public byte[] getPrivateKey() {
            return element;
        }

        @Override
        public boolean isVa() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public CCM() {
        System.out.println("CCM has started running");
    }

    public Pairing runningGen(int k) {
        int pBits = k;          //Security Parameter k;
        int qBits = 512;
        PairingParametersGenerator parametersGenerator = new TypeACurveGenerator(pBits, qBits);
        params = parametersGenerator.generate();
        try (PrintWriter out = new PrintWriter(new File("params.properties"))) {
            //System.out.println("Parameters for the Type A curve:\n"+params.toString());
            out.println(params.toString());
        } catch (Exception e) {
            System.out.println(e);
        }
        pairing = PairingFactory.getPairing("params.properties");
        return pairing;
    }

    public Element H(BigInteger plainText) {
        Element h = G.newElement();;
        try {
            MessageDigest mdSha1 = MessageDigest.getInstance("SHA-1");
            byte[] pSha = mdSha1.digest(plainText.toByteArray());
            BigInteger no = new BigInteger(1, pSha);     //1 indicates positive number.
            byte[] ba = no.toByteArray();
            h = G.newElement().setFromHash(ba, 0, ba.length);
        } catch (Exception e) {
            System.out.println(e);
        }
        return h;
    }

    public BigInteger H2(BigInteger plainText) throws Exception {
        Element h = G.newElement();
        MessageDigest mdSha1 = MessageDigest.getInstance("MD-5");
        byte[] pSha = mdSha1.digest(plainText.toByteArray());
        BigInteger no = new BigInteger(1, pSha);     //1 indicates positive number.
        byte[] ba = no.toByteArray();
        return new BigInteger(ba);
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

    public BigInteger HMAC(BigInteger key, BigInteger input) throws Exception {

        Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.toByteArray(), "HmacSHA512");
        hmacSHA512.init(secretKeySpec);
        byte[] digest = hmacSHA512.doFinal(input.toByteArray());
        return new BigInteger(digest);
    }

    public void multicast(String multicastMessage) throws IOException {
        socket = new DatagramSocket();
        group = InetAddress.getByName("230.0.0.0");
        buf = multicastMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
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
            os.writeObject(packet);
            sleep(500);
            os.flush();
            byte[] resdata = outputStream.toByteArray();
            InetAddress IPAddress = InetAddress.getByName("localhost");
            socket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(resdata, resdata.length, IPAddress, port);
            socket.send(sendPacket);
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    public void run() {
        int k = 160;
        pairing = runningGen(k);
        Zp = pairing.getZr();
        G = pairing.getG1();      //G1 is same as G2 since Symmetric Bilinear Pairing;
        GT = pairing.getGT();
        p = G.getOrder();
        P = G.newRandomElement().getImmutable();
        s = Zp.newRandomElement();
        Ppub = P.mulZn(s);
        Random rnd = new Random();
        q = BigInteger.probablePrime(1024, rnd);
        boolean flag = true;
        while (flag) {
            try {
                //Got Vehicle Id
                Packet dp = receive(9000);
                sleep(1000);
                String type = dp.typeOfPacket();
                System.out.println("Packet received = " + type);
                //Respond with private key
                switch (type) {
                    case "RequestPrivateKey":
                        BigInteger id = dp.getId();
                        Element priv_key = H(id).mulZn(s);
                        DataPacket resdp = new DataPacket("ResponsePrivateKey", id, priv_key.toBytes(), params.toString());
                        boolean sent=false;
                        do{
                            sent=send(resdp,9001);
                        }while(!sent);
                        System.out.println("Packet Sent ");      
                }

                //System.out.println("Private key generated :"+resdp.getPrivateKey());
            } catch (Exception e) {
                System.out.println(e);
            }
        }

    }

}


/*
    while (flag) {
            try {
                String dString;
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                
                
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
 */
