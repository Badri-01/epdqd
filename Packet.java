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

import java.io.Serializable;
import java.math.BigInteger;

public interface Packet extends Serializable{
    String typeOfPacket();
    String getPairingParameters();
    byte[] getPrivateKey();
    BigInteger getId();
    boolean isFirst();
    
}
