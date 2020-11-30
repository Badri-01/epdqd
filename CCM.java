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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;
import java.util.Date;
import java.math.BigInteger;
import java.security.MessageDigest;
import static java.lang.Thread.sleep;
import java.time.LocalTime;

public class CCM implements Runnable, Serializable {

    PairingParameters params;
    Pairing pairing;
    Field Zp, G, GT;              //Groups
    BigInteger p;               //prime number
    Element P;                 //Generator of Group G
    Element s;                //CCM master Key
    Element Ppub;            //CCM public key
    BigInteger q;           //Large Prime Set by CCM
    protected ServerSocket serverSock = null;
    Socket socket=null;
    int port;

    static class DataPacket implements Packet {

        String type;
        byte element[];
        String pairParams;
        BigInteger V_id;
        int priority;

        public DataPacket(String type, BigInteger V_id, byte[] element, String pairParams, int priority) {
            this.type = type;
            this.V_id = V_id;
            this.element = element;
            this.pairParams = pairParams;
            this.priority = priority;
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
        public boolean isFirst() {
            return priority == 1;
        }
    }

    class ClientHandler extends Thread {

        final ObjectInputStream in;
        final ObjectOutputStream out;
        final Socket soc;
        int priority;

        public ClientHandler(Socket soc, ObjectInputStream in, ObjectOutputStream out, int priority) {
            this.soc = soc;
            this.in = in;
            this.out = out;
            this.priority = priority;
        }

        @Override
        public void run() {
            try {
                Packet dp = (Packet) in.readObject();
                String type = dp.typeOfPacket();
                System.out.println("CCM received packet= " + type + " Priority: " + priority);
                BigInteger id = dp.getId();
                Element priv_key = H(id).mulZn(s);
                DataPacket resdp = new DataPacket("ResponsePrivateKey", id, priv_key.toBytes(), params.toString(), priority);
                out.writeObject(resdp);
                //System.out.println("Packet Sent ");
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e);
            }
            try {
                soc.close();
                this.in.close();
                this.out.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public CCM(int port) {
        this.port = port;
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
        Element h = G.newElement();
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
        try {
            serverSock = new ServerSocket(port);
            serverSock.setSoTimeout(20000);
            System.out.println("ServerReady");
        } catch (Exception e) {
            System.out.println(e);
        }
        int priority = 0;
        while (flag) {
            socket = null;
            try {
                sleep(100);
                socket = serverSock.accept();
                //System.out.println("A new Vehicle is connected : " + socket);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                //System.out.println("Assigning new thread for this client");
                Thread t = new ClientHandler(socket, in, out, priority++);
                t.start();
            } catch (SocketTimeoutException e) {
                System.out.println("CCM not getting any requests. Okay Never Mind :)");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}

